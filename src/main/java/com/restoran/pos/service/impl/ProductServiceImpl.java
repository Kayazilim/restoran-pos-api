package com.restoran.pos.service.impl;

import com.restoran.pos.dto.product.MenuItemResponse;
import com.restoran.pos.dto.product.ProductCreateRequest;
import com.restoran.pos.dto.product.ProductPatchRequest;
import com.restoran.pos.dto.product.ProductResponse;
import com.restoran.pos.dto.product.ProductUpdateRequest;
import com.restoran.pos.entity.Product;
import com.restoran.pos.exception.ConflictException;
import com.restoran.pos.exception.ResourceNotFoundException;
import com.restoran.pos.repository.ProductRepository;
import com.restoran.pos.service.ProductService;
import com.restoran.pos.util.AllergenFormatter;
import com.restoran.pos.util.PatchGuard;
import com.restoran.pos.util.Texts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> findAll(String category, Boolean active) {
        return productRepository.findByFilter(category, active).stream().map(ProductResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return ProductResponse.from(getProduct(id));
    }

    @Override
    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        String name = request.name().trim();
        ensureNameIsUnique(name, null);
        // Her zaman yeni bir varlık oluşturulur (persist); istemcinin gönderebileceği bir id hiçbir kaydı güncelleyemez.
        Product product = new Product(name, Texts.trimToNull(request.description()), request.category().trim(),
                request.price(), request.stockQuantity(), AllergenFormatter.join(request.allergens()),
                request.active() == null || request.active());
        return ProductResponse.from(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductUpdateRequest request) {
        Product product = getProduct(id);
        String name = request.name().trim();
        ensureNameIsUnique(name, id);
        product.setName(name);
        product.setDescription(Texts.trimToNull(request.description()));
        product.setCategory(request.category().trim());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setAllergens(AllergenFormatter.join(request.allergens()));
        product.setActive(request.active());
        return ProductResponse.from(productRepository.update(product));
    }

    @Override
    @Transactional
    public ProductResponse patch(Long id, ProductPatchRequest request) {
        PatchGuard.ensureIdUnchanged(id, request.id());
        Product product = getProduct(id);
        if (request.name() != null) {
            String name = request.name().trim();
            ensureNameIsUnique(name, id);
            product.setName(name);
        }
        if (request.description() != null) {
            product.setDescription(Texts.trimToNull(request.description()));
        }
        if (request.category() != null) {
            product.setCategory(request.category().trim());
        }
        if (request.price() != null) {
            product.setPrice(request.price());
        }
        if (request.stockQuantity() != null) {
            product.setStockQuantity(request.stockQuantity());
        }
        if (request.allergens() != null) {
            product.setAllergens(AllergenFormatter.join(request.allergens()));
        }
        if (request.active() != null) {
            product.setActive(request.active());
        }
        return ProductResponse.from(productRepository.update(product));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = getProduct(id);
        if (productRepository.isReferencedByOrders(id)) {
            throw new ConflictException("Ürün geçmiş siparişlerde kullanıldığı için silinemez; "
                    + "menüden kaldırmak için 'active' alanını false yapın.");
        }
        productRepository.delete(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> findMenu(String category) {
        return productRepository.findByFilter(category, true).stream().map(MenuItemResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MenuItemResponse findMenuItem(Long id) {
        Product product = productRepository.findById(id)
                .filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Menüde bu ürün bulunamadı: id=" + id));
        return MenuItemResponse.from(product);
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Ürün", id));
    }

    private void ensureNameIsUnique(String name, Long excludeId) {
        if (productRepository.existsByName(name, excludeId)) {
            throw new ConflictException("Bu isimde bir ürün zaten var: " + name);
        }
    }
}

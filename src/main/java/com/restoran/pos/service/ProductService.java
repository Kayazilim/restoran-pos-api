package com.restoran.pos.service;

import com.restoran.pos.dto.product.MenuItemResponse;
import com.restoran.pos.dto.product.ProductCreateRequest;
import com.restoran.pos.dto.product.ProductPatchRequest;
import com.restoran.pos.dto.product.ProductResponse;
import com.restoran.pos.dto.product.ProductUpdateRequest;

import java.util.List;

public interface ProductService {

    List<ProductResponse> findAll(String category, Boolean active);

    ProductResponse findById(Long id);

    ProductResponse create(ProductCreateRequest request);

    ProductResponse update(Long id, ProductUpdateRequest request);

    ProductResponse patch(Long id, ProductPatchRequest request);

    void delete(Long id);

    List<MenuItemResponse> findMenu(String category);

    MenuItemResponse findMenuItem(Long id);
}

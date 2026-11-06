package com.restoran.pos.repository;

import com.restoran.pos.entity.Product;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(Long id);

    List<Product> findAll();

    /** Kategori ve/veya aktiflik alanına göre parametreli JPQL filtresi. */
    List<Product> findByFilter(String category, Boolean active);

    /**
     * Ürün satırlarını id sırasıyla PESSIMISTIC_WRITE (SELECT ... FOR UPDATE) kilidiyle okur.
     * Her işlem kilitleri aynı sırada aldığı için kilitlenme (deadlock) oluşmaz.
     */
    List<Product> findAllByIdsForUpdate(Collection<Long> ids);

    Product update(Product product);

    void delete(Product product);

    boolean existsByName(String name, Long excludeId);

    boolean isReferencedByOrders(Long productId);
}

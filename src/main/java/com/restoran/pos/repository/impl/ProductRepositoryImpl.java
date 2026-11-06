package com.restoran.pos.repository.impl;

import com.restoran.pos.entity.Product;
import com.restoran.pos.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class ProductRepositoryImpl implements ProductRepository {

    private static final String LOCK_TIMEOUT_HINT = "jakarta.persistence.lock.timeout";

    private final EntityManager entityManager;
    private final int lockTimeoutMs;

    public ProductRepositoryImpl(EntityManager entityManager,
                                 @Value("${pos.concurrency.lock-timeout-ms}") int lockTimeoutMs) {
        this.entityManager = entityManager;
        this.lockTimeoutMs = lockTimeoutMs;
    }

    @Override
    public Product save(Product product) {
        entityManager.persist(product);
        return product;
    }

    @Override
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Product.class, id));
    }

    @Override
    public List<Product> findAll() {
        return entityManager.createQuery("SELECT p FROM Product p ORDER BY p.id", Product.class)
                .getResultList();
    }

    @Override
    public List<Product> findByFilter(String category, Boolean active) {
        boolean filterByCategory = category != null && !category.isBlank();
        StringBuilder jpql = new StringBuilder("SELECT p FROM Product p WHERE 1 = 1");
        if (filterByCategory) {
            jpql.append(" AND LOWER(p.category) = LOWER(:category)");
        }
        if (active != null) {
            jpql.append(" AND p.active = :active");
        }
        jpql.append(" ORDER BY p.category, p.name");

        TypedQuery<Product> query = entityManager.createQuery(jpql.toString(), Product.class);
        if (filterByCategory) {
            query.setParameter("category", category.trim());
        }
        if (active != null) {
            query.setParameter("active", active);
        }
        return query.getResultList();
    }

    @Override
    public List<Product> findAllByIdsForUpdate(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery("SELECT p FROM Product p WHERE p.id IN :ids ORDER BY p.id", Product.class)
                .setParameter("ids", ids)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setHint(LOCK_TIMEOUT_HINT, lockTimeoutMs)
                .getResultList();
    }

    @Override
    public Product update(Product product) {
        Product merged = entityManager.merge(product);
        // Sürüm (version) kontrolü ve kısıt ihlalleri burada, yanıt üretilmeden önce ortaya çıksın.
        entityManager.flush();
        return merged;
    }

    @Override
    public void delete(Product product) {
        entityManager.remove(entityManager.contains(product) ? product : entityManager.merge(product));
        entityManager.flush();
    }

    @Override
    public boolean existsByName(String name, Long excludeId) {
        String jpql = "SELECT COUNT(p) FROM Product p WHERE LOWER(p.name) = LOWER(:name)"
                + (excludeId != null ? " AND p.id <> :excludeId" : "");
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class)
                .setParameter("name", name);
        if (excludeId != null) {
            query.setParameter("excludeId", excludeId);
        }
        return query.getSingleResult() > 0;
    }

    @Override
    public boolean isReferencedByOrders(Long productId) {
        return entityManager.createQuery(
                        "SELECT COUNT(i) FROM OrderItem i WHERE i.product.id = :productId", Long.class)
                .setParameter("productId", productId)
                .getSingleResult() > 0;
    }
}

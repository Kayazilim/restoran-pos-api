package com.restoran.pos.repository.impl;

import com.restoran.pos.entity.Permission;
import com.restoran.pos.repository.PermissionRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class PermissionRepositoryImpl implements PermissionRepository {

    private final EntityManager entityManager;

    public PermissionRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Permission save(Permission permission) {
        entityManager.persist(permission);
        return permission;
    }

    @Override
    public Optional<Permission> findByName(String name) {
        return entityManager.createQuery("SELECT p FROM Permission p WHERE p.name = :name", Permission.class)
                .setParameter("name", name)
                .getResultList().stream()
                .findFirst();
    }

    @Override
    public List<Permission> findAll() {
        return entityManager.createQuery("SELECT p FROM Permission p ORDER BY p.id", Permission.class)
                .getResultList();
    }

    @Override
    public List<Permission> findByNames(Collection<String> names) {
        if (names.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery("SELECT p FROM Permission p WHERE p.name IN :names", Permission.class)
                .setParameter("names", names)
                .getResultList();
    }
}

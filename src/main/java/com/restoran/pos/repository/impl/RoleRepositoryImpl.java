package com.restoran.pos.repository.impl;

import com.restoran.pos.entity.Role;
import com.restoran.pos.repository.RoleRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class RoleRepositoryImpl implements RoleRepository {

    private final EntityManager entityManager;

    public RoleRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Role save(Role role) {
        entityManager.persist(role);
        return role;
    }

    @Override
    public Optional<Role> findById(Long id) {
        return Optional.ofNullable(entityManager.find(Role.class, id));
    }

    @Override
    public Optional<Role> findByName(String name) {
        return entityManager.createQuery("SELECT r FROM Role r WHERE r.name = :name", Role.class)
                .setParameter("name", name)
                .getResultList().stream()
                .findFirst();
    }

    @Override
    public List<Role> findAll() {
        return entityManager.createQuery("SELECT r FROM Role r ORDER BY r.id", Role.class).getResultList();
    }

    @Override
    public List<Role> findByNames(Collection<String> names) {
        if (names.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery("SELECT r FROM Role r WHERE r.name IN :names ORDER BY r.id", Role.class)
                .setParameter("names", names)
                .getResultList();
    }

    @Override
    public Role update(Role role) {
        Role merged = entityManager.merge(role);
        entityManager.flush();
        return merged;
    }

    @Override
    public void delete(Role role) {
        entityManager.remove(entityManager.contains(role) ? role : entityManager.merge(role));
        entityManager.flush();
    }

    @Override
    public long countUsersWithRole(Long roleId) {
        return entityManager.createQuery(
                        "SELECT COUNT(u) FROM UserAccount u JOIN u.roles r WHERE r.id = :roleId", Long.class)
                .setParameter("roleId", roleId)
                .getSingleResult();
    }
}

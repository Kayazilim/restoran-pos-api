package com.restoran.pos.repository.impl;

import com.restoran.pos.entity.UserAccount;
import com.restoran.pos.repository.UserAccountRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserAccountRepositoryImpl implements UserAccountRepository {

    private final EntityManager entityManager;

    public UserAccountRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public UserAccount save(UserAccount user) {
        entityManager.persist(user);
        return user;
    }

    @Override
    public Optional<UserAccount> findById(Long id) {
        return Optional.ofNullable(entityManager.find(UserAccount.class, id));
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return entityManager.createQuery("SELECT u FROM UserAccount u WHERE u.username = :username", UserAccount.class)
                .setParameter("username", username)
                .getResultList().stream()
                .findFirst();
    }

    @Override
    public List<UserAccount> findAll() {
        return entityManager.createQuery("SELECT u FROM UserAccount u ORDER BY u.id", UserAccount.class)
                .getResultList();
    }

    @Override
    public List<UserAccount> search(String roleName, Boolean enabled) {
        boolean filterByRole = roleName != null && !roleName.isBlank();
        StringBuilder jpql = new StringBuilder("SELECT DISTINCT u FROM UserAccount u");
        if (filterByRole) {
            jpql.append(" JOIN u.roles r");
        }
        jpql.append(" WHERE 1 = 1");
        if (filterByRole) {
            jpql.append(" AND r.name = :roleName");
        }
        if (enabled != null) {
            jpql.append(" AND u.enabled = :enabled");
        }
        jpql.append(" ORDER BY u.id");

        TypedQuery<UserAccount> query = entityManager.createQuery(jpql.toString(), UserAccount.class);
        if (filterByRole) {
            query.setParameter("roleName", roleName.trim().toUpperCase());
        }
        if (enabled != null) {
            query.setParameter("enabled", enabled);
        }
        return query.getResultList();
    }

    @Override
    public List<UserAccount> findByTableId(Long tableId) {
        return entityManager.createQuery("SELECT u FROM UserAccount u WHERE u.table.id = :tableId", UserAccount.class)
                .setParameter("tableId", tableId)
                .getResultList();
    }

    @Override
    public UserAccount update(UserAccount user) {
        UserAccount merged = entityManager.merge(user);
        entityManager.flush();
        return merged;
    }

    @Override
    public void delete(UserAccount user) {
        entityManager.remove(entityManager.contains(user) ? user : entityManager.merge(user));
        entityManager.flush();
    }

    @Override
    public boolean existsByUsername(String username, Long excludeId) {
        String jpql = "SELECT COUNT(u) FROM UserAccount u WHERE LOWER(u.username) = LOWER(:username)"
                + (excludeId != null ? " AND u.id <> :excludeId" : "");
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class)
                .setParameter("username", username);
        if (excludeId != null) {
            query.setParameter("excludeId", excludeId);
        }
        return query.getSingleResult() > 0;
    }

    @Override
    public long count() {
        return entityManager.createQuery("SELECT COUNT(u) FROM UserAccount u", Long.class).getSingleResult();
    }
}

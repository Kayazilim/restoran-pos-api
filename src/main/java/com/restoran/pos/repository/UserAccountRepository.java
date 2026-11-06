package com.restoran.pos.repository;

import com.restoran.pos.entity.UserAccount;

import java.util.List;
import java.util.Optional;

public interface UserAccountRepository {

    UserAccount save(UserAccount user);

    Optional<UserAccount> findById(Long id);

    Optional<UserAccount> findByUsername(String username);

    List<UserAccount> findAll();

    /** Rol adı ve/veya aktiflik bilgisine göre parametreli JPQL filtresi. */
    List<UserAccount> search(String roleName, Boolean enabled);

    List<UserAccount> findByTableId(Long tableId);

    UserAccount update(UserAccount user);

    void delete(UserAccount user);

    boolean existsByUsername(String username, Long excludeId);

    long count();
}

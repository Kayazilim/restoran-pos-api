package com.restoran.pos.repository;

import com.restoran.pos.entity.Role;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoleRepository {

    Role save(Role role);

    Optional<Role> findById(Long id);

    Optional<Role> findByName(String name);

    List<Role> findAll();

    List<Role> findByNames(Collection<String> names);

    Role update(Role role);

    void delete(Role role);

    long countUsersWithRole(Long roleId);
}

package com.restoran.pos.repository;

import com.restoran.pos.entity.Permission;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PermissionRepository {

    Permission save(Permission permission);

    Optional<Permission> findByName(String name);

    List<Permission> findAll();

    List<Permission> findByNames(Collection<String> names);
}

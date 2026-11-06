package com.restoran.pos.service;

import com.restoran.pos.dto.role.PermissionResponse;
import com.restoran.pos.dto.role.RoleCreateRequest;
import com.restoran.pos.dto.role.RolePatchRequest;
import com.restoran.pos.dto.role.RoleResponse;
import com.restoran.pos.dto.role.RoleUpdateRequest;

import java.util.List;

public interface RoleService {

    List<RoleResponse> findAll();

    RoleResponse findById(Long id);

    RoleResponse create(RoleCreateRequest request);

    RoleResponse update(Long id, RoleUpdateRequest request);

    RoleResponse patch(Long id, RolePatchRequest request);

    void delete(Long id);

    List<PermissionResponse> findAllPermissions();
}

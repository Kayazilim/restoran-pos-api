package com.restoran.pos.dto.role;

import com.restoran.pos.entity.Permission;

public record PermissionResponse(Long id, String name, String description) {

    public static PermissionResponse from(Permission permission) {
        return new PermissionResponse(permission.getId(), permission.getName(), permission.getDescription());
    }
}

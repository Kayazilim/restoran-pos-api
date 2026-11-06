package com.restoran.pos.dto.role;

import com.restoran.pos.entity.Permission;
import com.restoran.pos.entity.Role;

import java.util.List;

public record RoleResponse(Long id, String name, String description, boolean systemRole, List<String> permissions) {

    public static RoleResponse from(Role role) {
        List<String> permissions = role.getPermissions().stream().map(Permission::getName).sorted().toList();
        return new RoleResponse(role.getId(), role.getName(), role.getDescription(), role.isSystemRole(), permissions);
    }
}

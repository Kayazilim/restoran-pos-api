package com.restoran.pos.controller;

import com.restoran.pos.dto.role.PermissionResponse;
import com.restoran.pos.security.Permissions;
import com.restoran.pos.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@Tag(name = "Roller", description = "Genişletilebilir rol/yetki eşlemesi")
public class PermissionController {

    private final RoleService roleService;

    public PermissionController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.ROLES_READ + "')")
    @Operation(summary = "Tanımlı tüm yetkileri listele")
    public List<PermissionResponse> findAll() {
        return roleService.findAllPermissions();
    }
}

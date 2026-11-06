package com.restoran.pos.controller;

import com.restoran.pos.dto.role.RoleCreateRequest;
import com.restoran.pos.dto.role.RolePatchRequest;
import com.restoran.pos.dto.role.RoleResponse;
import com.restoran.pos.dto.role.RoleUpdateRequest;
import com.restoran.pos.security.Permissions;
import com.restoran.pos.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/roles")
@Tag(name = "Roller", description = "Genişletilebilir rol/yetki eşlemesi: yeni roller (ör. PAKET_SERVIS) kod değiştirmeden eklenebilir")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.ROLES_READ + "')")
    @Operation(summary = "Rolleri ve yetkilerini listele")
    public List<RoleResponse> findAll() {
        return roleService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.ROLES_READ + "')")
    @Operation(summary = "Kimliğe göre rol getir")
    public RoleResponse findById(@PathVariable Long id) {
        return roleService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.ROLES_WRITE + "')")
    @Operation(summary = "Yeni rol tanımla")
    @ApiResponse(responseCode = "201", description = "Rol oluşturuldu",
            content = @Content(schema = @Schema(implementation = RoleResponse.class)))
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody RoleCreateRequest request) {
        RoleResponse created = roleService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.ROLES_WRITE + "')")
    @Operation(summary = "Rolü tam güncelle (PUT)", description = "Sistem rolleri değiştirilemez (409).")
    public RoleResponse update(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return roleService.update(id, request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.ROLES_WRITE + "')")
    @Operation(summary = "Rolü kısmi güncelle (PATCH)")
    public RoleResponse patch(@PathVariable Long id, @Valid @RequestBody RolePatchRequest request) {
        return roleService.patch(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.ROLES_DELETE + "')")
    @Operation(summary = "Rolü sil", description = "Sistem rolleri ve kullanıcıya atanmış roller silinemez (409).")
    @ApiResponse(responseCode = "204", description = "Rol silindi")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

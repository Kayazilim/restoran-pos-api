package com.restoran.pos.service.impl;

import com.restoran.pos.dto.role.PermissionResponse;
import com.restoran.pos.dto.role.RoleCreateRequest;
import com.restoran.pos.dto.role.RolePatchRequest;
import com.restoran.pos.dto.role.RoleResponse;
import com.restoran.pos.dto.role.RoleUpdateRequest;
import com.restoran.pos.entity.Permission;
import com.restoran.pos.entity.Role;
import com.restoran.pos.exception.BadRequestException;
import com.restoran.pos.exception.ConflictException;
import com.restoran.pos.exception.ResourceNotFoundException;
import com.restoran.pos.repository.PermissionRepository;
import com.restoran.pos.repository.RoleRepository;
import com.restoran.pos.service.RoleService;
import com.restoran.pos.util.PatchGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Genişletilebilir rol yapısı: yeni roller (ör. paket servis platformu) kod değiştirmeden tanımlanıp yetkilerle
 * eşlenebilir. Proje önerisindeki beş sistem rolü değiştirilemez ve silinemez.
 */
@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleServiceImpl(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> findAll() {
        return roleRepository.findAll().stream().map(RoleResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse findById(Long id) {
        return RoleResponse.from(getRole(id));
    }

    @Override
    @Transactional
    public RoleResponse create(RoleCreateRequest request) {
        ensureNameIsUnique(request.name(), null);
        Role role = new Role(request.name(), request.description().trim(), false);
        role.replacePermissions(resolvePermissions(request.permissions()));
        return RoleResponse.from(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RoleResponse update(Long id, RoleUpdateRequest request) {
        Role role = getModifiableRole(id);
        ensureNameIsUnique(request.name(), id);
        role.setName(request.name());
        role.setDescription(request.description().trim());
        role.replacePermissions(resolvePermissions(request.permissions()));
        return RoleResponse.from(roleRepository.update(role));
    }

    @Override
    @Transactional
    public RoleResponse patch(Long id, RolePatchRequest request) {
        PatchGuard.ensureIdUnchanged(id, request.id());
        Role role = getModifiableRole(id);
        if (request.name() != null) {
            ensureNameIsUnique(request.name(), id);
            role.setName(request.name());
        }
        if (request.description() != null) {
            role.setDescription(request.description().trim());
        }
        if (request.permissions() != null) {
            role.replacePermissions(resolvePermissions(request.permissions()));
        }
        return RoleResponse.from(roleRepository.update(role));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Role role = getModifiableRole(id);
        if (roleRepository.countUsersWithRole(id) > 0) {
            throw new ConflictException("Bu rol kullanıcılara atanmış; önce kullanıcıların rolünü değiştirin.");
        }
        roleRepository.delete(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> findAllPermissions() {
        return permissionRepository.findAll().stream().map(PermissionResponse::from).toList();
    }

    private List<Permission> resolvePermissions(Set<String> permissionNames) {
        Set<String> normalized = permissionNames.stream()
                .map(name -> name.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        List<Permission> permissions = permissionRepository.findByNames(normalized);
        if (permissions.size() != normalized.size()) {
            Set<String> found = permissions.stream().map(Permission::getName).collect(Collectors.toSet());
            List<String> unknown = normalized.stream().filter(name -> !found.contains(name)).sorted().toList();
            throw new BadRequestException("Tanımsız yetki: " + unknown);
        }
        return permissions;
    }

    private Role getModifiableRole(Long id) {
        Role role = getRole(id);
        if (role.isSystemRole()) {
            throw new ConflictException("Sistem rolleri (" + role.getName() + ") değiştirilemez veya silinemez; "
                    + "ihtiyaç için yeni bir rol tanımlayın.");
        }
        return role;
    }

    private void ensureNameIsUnique(String name, Long excludeId) {
        roleRepository.findByName(name)
                .filter(existing -> !existing.getId().equals(excludeId))
                .ifPresent(existing -> {
                    throw new ConflictException("Bu isimde bir rol zaten var: " + name);
                });
    }

    private Role getRole(Long id) {
        return roleRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Rol", id));
    }
}

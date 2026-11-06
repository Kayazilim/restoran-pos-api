package com.restoran.pos.security;

import java.util.Set;

/**
 * Kimliği doğrulanmış kullanıcının servis katmanına aktarılan özeti.
 *
 * @param tableId yalnızca masaya bağlı müşteri hesaplarında dolu olur
 */
public record CurrentUser(Long id,
                          String username,
                          Set<String> roles,
                          Set<String> permissions,
                          Long tableId) {

    public CurrentUser {
        roles = Set.copyOf(roles);
        permissions = Set.copyOf(permissions);
    }

    public boolean isTableBound() {
        return tableId != null;
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}

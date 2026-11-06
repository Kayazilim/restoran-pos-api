package com.restoran.pos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Rol ve yetki eşlemesi veritabanında tutulur; yeni roller (ör. paket servis platformu) kod değişmeden eklenebilir.
 */
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "description", nullable = false, length = 200)
    private String description;

    /** Sistemle birlikte gelen beş temel rol değiştirilemez ve silinemez. */
    @Column(name = "system_role", nullable = false)
    private boolean systemRole;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions = new HashSet<>();

    protected Role() {
        // JPA için gerekli
    }

    public Role(String name, String description, boolean systemRole) {
        this.name = name;
        this.description = description;
        this.systemRole = systemRole;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSystemRole() {
        return systemRole;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void replacePermissions(Collection<Permission> newPermissions) {
        permissions.clear();
        permissions.addAll(newPermissions);
    }
}

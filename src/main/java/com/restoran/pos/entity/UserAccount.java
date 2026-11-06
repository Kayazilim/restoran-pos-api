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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Sisteme giriş yapan hesap. Personel hesapları ve masaya bağlı müşteri (QR) hesapları aynı tabloda tutulur;
 * kimlik doğrulama bu tablodaki bilgilerle yapılır.
 */
@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /** Argon2id hash'inin tamamı ($argon2id$v=19$m=...,t=...,p=...$salt$hash) saklanır. */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /** Aktiflik bilgisi: pasif hesaplar kimlik doğrulamasından geçemez (401). */
    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    /** Yalnızca masaya bağlı müşteri hesaplarında dolu olur (kayıt sahipliği denetiminin temeli). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    private RestaurantTable table;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserAccount() {
        // JPA için gerekli
    }

    public UserAccount(String username, String passwordHash, String fullName, boolean enabled) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.enabled = enabled;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(role -> role.getName().equals(roleName));
    }

    public void replaceRoles(Collection<Role> newRoles) {
        roles.clear();
        roles.addAll(newRoles);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public RestaurantTable getTable() {
        return table;
    }

    public void setTable(RestaurantTable table) {
        this.table = table;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

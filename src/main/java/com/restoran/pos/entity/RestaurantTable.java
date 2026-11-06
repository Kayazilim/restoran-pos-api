package com.restoran.pos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * Restorandaki masa. Masaya atanan garson, o masanın sipariş ve adisyonlarının "sahibidir".
 */
@Entity
@Table(name = "restaurant_tables")
public class RestaurantTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "table_number", nullable = false, unique = true)
    private Integer tableNumber;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "location", length = 50)
    private String location;

    @Column(name = "active", nullable = false)
    private boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waiter_id")
    private UserAccount waiter;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RestaurantTable() {
        // JPA için gerekli
    }

    public RestaurantTable(Integer tableNumber, Integer capacity, String location, UserAccount waiter, boolean active) {
        this.tableNumber = tableNumber;
        this.capacity = capacity;
        this.location = location;
        this.waiter = waiter;
        this.active = active;
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

    public Long getId() {
        return id;
    }

    public Integer getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(Integer tableNumber) {
        this.tableNumber = tableNumber;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public UserAccount getWaiter() {
        return waiter;
    }

    public void setWaiter(UserAccount waiter) {
        this.waiter = waiter;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

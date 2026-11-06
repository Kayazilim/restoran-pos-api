package com.restoran.pos.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Bir masaya verilen sipariş. ("Order" JPQL'de ayrılmış kelime olduğundan sınıf adı CustomerOrder'dır.)
 */
@Entity
@Table(name = "orders")
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id", nullable = false)
    private RestaurantTable table;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private UserAccount createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private OrderChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "note", length = 500)
    private String note;

    /**
     * İstemcinin gönderdiği Idempotency-Key. UNIQUE kısıtı, aynı isteğin iki kez işlenmesini
     * veritabanı seviyesinde de kesin olarak engeller.
     */
    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CustomerOrder() {
        // JPA için gerekli
    }

    public CustomerOrder(RestaurantTable table, Bill bill, UserAccount createdBy, OrderChannel channel,
                         String note, String idempotencyKey) {
        this.table = table;
        this.bill = bill;
        this.createdBy = createdBy;
        this.channel = channel;
        this.status = OrderStatus.CONFIRMED;
        this.note = note;
        this.idempotencyKey = idempotencyKey;
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

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    public void clearItems() {
        items.clear();
    }

    public void recalculateTotal() {
        totalAmount = items.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() {
        return id;
    }

    public RestaurantTable getTable() {
        return table;
    }

    public Bill getBill() {
        return bill;
    }

    public UserAccount getCreatedBy() {
        return createdBy;
    }

    public OrderChannel getChannel() {
        return channel;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public List<OrderItem> getItems() {
        return items;
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

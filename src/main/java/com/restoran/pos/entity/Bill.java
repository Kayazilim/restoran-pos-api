package com.restoran.pos.entity;

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
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Adisyon: bir masanın oturumu boyunca verilen siparişlerin hesabı.
 * Masada ilk sipariş verildiğinde açılır, kasiyer tamamen tahsil edince kapatılır.
 */
@Entity
@Table(name = "bills")
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id", nullable = false)
    private RestaurantTable table;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BillStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by_id")
    private UserAccount closedBy;

    @OneToMany(mappedBy = "bill")
    @OrderBy("id ASC")
    private List<CustomerOrder> orders = new ArrayList<>();

    @OneToMany(mappedBy = "bill")
    @OrderBy("id ASC")
    private List<Payment> payments = new ArrayList<>();

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected Bill() {
        // JPA için gerekli
    }

    public Bill(RestaurantTable table, Instant openedAt) {
        this.table = table;
        this.status = BillStatus.OPEN;
        this.openedAt = openedAt;
    }

    public BigDecimal getRemainingAmount() {
        return totalAmount.subtract(paidAmount);
    }

    public void close(UserAccount cashier, Instant closedAt) {
        this.status = BillStatus.CLOSED;
        this.closedBy = cashier;
        this.closedAt = closedAt;
    }

    public Long getId() {
        return id;
    }

    public RestaurantTable getTable() {
        return table;
    }

    public BillStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public UserAccount getClosedBy() {
        return closedBy;
    }

    public List<CustomerOrder> getOrders() {
        return orders;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public Long getVersion() {
        return version;
    }
}

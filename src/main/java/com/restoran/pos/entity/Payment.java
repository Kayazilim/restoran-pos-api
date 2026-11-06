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
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Adisyona alınan ödeme. Idempotency-Key üzerindeki UNIQUE kısıt çifte tahsilatı engeller.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private PaymentMethod method;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "received_by_id", nullable = false)
    private UserAccount receivedBy;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Payment() {
        // JPA için gerekli
    }

    public Payment(Bill bill, BigDecimal amount, PaymentMethod method, UserAccount receivedBy,
                   String idempotencyKey, Instant createdAt) {
        this.bill = bill;
        this.amount = amount;
        this.method = method;
        this.receivedBy = receivedBy;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Bill getBill() {
        return bill;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public UserAccount getReceivedBy() {
        return receivedBy;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

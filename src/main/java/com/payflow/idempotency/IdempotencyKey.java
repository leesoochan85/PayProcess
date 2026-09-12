package com.payflow.idempotency;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="idempotency_key",
        uniqueConstraints = {
        @UniqueConstraint
                (name="uk_idempotency_key",columnNames = "idempotency_key")
        }
    )

public class IdempotencyKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "transfer_id")
    private Long transferId;

    @Column(name="from_account_id", nullable=false)
    private Long fromAccountId;

    @Column(name="to_account_id", nullable=false)
    private Long toAccountId;

    @Column(nullable=false)
    private Long amount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected IdempotencyKey(){
    }


    public IdempotencyKey(String idempotencyKey, Long transferId, Long fromAccountId, Long toAccountId, Long amount) {
        this.idempotencyKey = idempotencyKey;
        this.transferId = transferId;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Long getTransferId() {
        return transferId;
    }

    public Long getFromAccountId() {
        return fromAccountId;
    }

    public Long getToAccountId() {
        return toAccountId;
    }

    public Long getAmount() {
        return amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isSameRequest(Long fromAccountId, Long toAccountId, Long amount){
        return this.fromAccountId.equals(fromAccountId) && this.toAccountId.equals(toAccountId) && this.amount.equals(amount);
    }

}

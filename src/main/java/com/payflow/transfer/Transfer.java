package com.payflow.transfer;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long fromAccountId;
    private Long  toAccountId;
    private Long amount;

    @Enumerated(EnumType.STRING)
    private TransferStatus status;

    private LocalDateTime createdAt;

    protected Transfer(){
    }

    public Transfer(Long fromAccountId, Long toAccountId, Long amount) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.status = TransferStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public void success(){
        this.status = TransferStatus.SUCCESS;
    }

    public void fail(){
        this.status = TransferStatus.FAILED;
    }

    public Long getId() {
        return id;
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

    public TransferStatus getStatus(){
        return status;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

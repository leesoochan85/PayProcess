package com.payflow.transfer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    Page<Transfer> findByFromAccountId(Long fromAccountId, Pageable pageable);

    Page<Transfer> findByToAccountId(Long toAccountId, Pageable pageable);

    Page<Transfer> findByFromAccountIdOrToAccountId(Long fromAccountId,Long toAccountId, Pageable pageable);
}

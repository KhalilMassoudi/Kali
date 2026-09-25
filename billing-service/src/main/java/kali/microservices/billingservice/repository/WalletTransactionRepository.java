package kali.microservices.billingservice.repository;

import kali.microservices.billingservice.entities.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<WalletTransaction> findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAscIdAsc(
            Long userId, LocalDateTime from, LocalDateTime to);

    Optional<WalletTransaction> findFirstByUserIdAndCreatedAtLessThanOrderByCreatedAtDescIdDesc(Long userId, LocalDateTime before);
}

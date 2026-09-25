package kali.microservices.billingservice.service;

import kali.microservices.billingservice.entities.UsageRecord;
import kali.microservices.billingservice.entities.Wallet;
import kali.microservices.billingservice.entities.WalletTransaction;
import kali.microservices.billingservice.repository.UsageRecordRepository;
import kali.microservices.billingservice.repository.WalletRepository;
import kali.microservices.billingservice.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final UsageRecordRepository usageRecordRepository;
    private final PricingService pricingService;

    @Transactional
    public Wallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId).orElseGet(() -> {
            Wallet wallet = new Wallet();
            wallet.setUserId(userId);
            wallet.setBalance(BigDecimal.ZERO);
            return walletRepository.save(wallet);
        });
    }

    public List<WalletTransaction> getTransactions(Long userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Wallet> getAllWallets() {
        return walletRepository.findAll();
    }

    @Transactional
    public Wallet recharge(Long userId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }
        return applyTransaction(userId, WalletTransaction.TransactionType.RECHARGE, amount,
                description != null ? description : "Recharge de crédit", null, null);
    }

    @Transactional
    public Wallet adminAdjust(Long userId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Le montant ne peut pas être nul");
        }
        return applyTransaction(userId, WalletTransaction.TransactionType.ADMIN_ADJUSTMENT, amount,
                description != null ? description : "Ajustement administrateur", null, null);
    }

    /**
     * Called by MeteringService - amount passed in is positive cost, stored as a negative ledger entry,
     * along with the tick's per-resource-type breakdown (saved atomically, linked to that entry).
     */
    @Transactional
    public void deductForUsage(Long userId, BigDecimal cost, String description, String resourceType, Long resourceId,
                               List<UsageRecord> breakdown) {
        if (cost.compareTo(BigDecimal.ZERO) <= 0) return;
        WalletTransaction tx = applyLedgerEntry(userId, WalletTransaction.TransactionType.USAGE_DEDUCTION, cost.negate(),
                description, resourceType, resourceId);
        Long transactionId = tx.getId();
        for (UsageRecord record : breakdown) {
            record.setUserId(userId);
            record.setTransactionId(transactionId);
            usageRecordRepository.save(record);
        }
    }

    @Transactional
    public void markLowBalanceAlertSent(Long userId) {
        Wallet wallet = getOrCreateWallet(userId);
        wallet.setLowBalanceAlertSent(true);
        walletRepository.save(wallet);
    }

    @Transactional
    public void markExhaustedAlertSent(Long userId) {
        Wallet wallet = getOrCreateWallet(userId);
        wallet.setExhaustedAlertSent(true);
        wallet.setLowBalanceAlertSent(true); // exhausted implies low - don't follow up with a "low" email
        walletRepository.save(wallet);
    }

    private Wallet applyTransaction(Long userId, WalletTransaction.TransactionType type, BigDecimal signedAmount,
                                     String description, String resourceType, Long resourceId) {
        applyLedgerEntry(userId, type, signedAmount, description, resourceType, resourceId);
        return getOrCreateWallet(userId);
    }

    private WalletTransaction applyLedgerEntry(Long userId, WalletTransaction.TransactionType type, BigDecimal signedAmount,
                                               String description, String resourceType, Long resourceId) {
        Wallet wallet = getOrCreateWallet(userId);
        wallet.setBalance(wallet.getBalance().add(signedAmount));
        if (signedAmount.signum() > 0) {
            // A credit that lifts the balance back over an alert level re-arms that alert.
            if (wallet.getBalance().compareTo(pricingService.lowBalanceThreshold()) >= 0) wallet.setLowBalanceAlertSent(false);
            if (wallet.getBalance().signum() > 0) wallet.setExhaustedAlertSent(false);
        }
        wallet = walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setType(type);
        tx.setAmount(signedAmount);
        tx.setBalanceAfter(wallet.getBalance());
        tx.setDescription(description);
        tx.setResourceType(resourceType);
        tx.setResourceId(resourceId);
        return transactionRepository.save(tx);
    }
}

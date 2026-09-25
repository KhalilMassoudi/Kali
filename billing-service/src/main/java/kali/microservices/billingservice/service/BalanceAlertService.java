package kali.microservices.billingservice.service;

import kali.microservices.billingservice.client.AuthClient;
import kali.microservices.billingservice.client.InfrastructureClient;
import kali.microservices.billingservice.client.UserSnapshot;
import kali.microservices.billingservice.client.VmSnapshot;
import kali.microservices.billingservice.entities.Wallet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Low-balance / exhausted-credit handling, run after a wallet is debited. Each email goes out
 * once per crossing (Wallet's alert flags; WalletService re-arms them on a credit back above the
 * level). A failed send leaves the flag unset so the next metering tick retries. Zero-balance VM
 * suspension, when enabled, is re-applied on every tick so a VM restarted while still at zero gets
 * stopped again - and is skipped whenever the user's role can't be confirmed as non-admin.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceAlertService {

    private final WalletService walletService;
    private final PricingService pricingService;
    private final AuthClient authClient;
    private final InfrastructureClient infrastructureClient;

    /**
     * @param userVms the user's VMs from the current metering snapshot, or null to fetch them
     *                (only needed if suspension actually kicks in)
     */
    public void evaluate(Long userId, List<VmSnapshot> userVms) {
        Wallet wallet = walletService.getOrCreateWallet(userId);
        BigDecimal balance = wallet.getBalance();
        BigDecimal threshold = pricingService.lowBalanceThreshold();
        String currency = wallet.getCurrency() != null ? wallet.getCurrency() : "TND";

        if (balance.signum() <= 0) {
            boolean suspend = pricingService.suspendVmsOnZeroBalance();
            if (!Boolean.TRUE.equals(wallet.getExhaustedAlertSent())) {
                String body = "Bonjour,\n\n"
                        + "Le crédit de votre compte Safozi Cloud est épuisé (solde actuel : " + format(balance) + " " + currency + ").\n\n"
                        + (suspend
                            ? "Vos machines virtuelles en cours d'exécution vont être arrêtées. Rechargez votre crédit pour pouvoir les redémarrer.\n\n"
                            : "Rechargez votre crédit dès que possible pour éviter toute interruption de service.\n\n")
                        + "Vous pouvez recharger depuis la page Facturation de votre espace client.\n\n"
                        + "L'équipe Safozi Cloud";
                if (authClient.notifyUser(userId, "Crédit épuisé - Safozi Cloud", body)) {
                    walletService.markExhaustedAlertSent(userId);
                }
            }
            if (suspend) {
                suspendRunningVms(userId, userVms);
            }
        } else if (balance.compareTo(threshold) < 0 && !Boolean.TRUE.equals(wallet.getLowBalanceAlertSent())) {
            String body = "Bonjour,\n\n"
                    + "Le solde de votre compte Safozi Cloud est passé sous le seuil d'alerte de " + format(threshold) + " " + currency
                    + " (solde actuel : " + format(balance) + " " + currency + ").\n\n"
                    + "Pensez à recharger votre crédit depuis la page Facturation de votre espace client pour éviter une interruption de service.\n\n"
                    + "L'équipe Safozi Cloud";
            if (authClient.notifyUser(userId, "Solde faible - Safozi Cloud", body)) {
                walletService.markLowBalanceAlertSent(userId);
            }
        }
    }

    private void suspendRunningVms(Long userId, List<VmSnapshot> userVms) {
        List<VmSnapshot> vms = userVms != null ? userVms
                : infrastructureClient.getAllVms().stream().filter(vm -> userId.equals(vm.userId())).toList();
        List<VmSnapshot> running = vms.stream().filter(VmSnapshot::isRunning).toList();
        if (running.isEmpty()) return;

        // Admin accounts are never suspended - and if auth-service can't confirm the role, don't risk it.
        Optional<UserSnapshot> user = authClient.findUser(userId);
        if (user.isEmpty()) {
            log.warn("Suspension skipped for userId={}: role could not be verified with auth-service", userId);
            return;
        }
        if (user.get().isAdmin()) return;

        for (VmSnapshot vm : running) {
            if (infrastructureClient.stopVm(vm.id())) {
                log.info("Suspension: stopped VM {} of userId={} (balance exhausted)", vm.id(), userId);
            }
        }
    }

    private static String format(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}

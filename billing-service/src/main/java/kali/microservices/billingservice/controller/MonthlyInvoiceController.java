package kali.microservices.billingservice.controller;

import kali.microservices.billingservice.client.AuthClient;
import kali.microservices.billingservice.client.UserSnapshot;
import kali.microservices.billingservice.dto.MonthlyInvoice;
import kali.microservices.billingservice.dto.MonthlyInvoiceSummary;
import kali.microservices.billingservice.security.AuthContext;
import kali.microservices.billingservice.security.AuthenticatedUser;
import kali.microservices.billingservice.service.InvoicePdfRenderer;
import kali.microservices.billingservice.service.MonthlyInvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/** Monthly pay-as-you-go invoices - the client for their own account, admins for any client. */
@RestController
@RequestMapping("/api/billing/monthly-invoices")
@RequiredArgsConstructor
public class MonthlyInvoiceController {

    private final MonthlyInvoiceService monthlyInvoiceService;
    private final InvoicePdfRenderer pdfRenderer;
    private final AuthClient authClient;
    private final AuthContext authContext;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<MonthlyInvoiceSummary>> listInvoices(@RequestHeader("Authorization") String authHeader,
                                                                    @PathVariable Long userId) {
        authContext.requireOwnerOrAdmin(authHeader, userId);
        return ResponseEntity.ok(monthlyInvoiceService.listInvoices(userId));
    }

    @GetMapping("/user/{userId}/{month}/pdf")
    public ResponseEntity<byte[]> downloadInvoice(@RequestHeader("Authorization") String authHeader,
                                                  @PathVariable Long userId,
                                                  @PathVariable String month) {
        AuthenticatedUser caller = authContext.requireOwnerOrAdmin(authHeader, userId);
        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mois invalide (format attendu : AAAA-MM)");
        }
        if (yearMonth.isAfter(YearMonth.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune facture pour un mois futur");
        }

        // Name/email live in auth-service: a client reads their own profile, an admin looks the
        // client up in the user list - both with the caller's own token.
        Optional<UserSnapshot> client = userId.equals(caller.userId())
                ? authClient.currentUser(authHeader)
                : authClient.findUser(authHeader, userId);
        String email = client.map(UserSnapshot::email)
                .orElse(userId.equals(caller.userId()) ? caller.email() : null);
        String name = client.map(UserSnapshot::displayName).orElse(email);

        MonthlyInvoice invoice = monthlyInvoiceService.buildInvoice(userId, yearMonth, name, email);
        byte[] pdf = pdfRenderer.render(invoice);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(invoice.summary().invoiceNumber() + ".pdf").build().toString())
                .body(pdf);
    }
}

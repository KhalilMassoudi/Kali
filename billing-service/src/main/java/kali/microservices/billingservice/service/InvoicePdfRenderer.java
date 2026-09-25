package kali.microservices.billingservice.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import kali.microservices.billingservice.dto.MonthlyInvoice;
import kali.microservices.billingservice.dto.MonthlyInvoiceSummary;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Renders a MonthlyInvoice to PDF (OpenPDF, built-in Helvetica - CP1252 covers French accents). */
@Component
public class InvoicePdfRenderer {

    private static final Locale FR = Locale.FRANCE;
    private static final Color BRAND = new Color(0xF9, 0x73, 0x16);   // --brand-primary
    private static final Color MUTED = new Color(0x6B, 0x72, 0x80);
    private static final Color RULE = new Color(0xE5, 0xE7, 0xEB);
    private static final Color HEAD_BG = new Color(0xF3, 0xF4, 0xF6);

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, BRAND);
    private static final Font H2 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
    private static final Font BODY_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
    private static final Font SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED);
    private static final Font TH = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, MUTED);

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMMM yyyy", FR);

    public byte[] render(MonthlyInvoice invoice) {
        MonthlyInvoiceSummary s = invoice.summary();
        YearMonth month = YearMonth.parse(s.month());
        String cur = s.currency();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 42, 42, 42, 42);
        PdfWriter.getInstance(doc, out);
        doc.addTitle("Facture " + s.invoiceNumber());
        doc.addAuthor("Safozi Cloud");
        doc.open();

        // ---- Header: brand on the left, invoice identity on the right ----
        PdfPTable header = table(new float[]{1, 1});
        PdfPCell brand = cell(Element.ALIGN_LEFT);
        brand.addElement(new Phrase("Safozi Cloud", TITLE));
        brand.addElement(new Paragraph("Services cloud à la demande", SMALL));
        header.addCell(brand);
        PdfPCell ident = cell(Element.ALIGN_RIGHT);
        ident.addElement(right(new Paragraph(s.current() ? "FACTURE PROVISOIRE" : "FACTURE", H2)));
        ident.addElement(right(new Paragraph("N° " + s.invoiceNumber(), BODY_BOLD)));
        ident.addElement(right(new Paragraph("Période : " + capitalize(month.format(MONTH_LABEL)), BODY)));
        ident.addElement(right(new Paragraph("Du " + month.atDay(1).format(DAY) + " au " + month.atEndOfMonth().format(DAY), SMALL)));
        ident.addElement(right(new Paragraph("Émise le " + LocalDate.now().format(DAY), SMALL)));
        header.addCell(ident);
        doc.add(header);

        doc.add(spacer(14));

        // ---- Client ----
        doc.add(new Paragraph("Facturé à", SMALL));
        doc.add(new Paragraph(invoice.clientName() != null ? invoice.clientName() : "Client #" + invoice.userId(), BODY_BOLD));
        if (invoice.clientEmail() != null && !invoice.clientEmail().equals(invoice.clientName())) {
            doc.add(new Paragraph(invoice.clientEmail(), BODY));
        }
        doc.add(new Paragraph("Identifiant client : " + invoice.userId(), SMALL));

        doc.add(spacer(14));

        // ---- Summary ----
        doc.add(new Paragraph("Récapitulatif", H2));
        doc.add(spacer(4));
        PdfPTable summary = table(new float[]{3, 1.4f});
        summaryRow(summary, "Solde d'ouverture", money(s.openingBalance(), cur), false);
        summaryRow(summary, "Recharges", "+ " + money(s.rechargeTotal(), cur), false);
        if (s.adjustmentTotal().signum() != 0) {
            summaryRow(summary, "Ajustements", signed(s.adjustmentTotal(), cur), false);
        }
        summaryRow(summary, "Consommation du mois", "- " + money(s.usageTotal(), cur), false);
        summaryRow(summary, "Solde de clôture" + (s.current() ? " (à date)" : ""), money(s.closingBalance(), cur), true);
        doc.add(summary);

        doc.add(spacer(14));

        // ---- Usage line items ----
        doc.add(new Paragraph("Détail de la consommation", H2));
        doc.add(spacer(4));
        PdfPTable usage = table(new float[]{3.2f, 1.3f, 0.8f, 1.3f, 1.4f});
        th(usage, "Désignation", Element.ALIGN_LEFT);
        th(usage, "Quantité", Element.ALIGN_RIGHT);
        th(usage, "Unité", Element.ALIGN_LEFT);
        th(usage, "Prix unitaire", Element.ALIGN_RIGHT);
        th(usage, "Montant", Element.ALIGN_RIGHT);
        if (invoice.usageLines().isEmpty()) {
            PdfPCell empty = td("Aucune consommation sur la période", Element.ALIGN_LEFT, SMALL);
            empty.setColspan(5);
            usage.addCell(empty);
        }
        for (MonthlyInvoice.UsageLine line : invoice.usageLines()) {
            usage.addCell(td(line.label(), Element.ALIGN_LEFT, BODY));
            usage.addCell(td(line.quantity() != null ? quantity(line.quantity()) : "-", Element.ALIGN_RIGHT, BODY));
            usage.addCell(td(line.unit() != null ? line.unit() : "-", Element.ALIGN_LEFT, BODY));
            usage.addCell(td(line.unitPrice() != null ? unitPrice(line.unitPrice()) + " " + cur : "-", Element.ALIGN_RIGHT, BODY));
            usage.addCell(td(money(line.amount(), cur), Element.ALIGN_RIGHT, BODY));
        }
        PdfPCell totalLabel = td("Total consommation", Element.ALIGN_RIGHT, BODY_BOLD);
        totalLabel.setColspan(4);
        usage.addCell(totalLabel);
        usage.addCell(td(money(s.usageTotal(), cur), Element.ALIGN_RIGHT, BODY_BOLD));
        doc.add(usage);

        doc.add(spacer(14));

        // ---- Recharges / adjustments ----
        doc.add(new Paragraph("Recharges et ajustements", H2));
        doc.add(spacer(4));
        PdfPTable credits = table(new float[]{1.2f, 4.4f, 1.4f});
        th(credits, "Date", Element.ALIGN_LEFT);
        th(credits, "Description", Element.ALIGN_LEFT);
        th(credits, "Montant", Element.ALIGN_RIGHT);
        if (invoice.credits().isEmpty()) {
            PdfPCell empty = td("Aucune recharge sur la période", Element.ALIGN_LEFT, SMALL);
            empty.setColspan(3);
            credits.addCell(empty);
        }
        for (MonthlyInvoice.LedgerLine line : invoice.credits()) {
            credits.addCell(td(line.date().toLocalDate().format(DAY), Element.ALIGN_LEFT, BODY));
            credits.addCell(td(line.label(), Element.ALIGN_LEFT, BODY));
            credits.addCell(td(signed(line.amount(), cur), Element.ALIGN_RIGHT, BODY));
        }
        doc.add(credits);

        doc.add(spacer(18));
        doc.add(new Paragraph("Facturation à l'usage : les montants ci-dessus ont été prélevés sur votre crédit prépayé "
                + "au fil de la consommation (calcul facturé uniquement pour les VM actives ; stockage et IP flottantes "
                + "facturés en continu)." + (s.current() ? " Le mois étant en cours, cette facture est provisoire." : ""), SMALL));

        doc.close();
        return out.toByteArray();
    }

    // ---------- helpers ----------

    private static PdfPTable table(float[] widths) {
        PdfPTable t = new PdfPTable(widths);
        t.setWidthPercentage(100);
        return t;
    }

    private static PdfPCell cell(int align) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(align);
        return c;
    }

    private static Paragraph right(Paragraph p) {
        p.setAlignment(Element.ALIGN_RIGHT);
        return p;
    }

    private static void th(PdfPTable t, String text, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text.toUpperCase(FR), TH));
        c.setHorizontalAlignment(align);
        c.setBackgroundColor(HEAD_BG);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(RULE);
        c.setPadding(5);
        t.addCell(c);
    }

    private static PdfPCell td(String text, int align, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setHorizontalAlignment(align);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(RULE);
        c.setPadding(5);
        return c;
    }

    private static void summaryRow(PdfPTable t, String label, String value, boolean strong) {
        Font f = strong ? BODY_BOLD : BODY;
        PdfPCell l = td(label, Element.ALIGN_LEFT, f);
        PdfPCell v = td(value, Element.ALIGN_RIGHT, f);
        if (strong) {
            l.setBackgroundColor(HEAD_BG);
            v.setBackgroundColor(HEAD_BG);
        }
        t.addCell(l);
        t.addCell(v);
    }

    private static Paragraph spacer(float height) {
        Paragraph p = new Paragraph(" ", BODY);
        p.setSpacingAfter(height - 9);
        return p;
    }

    private static DecimalFormat format(String pattern) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(FR);
        symbols.setGroupingSeparator(' '); // plain space: FR's narrow no-break space isn't in CP1252
        DecimalFormat format = new DecimalFormat(pattern, symbols);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format;
    }

    private static String money(BigDecimal amount, String currency) {
        return format("#,##0.00").format(amount) + " " + currency;
    }

    private static String signed(BigDecimal amount, String currency) {
        return (amount.signum() >= 0 ? "+ " : "- ") + money(amount.abs(), currency);
    }

    private static String quantity(BigDecimal q) {
        return format("#,##0.###").format(q);
    }

    private static String unitPrice(BigDecimal p) {
        return format("0.00####").format(p);
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

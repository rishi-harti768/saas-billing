package org.gb.billing.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.gb.billing.entity.Payment;
import org.gb.billing.entity.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Service for generating PDF invoices.
 * Stores invoices in the file system for MVP.
 */
@Service
public class InvoiceService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${invoice.storage.path:./invoices}")
    private String invoiceStoragePath;

    /**
     * Generate a PDF invoice for a payment.
     *
     * @param payment the payment record
     * @param subscription the subscription details
     * @return the file path to the generated invoice
     */
    public String generateInvoice(Payment payment, Subscription subscription) throws Exception {
        logger.info("Generating invoice for payment {}", payment.getId());

        // Create invoices directory if not exists
        Path invoiceDir = Paths.get(invoiceStoragePath);
        if (!Files.exists(invoiceDir)) {
            Files.createDirectories(invoiceDir);
        }

        // Generate invoice filename
        String filename = String.format("invoice_%s_%s.pdf",
                payment.getId().toString(),
                payment.getCreatedAt().toEpochMilli());
        Path invoicePath = invoiceDir.resolve(filename);

        // Create PDF document
        try (FileOutputStream fos = new FileOutputStream(invoicePath.toFile());
             PdfWriter writer = new PdfWriter(fos);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {

            // Add title
            Paragraph title = new Paragraph("INVOICE")
                    .setFontSize(24)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);

            // Add invoice details
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Invoice ID: " + payment.getId()));
            document.add(new Paragraph("Date: " + formatDate(payment.getCreatedAt())));
            document.add(new Paragraph("Payment Status: " + payment.getStatus()));
            document.add(new Paragraph("\n"));

            // Add customer details
            document.add(new Paragraph("Customer Information").setBold());
            document.add(new Paragraph("User ID: " + subscription.getUserId()));
            document.add(new Paragraph("Tenant ID: " + subscription.getTenantId()));
            document.add(new Paragraph("\n"));

            // Add subscription details
            document.add(new Paragraph("Subscription Details").setBold());
            document.add(new Paragraph("Plan: " + subscription.getPlan().getName()));
            document.add(new Paragraph("Billing Cycle: " + subscription.getPlan().getBillingCycle()));
            document.add(new Paragraph("Subscription ID: " + subscription.getId()));
            document.add(new Paragraph("\n"));

            // Add payment details table
            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 2}));
            table.setWidth(UnitValue.createPercentValue(100));

            table.addHeaderCell("Description");
            table.addHeaderCell("Amount");

            table.addCell("Subscription - " + subscription.getPlan().getName());
            table.addCell(String.format("%s %s", payment.getAmount(), payment.getCurrency()));

            table.addCell("Payment Method");
            table.addCell(payment.getPaymentMethod());

            table.addCell("Total");
            table.addCell(String.format("%s %s", payment.getAmount(), payment.getCurrency()));

            document.add(table);

            // Add footer
            document.add(new Paragraph("\n\n"));
            document.add(new Paragraph("Thank you for your business!")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setItalic());

            document.add(new Paragraph("Payment ID: " + payment.getExternalPaymentId())
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(8));
        }

        logger.info("Invoice generated successfully: {}", invoicePath);
        return invoicePath.toString();
    }

    /**
     * Get invoice file for download.
     */
    public File getInvoiceFile(String invoiceUrl) {
        return new File(invoiceUrl);
    }

    private String formatDate(java.time.Instant instant) {
        return instant.atZone(ZoneId.systemDefault()).format(DATE_FORMATTER);
    }
}

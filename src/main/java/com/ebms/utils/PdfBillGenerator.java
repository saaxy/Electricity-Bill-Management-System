package com.ebms.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.ebms.model.Consumer;
import com.ebms.service.BillService;

/**
 * Simple, dependency-free PDF generator.
 * This class writes a minimal PDF (Type1 /Helvetica) using a small custom writer
 * so the project does NOT need PDFBox on the classpath.
 */
public class PdfBillGenerator {
    private static final String BILLS_DIR = "src/bills";

    public static File generate(Consumer consumer, double units, double amount) throws IOException {
        // Ensure directory exists
        File dir = new File(BILLS_DIR);
        if (!dir.exists()) dir.mkdirs();
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = String.format("bill_%d_%s.pdf", consumer.getId(), ts);
        File out = new File(dir, fileName);

        BillService bs = new BillService();
        double tax = 0.0, lateFee = 0.0, discount = 0.0;
        double finalAmount = bs.calculateFinalAmount(amount, tax, lateFee, discount);

        StringBuilder sb = new StringBuilder();
        sb.append("ELECTRICITY BILL\n");
        sb.append("=================\n\n");
        sb.append("Consumer Details\n");
        sb.append(String.format("Name: %s\n", safe(consumer.getName())));
        sb.append(String.format("Email: %s\n", safe(consumer.getEmail())));
        sb.append(String.format("Address: %s\n", safe(consumer.getAddress())));
        sb.append(String.format("Meter No.: %s\n", safe(consumer.getMeterNumber())));
        sb.append(String.format("Consumer ID: %d\n\n", consumer.getId()));
        sb.append(bs.generateBillSummary(consumer.getId(), units, amount, tax, lateFee, discount, finalAmount));

        byte[] pdfBytes = buildFallbackPdf(sb.toString());
        try (FileOutputStream fos = new FileOutputStream(out)) {
            fos.write(pdfBytes);
        }

        return out;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    // Small, dependency-free PDF writer producing a very simple single-page PDF using
    // the Type1 Helvetica font. This is intentionally minimal and aims for broad
    // compatibility without external libs.
    private static byte[] buildFallbackPdf(String text) throws IOException {
        // Escape backslashes and parentheses for PDF string literals
        String escaped = text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
        String[] lines = escaped.split("\\r?\\n");

        StringBuilder contentStream = new StringBuilder();
        contentStream.append("BT /F1 12 Tf 50 750 Td 16 TL\n");
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) contentStream.append("T*\n");
            contentStream.append("(").append(lines[i]).append(") Tj\n");
        }
        contentStream.append("ET\n");

        byte[] cs = contentStream.toString().getBytes(StandardCharsets.US_ASCII);

        String obj1 = "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n";
        String obj2 = "2 0 obj << /Type /Pages /Count 1 /Kids [3 0 R] >> endobj\n";
        String obj3 = "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >> endobj\n";
        String obj4Header = "4 0 obj << /Length " + cs.length + " >> stream\n";
        String obj4Footer = "endstream endobj\n";
        String obj5 = "5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n";

        StringBuilder pdf = new StringBuilder();
        pdf.append("%PDF-1.4\n");
        int o1 = pdf.length(); pdf.append(obj1);
        int o2 = pdf.length(); pdf.append(obj2);
        int o3 = pdf.length(); pdf.append(obj3);
        int o4 = pdf.length(); pdf.append(obj4Header);
        pdf.append(new String(cs, StandardCharsets.US_ASCII));
        pdf.append("\n");
        pdf.append(obj4Footer);
        int o5 = pdf.length(); pdf.append(obj5);

        int xref = pdf.length();
        pdf.append("xref\n0 6\n");
        pdf.append(String.format("%010d %05d f \n", 0, 65535));
        pdf.append(String.format("%010d %05d n \n", o1, 00000));
        pdf.append(String.format("%010d %05d n \n", o2, 00000));
        pdf.append(String.format("%010d %05d n \n", o3, 00000));
        pdf.append(String.format("%010d %05d n \n", o4, 00000));
        pdf.append(String.format("%010d %05d n \n", o5, 00000));
        pdf.append("trailer << /Root 1 0 R /Size 6 >>\nstartxref\n");
        pdf.append(xref).append("\n%%EOF");

        return pdf.toString().getBytes(StandardCharsets.US_ASCII);
    }
}

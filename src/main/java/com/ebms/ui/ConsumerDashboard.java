package com.ebms.ui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.ebms.controller.UserSession;
import com.ebms.dao.BillDAO;
import com.ebms.dao.ComplaintDAO;
import com.ebms.dao.ConsumerDAO;
import com.ebms.model.BillSummary;
import com.ebms.model.Complaint;
import com.ebms.service.EmailService;

public class ConsumerDashboard extends JFrame {
    private final UserSession session;
    private final ConsumerDAO consumerDAO = new ConsumerDAO();
    private final BillDAO billDAO = new BillDAO();
    private final ComplaintDAO complaintDAO = new ComplaintDAO();
    private final EmailService emailService = new EmailService();

    public ConsumerDashboard(UserSession session) {
        super("Consumer Dashboard");
        this.session = session;
        initUI();
    }

    private void initUI() {
        setSize(600, 360);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8,8));
        JLabel title = new JLabel("Consumer Dashboard", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        add(title, BorderLayout.NORTH);

    JPanel center = new JPanel(new GridLayout(6,2,8,8));
        JButton genBtn = new JButton("Generate bill");
        JButton payBtn = new JButton("Pay bill");
    JButton markPaidBtn = new JButton("Mark bill paid");
    JButton getComplaintStatusBtn = new JButton("Get Complaint Status");
        JButton editBtn = new JButton("Edit consumer");
        JButton detailsBtn = new JButton("My Details");
        JButton complaintBtn = new JButton("Register Complaint");
        JButton changePwdBtn = new JButton("Change password");
        JButton logoutBtn = new JButton("Logout");
        JButton exitBtn = new JButton("Exit App");

        genBtn.addActionListener(this::onGenerate);
        payBtn.addActionListener(this::onPayBill);
    markPaidBtn.addActionListener(this::onMarkPaid);
    getComplaintStatusBtn.addActionListener(this::onGetComplaintStatus);
        editBtn.addActionListener(this::onEdit);
        detailsBtn.addActionListener(this::onDetails);
        complaintBtn.addActionListener(this::onComplaint);
        changePwdBtn.addActionListener(this::onChangePassword);
        logoutBtn.addActionListener(this::onLogout);
        exitBtn.addActionListener(this::onExit);

        center.add(genBtn);
        center.add(payBtn);
    center.add(getComplaintStatusBtn);
        center.add(editBtn);
        center.add(detailsBtn);
        center.add(complaintBtn);
        center.add(changePwdBtn);
        center.add(logoutBtn);
    center.add(exitBtn);
    center.add(markPaidBtn);

        add(center, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
    }

    // Add consumer is now an admin-only operation. The consumer dashboard no longer exposes it.

    private void onGenerate(java.awt.event.ActionEvent e) {
        int id = session.getConsumerId();
        String sstr = JOptionPane.showInputDialog(this, "Enter units consumed:", "Generate Bill", JOptionPane.PLAIN_MESSAGE);
        if (sstr == null) return;
        try {
            double units = Double.parseDouble(sstr.trim());
            com.ebms.model.BillResult br = billDAO.generateBillReturnId(id, units);
            var consumer = consumerDAO.getById(id);
            java.io.File pdf = br != null ? br.getPdf() : null;
            if (br != null) {
                // offer immediate pay
                int yn = JOptionPane.showConfirmDialog(this, "Bill generated. Do you want to pay now?", "Pay Now", JOptionPane.YES_NO_OPTION);
                if (yn == JOptionPane.YES_OPTION) {
                    // attempt to open checkout
                    openCheckoutForBill(br.getId(), br.getAmount());
                }
            }
            if (pdf != null && consumer != null && consumer.getEmail() != null && !consumer.getEmail().isBlank()) {
                int yn = JOptionPane.showConfirmDialog(this, "Email this bill to " + consumer.getEmail() + "?", "Send Email", JOptionPane.YES_NO_OPTION);
                if (yn == JOptionPane.YES_OPTION) {
                    String subject = "Your Electricity Bill (Consumer ID " + consumer.getId() + ")";
                    String text = "Hello " + consumer.getName() + ",\n\nPlease find your electricity bill attached.\n\nThank you.";
                    boolean sent = emailService.sendBill(consumer.getEmail(), subject, text, pdf);
                    JOptionPane.showMessageDialog(this, sent?"Email sent":"Email not sent. Check logs or EMAIL_ENABLED");
                }
            }
            JOptionPane.showMessageDialog(this, pdf != null ? "Bill generated: " + (br!=null?"ID="+br.getId()+" ":"") + pdf.getAbsolutePath() : "Failed to generate bill");
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Invalid number", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onEdit(java.awt.event.ActionEvent e) {
        int id = session.getConsumerId();
        var existing = consumerDAO.getById(id);
        if (existing == null) { JOptionPane.showMessageDialog(this, "No consumer found for ID="+id); return; }
        // include phone in editable profile
        JPanel p = new JPanel(new GridLayout(6,2,6,6));
        JTextField name = new JTextField(existing.getName());
        JTextField addr = new JTextField(existing.getAddress());
        JTextField email = new JTextField(existing.getEmail());
        JTextField meter = new JTextField(existing.getMeterNumber());
        JTextField dob = new JTextField(existing.getBirthdate()!=null?existing.getBirthdate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")):"");
        JTextField phone = new JTextField(existing.getPhone()!=null?existing.getPhone():"");
        p.add(new JLabel("Name:")); p.add(name);
        p.add(new JLabel("Address:")); p.add(addr);
        p.add(new JLabel("Email:")); p.add(email);
        p.add(new JLabel("Meter Number:")); p.add(meter);
        p.add(new JLabel("Birthdate (DD/MM/YYYY):")); p.add(dob);
        p.add(new JLabel("Phone:")); p.add(phone);
        int res = JOptionPane.showConfirmDialog(this, p, "Edit Consumer", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;
        try {
            existing.setName(name.getText().trim());
            existing.setAddress(addr.getText().trim());
            existing.setEmail(email.getText().trim());
            existing.setMeterNumber(meter.getText().trim());
            existing.setPhone(phone.getText().trim());
            if (!dob.getText().isBlank()) existing.setBirthdate(java.time.LocalDate.parse(dob.getText().trim(), java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            consumerDAO.update(existing);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onDetails(java.awt.event.ActionEvent e) {
        int id = session.getConsumerId();
        var c = consumerDAO.getById(id);
        if (c == null) { JOptionPane.showMessageDialog(this, "No consumer found"); return; }
        StringBuilder sb = new StringBuilder();
        sb.append("ID: ").append(c.getId()).append('\n');
        sb.append("Name: ").append(c.getName()).append('\n');
        sb.append("Email: ").append(c.getEmail()).append('\n');
        sb.append("Phone: ").append(c.getPhone()!=null?c.getPhone():"").append('\n');
        sb.append("Address: ").append(c.getAddress()).append('\n');
        sb.append("Meter: ").append(c.getMeterNumber()).append('\n');
        JOptionPane.showMessageDialog(this, sb.toString(), "My Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void onComplaint(java.awt.event.ActionEvent e) {
        String desc = JOptionPane.showInputDialog(this, "Enter complaint description:");
        if (desc == null || desc.isBlank()) return;
        Complaint comp = new Complaint(desc.trim(), "OPEN");
        int id = complaintDAO.addComplaint(comp);
        JOptionPane.showMessageDialog(this, id>0?"Complaint recorded. ID="+id:"Failed to record complaint");
    }

    private void onChangePassword(java.awt.event.ActionEvent e) {
        int id = session.getConsumerId();
        String oldPwd = JOptionPane.showInputDialog(this, "Enter current password:");
        if (oldPwd == null) return;
        boolean ok = consumerDAO.authenticate(id, oldPwd);
        if (!ok) { JOptionPane.showMessageDialog(this, "Invalid current password"); return; }
        String np1 = JOptionPane.showInputDialog(this, "Enter new password:");
        if (np1 == null || np1.isBlank()) return;
        String np2 = JOptionPane.showInputDialog(this, "Confirm new password:");
        if (!np1.equals(np2)) { JOptionPane.showMessageDialog(this, "Passwords do not match"); return; }
        boolean changed = consumerDAO.changePassword(id, np1);
        JOptionPane.showMessageDialog(this, changed?"Password changed":"Failed to change password");
    }

    private void onLogout(java.awt.event.ActionEvent e) { dispose(); }
    private void onExit(java.awt.event.ActionEvent e) { System.exit(0); }

    private void onGetComplaintStatus(java.awt.event.ActionEvent e) {
        String s = javax.swing.JOptionPane.showInputDialog(this, "Enter complaint ID:");
        if (s == null) return;
        try {
            int id = Integer.parseInt(s.trim());
            Complaint c = complaintDAO.getById(id);
            if (c == null) { JOptionPane.showMessageDialog(this, "Complaint not found"); return; }
            StringBuilder sb = new StringBuilder();
            sb.append("ID: ").append(c.getId()).append('\n');
            sb.append("Description: ").append(c.getDescription()).append('\n');
            sb.append("Status: ").append(c.getStatus()).append('\n');
            JOptionPane.showMessageDialog(this, sb.toString(), "Complaint Status", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid number");
        }
    }

    private void onPayBill(java.awt.event.ActionEvent e) {
        try {
            int cid = session.getConsumerId();
            java.util.List<BillSummary> unpaid = billDAO.getUnpaidBillsForConsumer(cid);
            if (unpaid.isEmpty()) { JOptionPane.showMessageDialog(this, "No unpaid bills found"); return; }
            BillSummary choice = (BillSummary) JOptionPane.showInputDialog(this, "Select bill to pay:", "Pay bill", JOptionPane.PLAIN_MESSAGE, null, unpaid.toArray(), unpaid.get(0));
            if (choice == null) return;
            int billId = choice.getId();
            double amount = choice.getAmount();
            long amountPaise = Math.round(amount * 100);
            String url = String.format("http://localhost:8080/create-payment-link?billId=%d&amountPaise=%d", billId, amountPaise);
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest req = HttpRequest.newBuilder().uri(new URI(url)).GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) { JOptionPane.showMessageDialog(this, "Failed to create order: " + resp.body()); return; }
            String body = resp.body();
            // extract short_url from payment-link response
            String shortUrl = null;
            String marker = "\"short_url\":\"";
            int idx = body.indexOf(marker);
            if (idx >= 0) {
                int start = idx + marker.length();
                int end = body.indexOf('"', start);
                if (end > start) shortUrl = body.substring(start, end);
            }
            if (shortUrl == null) { JOptionPane.showMessageDialog(this, "No payment link returned by server"); return; }
            Desktop.getDesktop().browse(new URI(shortUrl));
            JOptionPane.showMessageDialog(this, "Opened checkout page in browser. Complete payment there. The app will be updated when webhook triggers.");
        } catch (ConnectException ce) {
            JOptionPane.showMessageDialog(this, "Payment server not available. Start the local payment dev server (RazorpayServerRunner) or ensure http://localhost:8080 is reachable.\nDetails: " + ce.getMessage(), "Payment error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Payment error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void openCheckoutForBill(int billId, double amount) {
        try {
            long amountPaise = Math.round(amount * 100);
            String url = String.format("http://localhost:8080/create-payment-link?billId=%d&amountPaise=%d", billId, amountPaise);
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest req = HttpRequest.newBuilder().uri(new URI(url)).GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) { JOptionPane.showMessageDialog(this, "Failed to create order: " + resp.body()); return; }
            String body = resp.body();
            // extract short_url from payment-link response
            String shortUrl = null;
            String marker = "\"short_url\":\"";
            int idx = body.indexOf(marker);
            if (idx >= 0) {
                int start = idx + marker.length();
                int end = body.indexOf('"', start);
                if (end > start) shortUrl = body.substring(start, end);
            }
            if (shortUrl == null) { JOptionPane.showMessageDialog(this, "No payment link returned by server"); return; }
            Desktop.getDesktop().browse(new URI(shortUrl));
            JOptionPane.showMessageDialog(this, "Opened checkout page in browser. Complete payment there. The app will be updated when webhook triggers.");
        } catch (ConnectException ce) {
            JOptionPane.showMessageDialog(this, "Payment server not available. Start the local payment dev server (RazorpayServerRunner) or ensure http://localhost:8080 is reachable.\nDetails: " + ce.getMessage(), "Payment error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Payment error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void onMarkPaid(java.awt.event.ActionEvent e) {
        int cid = session.getConsumerId();
        java.util.List<BillSummary> unpaid = billDAO.getUnpaidBillsForConsumer(cid);
        if (unpaid.isEmpty()) { JOptionPane.showMessageDialog(this, "No unpaid bills found"); return; }
        BillSummary choice = (BillSummary) JOptionPane.showInputDialog(this, "Select bill to mark paid:", "Mark Paid", JOptionPane.PLAIN_MESSAGE, null, unpaid.toArray(), unpaid.get(0));
        if (choice == null) return;
        String pid = JOptionPane.showInputDialog(this, "Enter payment ID (from Razorpay):");
        if (pid == null || pid.isBlank()) { JOptionPane.showMessageDialog(this, "Payment ID required to mark paid"); return; }
        boolean ok = billDAO.markBillAsPaid(choice.getId(), pid.trim());
        JOptionPane.showMessageDialog(this, ok?"Bill marked as PAID":"Failed to mark bill paid. Check logs.");
    }
}

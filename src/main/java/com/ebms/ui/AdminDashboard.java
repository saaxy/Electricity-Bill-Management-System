package com.ebms.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.ebms.controller.UserSession;
import com.ebms.dao.AdminDAO;
import com.ebms.dao.ComplaintDAO;
import com.ebms.dao.ConsumerDAO;
import com.ebms.model.Consumer;

public class AdminDashboard extends JFrame {
    private final UserSession session;
    private final AdminDAO adminDAO = new AdminDAO();
    private final ConsumerDAO consumerDAO = new ConsumerDAO();
    private final ComplaintDAO complaintDAO = new ComplaintDAO();

    public AdminDashboard(UserSession session) {
        super("Admin Dashboard");
        this.session = session;
        initUI();
    }

    // Bridge methods to accept ActionEvent for method references
    private void onAddConsumer(java.awt.event.ActionEvent e) { onAddConsumer(); }
    private void onViewConsumers(java.awt.event.ActionEvent e) { onViewConsumers(); }
    private void onDeleteConsumer(java.awt.event.ActionEvent e) { onDeleteConsumer(); }
    private void onGetDetails(java.awt.event.ActionEvent e) { onGetDetails(); }
    private void onViewComplaints(java.awt.event.ActionEvent e) { onViewComplaints(); }
    private void onChangePassword(java.awt.event.ActionEvent e) { onChangePassword(); }
    private void onLogout(java.awt.event.ActionEvent e) { onLogout(); }
    private void onExit(java.awt.event.ActionEvent e) { onExit(); }
    private void onUpdateComplaint(java.awt.event.ActionEvent e) { onUpdateComplaintBridge(); }

    private void initUI() {
        setSize(640, 360);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8,8));
        JLabel title = new JLabel("Admin Dashboard", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        add(title, BorderLayout.NORTH);

    JPanel center = new JPanel(new GridLayout(3,3,8,8));
    JButton addConsumer = new JButton("Add consumer");
    JButton viewConsumers = new JButton("View Consumers");
    JButton updateComplaint = new JButton("Update Complaint Status");
    JButton deleteConsumer = new JButton("Delete Consumer");
    JButton details = new JButton("Get Consumer Details");
    JButton viewComplaints = new JButton("View Complaints");
    JButton changePwd = new JButton("Change my password");
    JButton logout = new JButton("Logout");
    JButton exitBtn = new JButton("Exit App");

    addConsumer.addActionListener(this::onAddConsumer);
    viewConsumers.addActionListener(this::onViewConsumers);
    updateComplaint.addActionListener(this::onUpdateComplaint);
    deleteConsumer.addActionListener(this::onDeleteConsumer);
    details.addActionListener(this::onGetDetails);
    viewComplaints.addActionListener(this::onViewComplaints);
    changePwd.addActionListener(this::onChangePassword);
    logout.addActionListener(this::onLogout);
    exitBtn.addActionListener(this::onExit);
    center.add(addConsumer);
    center.add(viewConsumers);
    center.add(updateComplaint);
    center.add(deleteConsumer);
    center.add(details);
    center.add(viewComplaints);
    center.add(changePwd);
    center.add(logout);
    center.add(exitBtn);

        add(center, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
    }

    private void onViewConsumers() {
    java.util.List<com.ebms.model.Consumer> list = consumerDAO.getAllConsumers();
        if (list == null || list.isEmpty()) { JOptionPane.showMessageDialog(this, "No consumers found"); return; }
        StringBuilder sb = new StringBuilder();
        for (var c : list) {
            sb.append(c.getId()).append(" - ").append(c.getName()).append(" - ").append(c.getEmail());
            if (c.getPhone() != null && !c.getPhone().isBlank()) sb.append(" - ").append(c.getPhone());
            sb.append('\n');
        }
        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(560,240));
        JOptionPane.showMessageDialog(this, sp, "Consumers", JOptionPane.PLAIN_MESSAGE);
    }

    private void onAddConsumer() {
    JPanel p = new JPanel(new GridLayout(6,2,6,6));
        JTextField name = new JTextField();
        JTextField addr = new JTextField();
        JTextField email = new JTextField();
        JTextField meter = new JTextField();
    JTextField phone = new JTextField();
        JTextField dob = new JTextField();
        p.add(new JLabel("Name:")); p.add(name);
        p.add(new JLabel("Address:")); p.add(addr);
        p.add(new JLabel("Email:")); p.add(email);
        p.add(new JLabel("Meter Number:")); p.add(meter);
    p.add(new JLabel("Phone:")); p.add(phone);
        p.add(new JLabel("Birthdate (DD/MM/YYYY):")); p.add(dob);
        int res = JOptionPane.showConfirmDialog(this, p, "Add Consumer", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;
        try {
            java.time.LocalDate bd = java.time.LocalDate.of(1999,1,1);
            if (!dob.getText().isBlank()) bd = java.time.LocalDate.parse(dob.getText().trim(), java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            Consumer c = new Consumer(name.getText().trim(), addr.getText().trim(), email.getText().trim(), meter.getText().trim(), bd, null);
            c.setPhone(phone.getText().trim());
            int id = consumerDAO.addConsumer(c);
            JOptionPane.showMessageDialog(this, id>0?"Consumer added with ID="+id:"Failed to add consumer");
        } catch (java.time.format.DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Invalid birthdate format: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    

    private void onDeleteConsumer() {
        String s = JOptionPane.showInputDialog(this, "Enter consumer ID to delete:");
        if (s==null) return;
        try {
            int id = Integer.parseInt(s.trim());
            boolean ok = consumerDAO.deleteById(id);
            JOptionPane.showMessageDialog(this, ok?"Deleted":"Delete failed");
        } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Invalid number"); }
    }

    private void onGetDetails() {
        String s = JOptionPane.showInputDialog(this, "Enter consumer ID:");
        if (s==null) return;
        try {
            int id = Integer.parseInt(s.trim());
            var c = consumerDAO.getById(id);
            if (c==null) { JOptionPane.showMessageDialog(this, "Not found"); return; }
            StringBuilder sb = new StringBuilder();
            sb.append("ID: ").append(c.getId()).append('\n');
            sb.append("Name: ").append(c.getName()).append('\n');
            sb.append("Email: ").append(c.getEmail()).append('\n');
            sb.append("Phone: ").append(c.getPhone()).append('\n');
            sb.append("Address: ").append(c.getAddress()).append('\n');
            sb.append("Meter: ").append(c.getMeterNumber()).append('\n');
            JOptionPane.showMessageDialog(this, sb.toString(), "Consumer Details", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Invalid number"); }
    }

    private void onViewComplaints() {
        java.util.List<com.ebms.model.Complaint> list = complaintDAO.getAll();
        if (list==null || list.isEmpty()) { JOptionPane.showMessageDialog(this, "No complaints"); return; }
        StringBuilder sb = new StringBuilder();
        for (var c: list) sb.append(c.getId()).append(" - ").append(c.getDescription()).append(" - ").append(c.getStatus()).append('\n');
        JTextArea area = new JTextArea(sb.toString()); area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Complaints", JOptionPane.PLAIN_MESSAGE);
    }

    private void onChangePassword() {
        String cur = JOptionPane.showInputDialog(this, "Enter current password:");
        if (cur==null) return;
        boolean ok = adminDAO.authenticate(session.getAdminUsername(), cur);
        if (!ok) { JOptionPane.showMessageDialog(this, "Invalid current password"); return; }
        String np = JOptionPane.showInputDialog(this, "Enter new password:");
        if (np==null || np.isBlank()) return;
        String np2 = JOptionPane.showInputDialog(this, "Confirm new password:");
        if (!np.equals(np2)) { JOptionPane.showMessageDialog(this, "Passwords do not match"); return; }
        boolean changed = adminDAO.changePassword(session.getAdminUsername(), np);
        JOptionPane.showMessageDialog(this, changed?"Password changed":"Failed to change password");
    }

    private void onLogout() { dispose(); }
    private void onExit() { System.exit(0); }

    // Internal handler invoked by bridge method for updating complaint status
    private void onUpdateComplaintBridge() {
        String s = JOptionPane.showInputDialog(this, "Enter complaint ID to close:");
        if (s == null) return;
        try {
            int id = Integer.parseInt(s.trim());
            var existing = complaintDAO.getById(id);
            if (existing == null) { JOptionPane.showMessageDialog(this, "Complaint not found"); return; }
            String current = existing.getStatus();
            if (current != null && current.equalsIgnoreCase("CLOSE")) {
                JOptionPane.showMessageDialog(this, "Complaint is already CLOSED");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this, "Mark complaint #" + id + " as CLOSE?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
            boolean ok = complaintDAO.updateStatus(id, "CLOSE");
            JOptionPane.showMessageDialog(this, ok ? "Complaint closed." : "Failed to update complaint status.");
        } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Invalid number"); }
    }
}

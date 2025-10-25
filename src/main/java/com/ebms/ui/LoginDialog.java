package com.ebms.ui;

import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.ebms.controller.UserSession;
import com.ebms.dao.AdminDAO;
import com.ebms.dao.ConsumerDAO;
import com.ebms.service.EmailService;

public class LoginDialog {
    public static UserSession showLoginForConsumer(Component parent) {
        ConsumerDAO dao = new ConsumerDAO();
        JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        panel.add(new JLabel("Consumer ID:"));
        JTextField idField = new JTextField();
        panel.add(idField);
        panel.add(new JLabel("Password (passcode):"));
        JPasswordField pwd = new JPasswordField();
        panel.add(pwd);

        Object[] options = {"Login", "Forgot Password", "Cancel"};
        int res = JOptionPane.showOptionDialog(parent, panel, "Consumer Login",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (res == 0) { // Login
            try {
                int id = Integer.parseInt(idField.getText().trim());
                String pass = new String(pwd.getPassword());
                boolean ok = dao.authenticate(id, pass);
                if (ok) {
                    UserSession s = new UserSession(UserSession.Role.CONSUMER);
                    s.setConsumerId(id);
                    return s;
                } else {
                    JOptionPane.showMessageDialog(parent, "Login failed: invalid ID or password", "Error", JOptionPane.ERROR_MESSAGE);
                    return null;
                }
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(parent, "Invalid Consumer ID", "Error", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        } else if (res == 1) { // Forgot Password
            String idText = JOptionPane.showInputDialog(parent, "Enter your Consumer ID:");
            if (idText == null) return null;
            int id;
            try {
                id = Integer.parseInt(idText.trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(parent, "Invalid Consumer ID", "Error", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            com.ebms.model.Consumer c = dao.getById(id);
            if (c == null) {
                JOptionPane.showMessageDialog(parent, "No consumer found with that ID", "Error", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            String bdText = JOptionPane.showInputDialog(parent, "Enter your birthdate (DD/MM/YYYY):");
            if (bdText == null) return null;
            try {
                java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                java.time.LocalDate provided = java.time.LocalDate.parse(bdText.trim(), fmt);
                java.time.LocalDate actual = c.getBirthdate();
                if (actual == null || !actual.equals(provided)) {
                    JOptionPane.showMessageDialog(parent, "Birthdate does not match our records", "Error", JOptionPane.ERROR_MESSAGE);
                    return null;
                }
                // Offer to send a reset email or reset directly
                Object[] opts = {"Reset now", "Send reset email", "Cancel"};
                int choice = JOptionPane.showOptionDialog(parent, "Choose reset method:", "Password Reset",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opts, opts[0]);
                if (choice == 0) {
                    // Reset now (masked input)
                    JPasswordField np1 = new JPasswordField();
                    JPasswordField np2 = new JPasswordField();
                    JPanel pp = new JPanel(new GridLayout(2,2,8,8));
                    pp.add(new JLabel("New password:")); pp.add(np1);
                    pp.add(new JLabel("Confirm new password:")); pp.add(np2);
                    int r = JOptionPane.showConfirmDialog(parent, pp, "Reset Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                    if (r != JOptionPane.OK_OPTION) return null;
                    String s1 = new String(np1.getPassword());
                    String s2 = new String(np2.getPassword());
                    if (s1.isBlank()) { JOptionPane.showMessageDialog(parent, "Password cannot be empty", "Error", JOptionPane.ERROR_MESSAGE); return null; }
                    if (!s1.equals(s2)) { JOptionPane.showMessageDialog(parent, "Passwords do not match", "Error", JOptionPane.ERROR_MESSAGE); return null; }
                    boolean changed = dao.changePassword(id, s1);
                    if (changed) {
                        JOptionPane.showMessageDialog(parent, "Password updated. You are now logged in.");
                        UserSession s = new UserSession(UserSession.Role.CONSUMER);
                        s.setConsumerId(id);
                        return s;
                    } else {
                        JOptionPane.showMessageDialog(parent, "Failed to update password. Contact admin.", "Error", JOptionPane.ERROR_MESSAGE);
                        return null;
                    }
                } else if (choice == 1) {
                    // Send reset email with one-time code
                    String to = c.getEmail();
                    if (to == null || to.isBlank()) { JOptionPane.showMessageDialog(parent, "No email on file for this consumer.", "Error", JOptionPane.ERROR_MESSAGE); return null; }
                    String code = String.format("%06d", new java.util.Random().nextInt(1_000_000));
                    EmailService es = new EmailService();
                    String subject = "Password reset code";
                    String body = "Your password reset code is: " + code + "\nIf you did not request this, ignore this email.";
                    boolean sent = es.sendBill(to, subject, body, null);
                    if (!sent) {
                        int opt = JOptionPane.showConfirmDialog(parent, "Failed to send reset email (EMAIL_ENABLED or SMTP may be misconfigured). Reset now instead?", "Email failed", JOptionPane.YES_NO_OPTION);
                        if (opt != JOptionPane.YES_OPTION) return null;
                        // fallback to reset now
                        JPasswordField np1 = new JPasswordField();
                        JPasswordField np2 = new JPasswordField();
                        JPanel pp = new JPanel(new GridLayout(2,2,8,8));
                        pp.add(new JLabel("New password:")); pp.add(np1);
                        pp.add(new JLabel("Confirm new password:")); pp.add(np2);
                        int r = JOptionPane.showConfirmDialog(parent, pp, "Reset Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                        if (r != JOptionPane.OK_OPTION) return null;
                        String s1 = new String(np1.getPassword());
                        String s2 = new String(np2.getPassword());
                        if (s1.isBlank()) { JOptionPane.showMessageDialog(parent, "Password cannot be empty", "Error", JOptionPane.ERROR_MESSAGE); return null; }
                        if (!s1.equals(s2)) { JOptionPane.showMessageDialog(parent, "Passwords do not match", "Error", JOptionPane.ERROR_MESSAGE); return null; }
                        boolean changed = dao.changePassword(id, s1);
                        if (changed) {
                            JOptionPane.showMessageDialog(parent, "Password updated. You are now logged in.");
                            UserSession s = new UserSession(UserSession.Role.CONSUMER);
                            s.setConsumerId(id);
                            return s;
                        } else {
                            JOptionPane.showMessageDialog(parent, "Failed to update password. Contact admin.", "Error", JOptionPane.ERROR_MESSAGE);
                            return null;
                        }
                    }
                    // If email sent, prompt for code
                    String entered = JOptionPane.showInputDialog(parent, "Enter the 6-digit code sent to your email:");
                    if (entered == null) return null;
                    if (!entered.trim().equals(code)) { JOptionPane.showMessageDialog(parent, "Code mismatch", "Error", JOptionPane.ERROR_MESSAGE); return null; }
                    // code matches, proceed to prompt new password
                    JPasswordField np1 = new JPasswordField();
                    JPasswordField np2 = new JPasswordField();
                    JPanel pp = new JPanel(new GridLayout(2,2,8,8));
                    pp.add(new JLabel("New password:")); pp.add(np1);
                    pp.add(new JLabel("Confirm new password:")); pp.add(np2);
                    int r = JOptionPane.showConfirmDialog(parent, pp, "Reset Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                    if (r != JOptionPane.OK_OPTION) return null;
                    String s1 = new String(np1.getPassword());
                    String s2 = new String(np2.getPassword());
                    if (s1.isBlank()) { JOptionPane.showMessageDialog(parent, "Password cannot be empty", "Error", JOptionPane.ERROR_MESSAGE); return null; }
                    if (!s1.equals(s2)) { JOptionPane.showMessageDialog(parent, "Passwords do not match", "Error", JOptionPane.ERROR_MESSAGE); return null; }
                    boolean changed = dao.changePassword(id, s1);
                    if (changed) {
                        JOptionPane.showMessageDialog(parent, "Password updated. You are now logged in.");
                        UserSession s = new UserSession(UserSession.Role.CONSUMER);
                        s.setConsumerId(id);
                        return s;
                    } else {
                        JOptionPane.showMessageDialog(parent, "Failed to update password. Contact admin.", "Error", JOptionPane.ERROR_MESSAGE);
                        return null;
                    }
                } else {
                    return null;
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent, "Invalid birthdate format", "Error", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        } else {
            return null;
        }
    }

    public static UserSession showLoginForAdmin(Component parent) {
        AdminDAO dao = new AdminDAO();
        JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        panel.add(new JLabel("Admin username:"));
        JTextField user = new JTextField();
        panel.add(user);
        panel.add(new JLabel("Password:"));
        JPasswordField pwd = new JPasswordField();
        panel.add(pwd);

        int res = JOptionPane.showConfirmDialog(parent, panel, "Admin Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return null;
        String uname = user.getText().trim();
        String pass = new String(pwd.getPassword());
        boolean ok = dao.authenticate(uname, pass);
        if (ok) {
            UserSession s = new UserSession(UserSession.Role.ADMIN);
            s.setAdminUsername(uname);
            return s;
        } else {
            JOptionPane.showMessageDialog(parent, "Login failed: invalid username or password", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }
}

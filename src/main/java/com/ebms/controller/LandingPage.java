package com.ebms.controller;

import java.util.Scanner;

import com.ebms.dao.AdminDAO;
import com.ebms.dao.ConsumerDAO;
import com.ebms.service.EmailService;

public class LandingPage {
    /**
     * Runs the landing/login page. Returns a UserSession on successful login
     * or null if the user chose to quit.
     */
    public static UserSession run(Scanner sc) {
        AdminDAO adminDAO = new AdminDAO();
        ConsumerDAO consumerDAO = new ConsumerDAO();

        while (true) {
            System.out.println("\n=== Welcome to Electricity Bill Management (Landing) ===");
            System.out.println("1) Login as Consumer");
            System.out.println("2) Login as Admin");
            System.out.println("3) Quit");
            System.out.print("Choose option: ");
            int opt = -1;
            try {
                opt = Integer.parseInt(sc.nextLine().trim());
            } catch (Exception e) {
                System.out.println("Invalid input. Try again.");
                continue;
            }

            switch (opt) {
                case 1 -> {
                    System.out.print("Enter Consumer ID: ");
                    String idStr = sc.nextLine().trim();
                    int id = -1;
                    try { id = Integer.parseInt(idStr); } catch (NumberFormatException nfe) { System.out.println("Invalid ID."); break; }

                    System.out.println("1) Enter password to login");
                    System.out.println("2) Forgot password");
                    System.out.print("Choose option: ");
                    String choice = sc.nextLine().trim();
                    if (choice.equals("1")) {
                        System.out.print("Enter password (passcode): ");
                        String pwd = sc.nextLine();
                        boolean ok = consumerDAO.authenticate(id, pwd);
                        if (ok) {
                            UserSession s = new UserSession(UserSession.Role.CONSUMER);
                            s.setConsumerId(id);
                            System.out.println("✅ Consumer login successful. Welcome!");
                            return s;
                        } else {
                            System.out.println("❌ Consumer login failed. Check ID/password.");
                        }
                    } else if (choice.equals("2")) {
                        var c = consumerDAO.getById(id);
                        if (c == null) { System.out.println("No consumer found with that ID."); break; }
                        System.out.print("Enter your birthdate (DD/MM/YYYY): ");
                        String bdStr = sc.nextLine().trim();
                        try {
                            var fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                            var provided = java.time.LocalDate.parse(bdStr, fmt);
                            var actual = c.getBirthdate();
                            if (actual == null || !actual.equals(provided)) { System.out.println("Birthdate does not match our records."); break; }
                            // Offer to send reset email or reset now
                            System.out.println("1) Reset now");
                            System.out.println("2) Send reset email");
                            System.out.print("Choose option: ");
                            String rchoice = sc.nextLine().trim();
                            if (rchoice.equals("1")) {
                                System.out.print("Enter new password: ");
                                String np1 = sc.nextLine();
                                System.out.print("Confirm new password: ");
                                String np2 = sc.nextLine();
                                if (np1 == null || np1.isBlank()) { System.out.println("Password cannot be empty."); break; }
                                if (!np1.equals(np2)) { System.out.println("Passwords do not match."); break; }
                                boolean changed = consumerDAO.changePassword(id, np1);
                                if (changed) {
                                    System.out.println("Password updated. You are now logged in.");
                                    UserSession s = new UserSession(UserSession.Role.CONSUMER);
                                    s.setConsumerId(id);
                                    return s;
                                } else {
                                    System.out.println("Failed to update password. Contact admin.");
                                }
                            } else if (rchoice.equals("2")) {
                                String to = c.getEmail();
                                if (to == null || to.isBlank()) { System.out.println("No email on file for this consumer."); break; }
                                String code = String.format("%06d", new java.util.Random().nextInt(1_000_000));
                                EmailService es = new EmailService();
                                String subject = "Password reset code";
                                String body = "Your password reset code is: " + code + "\nIf you did not request this, ignore this email.";
                                boolean sent = es.sendBill(to, subject, body, null);
                                if (!sent) {
                                    System.out.println("Failed to send reset email (EMAIL_ENABLED or SMTP may be misconfigured). Reset now instead? (y/n)");
                                    String yn = sc.nextLine().trim();
                                    if (!"y".equalsIgnoreCase(yn)) break;
                                    System.out.print("Enter new password: ");
                                    String np1 = sc.nextLine();
                                    System.out.print("Confirm new password: ");
                                    String np2 = sc.nextLine();
                                    if (np1 == null || np1.isBlank()) { System.out.println("Password cannot be empty."); break; }
                                    if (!np1.equals(np2)) { System.out.println("Passwords do not match."); break; }
                                    boolean changed = consumerDAO.changePassword(id, np1);
                                    if (changed) {
                                        System.out.println("Password updated. You are now logged in.");
                                        UserSession s = new UserSession(UserSession.Role.CONSUMER);
                                        s.setConsumerId(id);
                                        return s;
                                    } else {
                                        System.out.println("Failed to update password. Contact admin.");
                                    }
                                } else {
                                    System.out.print("Enter the 6-digit code sent to your email: ");
                                    String entered = sc.nextLine().trim();
                                    if (!entered.equals(code)) { System.out.println("Code mismatch"); break; }
                                    System.out.print("Enter new password: ");
                                    String np1 = sc.nextLine();
                                    System.out.print("Confirm new password: ");
                                    String np2 = sc.nextLine();
                                    if (np1 == null || np1.isBlank()) { System.out.println("Password cannot be empty."); break; }
                                    if (!np1.equals(np2)) { System.out.println("Passwords do not match."); break; }
                                    boolean changed = consumerDAO.changePassword(id, np1);
                                    if (changed) {
                                        System.out.println("Password updated. You are now logged in.");
                                        UserSession s = new UserSession(UserSession.Role.CONSUMER);
                                        s.setConsumerId(id);
                                        return s;
                                    } else {
                                        System.out.println("Failed to update password. Contact admin.");
                                    }
                                }
                            } else {
                                System.out.println("Invalid choice.");
                            }
                        } catch (Exception ex) {
                            System.out.println("Invalid date format. Use DD/MM/YYYY.");
                        }
                    } else {
                        System.out.println("Invalid choice.");
                    }
                }
                case 2 -> {
                    System.out.print("Enter Admin username: ");
                    String uname = sc.nextLine();
                    System.out.print("Enter Admin password: ");
                    String pwd = sc.nextLine();
                    boolean ok = adminDAO.authenticate(uname, pwd);
                    if (ok) {
                        UserSession s = new UserSession(UserSession.Role.ADMIN);
                        s.setAdminUsername(uname);
                        System.out.println("✅ Admin login successful. Welcome, " + uname + "!");
                        return s;
                    } else {
                        System.out.println("❌ Admin login failed. Check username/password.");
                    }
                }
                case 3 -> {
                    System.out.println("Goodbye!");
                    return null;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
}

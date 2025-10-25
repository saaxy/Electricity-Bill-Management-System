package com.ebms.controller;

// src/main/java/com/ebms/controller/MainApp.java


import java.util.Scanner;

import com.ebms.dao.AdminDAO;
import com.ebms.dao.BillDAO;
import com.ebms.dao.ComplaintDAO;
import com.ebms.dao.ConsumerDAO;
import com.ebms.model.Complaint;
import com.ebms.model.Consumer;
import com.ebms.service.EmailService;
import com.ebms.ui.LandingFrame;
import com.ebms.utils.DefaultDataSeeder;

public class MainApp {
    public static void main(String[] args) {
    try (Scanner sc = new Scanner(System.in)) {
        // Ensure a default consumer and a default bill exist at startup
        Integer defaultId = DefaultDataSeeder.seedDefaultConsumer();
        if (defaultId != null) {
            com.ebms.utils.DefaultDataSeeder.seedDefaultBillIfMissing(defaultId);
        }
    ConsumerDAO consumerDAO = new ConsumerDAO();
        BillDAO billDAO = new BillDAO();
    AdminDAO adminDAO = new AdminDAO();
    ComplaintDAO complaintDAO = new ComplaintDAO();
    EmailService emailService = new EmailService();

        // If environment variable EBMS_CLI is set to "1" we keep the CLI flow.
        // Otherwise start the Swing landing frame on the Event Dispatch Thread and exit main (GUI owns lifecycle).
        String ebmsCli = System.getenv("EBMS_CLI");
        if (ebmsCli == null || !ebmsCli.equals("1")) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    LandingFrame lf = new LandingFrame();
                    lf.setVisible(true);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            });
            return;
        }

        while (true) {
            // Landing page: authenticate and obtain a session (CLI fallback)
            UserSession session = LandingPage.run(sc);
            if (session == null) { return; }

            if (session.getRole() == UserSession.Role.CONSUMER) {
                int sessionConsumerId = session.getConsumerId();
                // Consumer menu (authenticated for consumer id) - add-consumer moved to admin
                while (true) {
                    System.out.println("\n--- Consumer Menu ---");
                    System.out.println("1) Generate bill");
                    System.out.println("2) Edit consumer");
                    System.out.println("3) Get Consumer Details (your account)");
                    System.out.println("4) Register Complaint");
                    System.out.println("5) Change password");
                    System.out.println("6) Logout");
                    System.out.print("Enter choice: ");
                    int ch = Integer.parseInt(sc.nextLine().trim());
                    switch (ch) {
                        case 1 -> {
                            int id = sessionConsumerId;
                            System.out.print("Enter units consumed: ");
                            double units = Double.parseDouble(sc.nextLine().trim());
                            java.io.File pdf = billDAO.generateBill(id, units);
                            var consumer = consumerDAO.getById(id);
                            if (pdf != null && consumer != null && consumer.getEmail() != null && !consumer.getEmail().isBlank()) {
                                System.out.print("Email this bill to " + consumer.getEmail() + "? (y/n): ");
                                String ans = sc.nextLine();
                                if (ans != null && (ans.equalsIgnoreCase("y") || ans.equalsIgnoreCase("yes"))) {
                                    String subject = "Your Electricity Bill (Consumer ID " + consumer.getId() + ")";
                                    String text = "Hello " + consumer.getName() + ",\n\nPlease find your electricity bill attached.\n\nThank you.";
                                    boolean sent = emailService.sendBill(consumer.getEmail(), subject, text, pdf);
                                    System.out.println(sent ? "📨 Email sent." : "Email not sent. See logs for details or ensure EMAIL_ENABLED=true.");
                                } else {
                                    System.out.println("📄 Bill PDF saved: " + pdf.getPath());
                                }
                            }
                        }
                        case 2 -> {
                            int editId = sessionConsumerId;
                            var existing = consumerDAO.getById(editId);
                            if (existing == null) { System.out.println("ℹ️ No consumer found with ID=" + editId); break; }

                            System.out.println("Leave blank to keep current value.");
                            System.out.println("Current Name: " + existing.getName());
                            System.out.print("New Name: ");
                            String name = sc.nextLine();
                            if (!name.isBlank()) existing.setName(name);

                            System.out.println("Current Address: " + existing.getAddress());
                            System.out.print("New Address: ");
                            String addr = sc.nextLine();
                            if (!addr.isBlank()) existing.setAddress(addr);

                            System.out.println("Current Email: " + existing.getEmail());
                            System.out.print("New Email: ");
                            String email = sc.nextLine();
                            if (!email.isBlank()) existing.setEmail(email);

                            System.out.println("Current Meter Number: " + existing.getMeterNumber());
                            System.out.print("New Meter Number: ");
                            String meter = sc.nextLine();
                            if (!meter.isBlank()) existing.setMeterNumber(meter);

                            java.time.format.DateTimeFormatter dispFmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                            String currentDob = (existing.getBirthdate() != null) ? existing.getBirthdate().format(dispFmt) : "(not set)";
                            System.out.println("Current Birthdate: " + currentDob);
                            System.out.print("New Birthdate (DD/MM/YYYY, blank to keep): ");
                            String dobStr = sc.nextLine();
                            if (!dobStr.isBlank()) {
                                try {
                                    var fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                                    var bd = java.time.LocalDate.parse(dobStr.trim(), fmt);
                                    existing.setBirthdate(bd);
                                    String pass = String.format("%02d%02d%02d", bd.getDayOfMonth(), bd.getMonthValue(), bd.getYear() % 100);
                                    existing.setPasscode(pass);
                                    System.out.println("Passcode updated to: " + pass + " (use this next time to log in)");
                                } catch (Exception e) {
                                    System.out.println("Invalid date format. Birthdate unchanged.");
                                }
                            }

                            consumerDAO.update(existing);
                        }
                        case 3 -> {
                            int cid = sessionConsumerId;
                            var c = consumerDAO.getById(cid);
                            if (c == null) {
                                System.out.println("ℹ️ No consumer found with ID=" + cid);
                            } else {
                                System.out.println("\n--- Consumer Details ---");
                                System.out.println("ID         : " + c.getId());
                                System.out.println("Name       : " + c.getName());
                                System.out.println("Address    : " + c.getAddress());
                                System.out.println("Email      : " + c.getEmail());
                                System.out.println("Phone      : " + c.getPhone());
                                System.out.println("Meter No.  : " + c.getMeterNumber());
                            }
                        }
                        case 4 -> {
                            System.out.print("Enter complaint description: ");
                            String desc = sc.nextLine();
                            if (desc == null || desc.isBlank()) {
                                System.out.println("Description cannot be empty.");
                                break;
                            }
                            Complaint comp = new Complaint(desc.trim(), "OPEN");
                            int compId = complaintDAO.addComplaint(comp);
                            if (compId > 0) {
                                System.out.println("Your complaint has been recorded. Complaint ID: " + compId);
                            } else {
                                System.out.println("Failed to record complaint. Please try again.");
                            }
                        }
                        case 5 -> {
                            int cid = sessionConsumerId;
                            System.out.print("Enter current password: ");
                            String oldPwd = sc.nextLine();
                            boolean ok = consumerDAO.authenticate(cid, oldPwd);
                            if (!ok) {
                                System.out.println("❌ Invalid current password or consumer ID.");
                                break;
                            }
                            System.out.print("Enter new password: ");
                            String np1 = sc.nextLine();
                            System.out.print("Confirm new password: ");
                            String np2 = sc.nextLine();
                            if (np1 == null || np1.isBlank()) { System.out.println("New password cannot be empty."); break; }
                            if (!np1.equals(np2)) { System.out.println("Passwords do not match."); break; }
                            if (np1.equals(oldPwd)) { System.out.println("New password must be different from old password."); break; }
                            boolean changed = consumerDAO.changePassword(cid, np1);
                            System.out.println(changed ? "✅ Password updated successfully." : "Failed to update password.");
                        }
                        case 6 -> {
                            // Logout to landing page
                            break;
                        }
                        default -> System.out.println("Invalid choice!");
                    }
                    if (ch == 6) break; // logout consumer
                }
            } else if (session.getRole() == UserSession.Role.ADMIN) {
                String adminUser = session.getAdminUsername();
                // Admin menu (authenticated as adminUser)
                while (true) {
                    System.out.println("\n--- Admin Menu ---");
                    System.out.println("1) Add consumer");
                    System.out.println("2) View Consumers");
                    System.out.println("3) Delete Consumer (admin)");
                    System.out.println("4) Get Consumer Details by ID");
                    System.out.println("5) View Complaints");
                    System.out.println("6) Change Password");
                    System.out.println("7) Logout");
                    System.out.print("Enter choice: ");
                    int ch = Integer.parseInt(sc.nextLine().trim());
                    switch (ch) {
                        case 1 -> {
                            System.out.print("Name: ");
                            String name = sc.nextLine();
                            System.out.print("Address: ");
                            String addr = sc.nextLine();
                            System.out.print("Email: ");
                            String email = sc.nextLine();
                            System.out.print("Meter Number: ");
                            String meter = sc.nextLine();
                            System.out.print("Phone (optional): ");
                            String phone = sc.nextLine();
                            System.out.print("Birthdate (DD/MM/YYYY, blank for 01/01/1999): ");
                            String dobStr = sc.nextLine();
                            java.time.LocalDate bd;
                            if (dobStr == null || dobStr.isBlank()) {
                                bd = java.time.LocalDate.of(1999, 1, 1);
                            } else {
                                try {
                                    java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                                    bd = java.time.LocalDate.parse(dobStr.trim(), fmt);
                                } catch (Exception e) {
                                    System.out.println("Invalid date format. Defaulting to 01/01/1999.");
                                    bd = java.time.LocalDate.of(1999, 1, 1);
                                }
                            }
                            String passcode = String.format("%02d%02d%02d", bd.getDayOfMonth(), bd.getMonthValue(), bd.getYear() % 100);
                            Consumer newConsumer = new Consumer(name, addr, email, meter, bd, passcode);
                            newConsumer.setPhone(phone == null ? null : phone.trim());
                            int newId = consumerDAO.addConsumer(newConsumer);
                            if (newId > 0) {
                                System.out.println("Note this Consumer ID for billing: " + newId);
                                System.out.println("Initial password (derived from DOB): " + passcode);
                            } else {
                                System.out.println("Failed to retrieve new Consumer ID. Check logs/errors.");
                            }
                        }
                        case 2 -> {
                            System.out.println("\n--- Consumer List ---");
                            consumerDAO.getAllConsumers().forEach(c ->
                                    System.out.println(c.getId() + " | " + c.getName() + " | " + c.getMeterNumber()));
                        }
                        case 3 -> {
                            // Already authenticated as adminUser
                            System.out.print("Enter Consumer ID to delete: ");
                            int delId = Integer.parseInt(sc.nextLine().trim());
                            consumerDAO.deleteById(delId);
                        }
                        case 4 -> {
                            System.out.print("Enter Consumer ID: ");
                            int cid = sc.nextInt(); sc.nextLine();
                            var c = consumerDAO.getById(cid);
                            if (c == null) {
                                System.out.println("ℹ️ No consumer found with ID=" + cid);
                            } else {
                                System.out.println("\n--- Consumer Details ---");
                                System.out.println("ID         : " + c.getId());
                                System.out.println("Name       : " + c.getName());
                                System.out.println("Address    : " + c.getAddress());
                                System.out.println("Email      : " + c.getEmail());
                                System.out.println("Phone      : " + c.getPhone());
                                System.out.println("Meter No.  : " + c.getMeterNumber());
                            }
                        }
                        case 5 -> {
                            System.out.println("\n--- Complaints ---");
                            var list = complaintDAO.getAll();
                            if (list.isEmpty()) {
                                System.out.println("No complaints found.");
                            } else {
                                list.forEach(c -> {
                                    String snippet = c.getDescription();
                                    if (snippet != null && snippet.length() > 50) snippet = snippet.substring(0, 50) + "...";
                                    System.out.println(c.getId() + " | " + c.getStatus() + " | " + (snippet == null ? "" : snippet));
                                });
                            }
                        }
                        case 6 -> {
                            // Change password for authenticated adminUser
                            System.out.print("Current password: ");
                            String oldPwd = sc.nextLine();
                            boolean ok = adminDAO.authenticate(adminUser, oldPwd);
                            if (!ok) { System.out.println("❌ Authentication failed."); break; }
                            System.out.print("Enter new password: ");
                            String np1 = sc.nextLine();
                            System.out.print("Confirm new password: ");
                            String np2 = sc.nextLine();
                            if (np1 == null || np1.isBlank()) { System.out.println("New password cannot be empty."); break; }
                            if (!np1.equals(np2)) { System.out.println("Passwords do not match."); break; }
                            if (np1.equals(oldPwd)) { System.out.println("New password must be different from old password."); break; }
                            boolean changed = adminDAO.changePassword(adminUser, np1);
                            System.out.println(changed ? "✅ Password updated successfully." : "Failed to update password.");
                        }
                        case 7 -> { break; }
                        default -> System.out.println("Invalid choice!");
                    }
                    if (ch == 7) break; // logout admin
                }
            } else {
                System.out.println("Invalid session role. Returning to landing.");
            }
        }
        }
    }
}

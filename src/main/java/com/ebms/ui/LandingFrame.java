package com.ebms.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.ebms.controller.UserSession;

public class LandingFrame extends JFrame {
    public LandingFrame() {
        super("Electricity Bill Management - Landing");
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 220);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        JLabel title = new JLabel("Welcome to Electricity Bill Management", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        add(title, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new GridLayout(1, 3, 8, 8));

        JButton consumerBtn = new JButton("Consumer");
        JButton adminBtn = new JButton("Admin");
        JButton quitBtn = new JButton("Quit");

    consumerBtn.addActionListener(this::onConsumer);
    adminBtn.addActionListener(this::onAdmin);
    quitBtn.addActionListener(this::onQuit);

        center.add(consumerBtn);
        center.add(adminBtn);
        center.add(quitBtn);
        add(center, BorderLayout.CENTER);

        JLabel hint = new JLabel("Select your role to continue.", SwingConstants.CENTER);
        add(hint, BorderLayout.SOUTH);

        // Ensure clean exit on close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
    }

    private void onConsumer(java.awt.event.ActionEvent e) {
        UserSession s = LoginDialog.showLoginForConsumer(this);
        if (s != null) {
            ConsumerDashboard dash = new ConsumerDashboard(s);
            dash.setVisible(true);
            this.setVisible(false);
            dash.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    LandingFrame.this.setVisible(true);
                }
            });
        }
    }

    private void onAdmin(java.awt.event.ActionEvent e) {
        UserSession s = LoginDialog.showLoginForAdmin(this);
        if (s != null) {
            AdminDashboard dash = new AdminDashboard(s);
            dash.setVisible(true);
            this.setVisible(false);
            dash.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    LandingFrame.this.setVisible(true);
                }
            });
        }
    }

    private void onQuit(java.awt.event.ActionEvent e) {
        dispose();
        System.exit(0);
    }
}

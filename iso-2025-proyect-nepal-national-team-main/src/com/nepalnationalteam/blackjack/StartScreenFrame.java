package com.nepalnationalteam.blackjack;

import javax.swing.*;
import java.awt.*;

public class StartScreenFrame extends JFrame {
    private final FirebaseService firebase = new FirebaseService();

    public StartScreenFrame() {
        setTitle("Nepal National Team Blackjack");
        setSize(520, 420);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(Color.BLACK);
        setLayout(null);

        JLabel title = new JLabel("Nepal National Team Blackjack", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setBounds(20, 30, 480, 30);
        add(title);

        JButton btnLogin = ui("Login");
        JButton btnRegister = ui("Register");
        btnLogin.setBounds(170, 120, 180, 44);
        btnRegister.setBounds(170, 180, 180, 44);
        add(btnLogin); add(btnRegister);

        btnLogin.addActionListener(e -> { dispose(); new LoginFrame(this).setVisible(true); });
        btnRegister.addActionListener(e -> { dispose(); new RegisterFrame(this).setVisible(true); });

        SessionManager.Session s = SessionManager.load();
        if (s != null && s.rememberMe && s.refreshToken != null && !s.refreshToken.isEmpty()) {
            try {
                String newIdToken = firebase.refreshIdToken(s.refreshToken);
                if (newIdToken != null && !newIdToken.isEmpty()) {
                    s.idToken = newIdToken;
                    SessionManager.save(s);
                    SwingUtilities.invokeLater(() -> { dispose(); new LobbyFrame(s.email, s.displayName).setVisible(true); });
                }
            } catch (Exception ex) {
                System.err.println("Auto-login refresh failed: " + ex.getMessage());
            }
        }
    }

    private JButton ui(String t) {
        JButton b = new JButton(t);
        b.setBackground(new Color(30,30,30));
        b.setForeground(new Color(220,220,220));
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setBorder(BorderFactory.createLineBorder(new Color(70,70,70)));
        return b;
    }
}

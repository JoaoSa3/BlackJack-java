
package com.nepalnationalteam.blackjackdesktop;

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

        JLabel title = new JLabel("Nepal National Team Blackjack");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setBounds(60, 30, 400, 30);
        add(title);

        JButton btnLogin = new JButton("Login");
        JButton btnRegister = new JButton("Register");
        btnLogin.setBounds(170, 120, 180, 40);
        btnRegister.setBounds(170, 180, 180, 40);
        style(btnLogin); style(btnRegister);
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

    void style(JButton b) {
        b.setBackground(new Color(30,30,30));
        b.setForeground(new Color(220,220,220));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(new Color(70,70,70)));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StartScreenFrame().setVisible(true));
    }
}

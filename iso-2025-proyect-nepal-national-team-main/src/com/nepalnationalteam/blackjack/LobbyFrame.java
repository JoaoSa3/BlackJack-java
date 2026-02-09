package com.nepalnationalteam.blackjack;

import javax.swing.*;
import java.awt.*;

public class LobbyFrame extends JFrame {
    public LobbyFrame(String email, String displayName) {
        setTitle("Lobby");
        setSize(520, 320);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(Color.BLACK);
        setLayout(null);

        String who = (displayName == null || displayName.isEmpty()) ? email : displayName;

        JLabel hello = new JLabel("Hello, " + who, SwingConstants.CENTER);
        hello.setForeground(new Color(230, 230, 230));
        hello.setFont(new Font("Segoe UI", Font.BOLD, 22));
        hello.setBounds(20, 30, 480, 40);
        add(hello);

        JButton btnLogout = ui("Logout");
        btnLogout.setBounds(80, 110, 160, 40);
        add(btnLogout);

        JButton btnStart = ui("Start Game");
        btnStart.setBounds(280, 110, 160, 40);
        add(btnStart);

        btnLogout.addActionListener(e -> {
            SessionManager.clear();
            JOptionPane.showMessageDialog(this, "Logged out.");
            dispose();
            new StartScreenFrame().setVisible(true);
        });

        btnStart.addActionListener(e -> new GameFrame().setVisible(true));
    }

    private JButton ui(String t) {
        JButton b = new JButton(t);
        b.setBackground(new Color(240, 240, 240));
        b.setForeground(Color.BLACK);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60,60,60)),
                BorderFactory.createEmptyBorder(6,12,6,12)
        ));
        return b;
    }
}

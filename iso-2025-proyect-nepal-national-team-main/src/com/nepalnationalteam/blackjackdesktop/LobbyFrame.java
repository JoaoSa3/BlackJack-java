
package com.nepalnationalteam.blackjackdesktop;

import javax.swing.*;
import java.awt.*;

public class LobbyFrame extends JFrame {
    public LobbyFrame(String email, String displayName) {
        setTitle("Lobby");
        setSize(480, 320);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(Color.BLACK);
        setLayout(null);

        String who = (displayName == null || displayName.isEmpty()) ? email : displayName;
        JLabel hello = new JLabel("Hello, " + who);
        hello.setForeground(Color.WHITE);
        hello.setBounds(40, 30, 300, 28);
        add(hello);

        JButton btnLogout = new JButton("Logout");
        btnLogout.setBounds(40, 80, 150, 36);
        style(btnLogout);
        add(btnLogout);

        JButton btnStart = new JButton("Start Game");
        btnStart.setBounds(200, 80, 150, 36);
        style(btnStart);
        add(btnStart);
        btnStart.addActionListener(e -> new GameFrame().setVisible(true));

        btnLogout.addActionListener(e -> {
            SessionManager.clear();
            JOptionPane.showMessageDialog(this, "Logged out.");
            dispose();
            new StartScreenFrame().setVisible(true);
        });
    }

    private void style(JButton b) {
        b.setBackground(new Color(30,30,30));
        b.setForeground(new Color(220,220,220));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(new Color(70,70,70)));
    }
}

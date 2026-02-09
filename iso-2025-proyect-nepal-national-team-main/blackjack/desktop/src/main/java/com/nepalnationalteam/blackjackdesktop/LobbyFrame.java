package com.nepalnationalteam.blackjackdesktop;

import javax.swing.*;
import java.awt.*;

/**
 * Lobby depois de login:
 *  - Carrega/Cria utilizador na Users API (carteira)
 *  - Mostra saldo
 *  - Abre o GameFrame (mesa casino) com ApiClient + UserProfile
 */
public class LobbyFrame extends JFrame {

    private final ApiClient apiClient;
    private final UserProfile profile;

    public LobbyFrame(String email, String displayName) {
        setTitle("Lobby");
        setSize(520, 320);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(Color.BLACK);
        setLayout(null);

        // definir quem é
        String who = (displayName == null || displayName.isEmpty()) ? email : displayName;

        JLabel lblWelcome = new JLabel("Logged in as: " + who);
        lblWelcome.setForeground(new Color(220,220,220));
        lblWelcome.setBounds(40, 20, 420, 30);
        add(lblWelcome);

        JLabel lblWallet = new JLabel("Wallet: loading...");
        lblWallet.setForeground(new Color(200, 220, 200));
        lblWallet.setBounds(40, 50, 420, 30);
        add(lblWallet);

        JButton btnLogout = new JButton("Logout");
        btnLogout.setBounds(40, 100, 150, 36);
        style(btnLogout);
        add(btnLogout);

        JButton btnStart = new JButton("Start Game");
        btnStart.setBounds(220, 100, 180, 36);
        style(btnStart);
        add(btnStart);

        // inicializar API + perfil
        ApiClient ac = null;
        UserProfile up = null;
        try {
            ac = new ApiClient("http://localhost:3000");
            SessionManager.Session s = SessionManager.load();
            String userId = (s != null && s.localId != null && !s.localId.isEmpty())
                    ? s.localId
                    : email;
            // carteira inicial 1000
            up = ac.getOrCreateUser(userId, who, 1000);
            lblWallet.setText("Wallet: " + up.getWallet());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Warning: could not contact Users API. Wallet will be local only.\n" + ex.getMessage(),
                    "API error",
                    JOptionPane.WARNING_MESSAGE);
            // fallback local
            up = new UserProfile(email, who, 1000);
            lblWallet.setText("Wallet: " + up.getWallet());
        }

        this.apiClient = ac;
        this.profile = up;

        UserProfile finalProfile = this.profile;
        ApiClient finalApiClient = this.apiClient;

        btnStart.addActionListener(e -> {
            GameFrame gf = new GameFrame(finalApiClient, finalProfile);
            gf.setVisible(true);
        });

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

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
        setMinimumSize(new Dimension(480, 280)); // Permite redimensionar
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(Color.BLACK);

        // Painel principal com um layout flexível para centrar o conteúdo
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.gridwidth = GridBagConstraints.REMAINDER; // Cada componente numa nova linha
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Label de boas-vindas
        String who = (displayName == null || displayName.isEmpty()) ? email : displayName;
        JLabel lblWelcome = new JLabel("Logged in as: " + who, SwingConstants.CENTER);
        lblWelcome.setForeground(new Color(220, 220, 220));
        lblWelcome.setFont(lblWelcome.getFont().deriveFont(16f));
        mainPanel.add(lblWelcome, gbc);

        // Label da carteira
        JLabel lblWallet = new JLabel("Wallet: loading...", SwingConstants.CENTER);
        lblWallet.setForeground(new Color(200, 220, 200));
        lblWallet.setFont(lblWallet.getFont().deriveFont(Font.BOLD, 18f));
        mainPanel.add(lblWallet, gbc);

        // Painel para os botões, para os manter juntos
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setOpaque(false);

        // Botão de Logout
        JButton btnLogout = new JButton("Logout");
        style(btnLogout);
        buttonPanel.add(btnLogout);

        // Botão de Iniciar Jogo
        JButton btnStart = new JButton("Start Game");
        style(btnStart);
        buttonPanel.add(btnStart);

        gbc.insets = new Insets(20, 10, 5, 10); // Adiciona mais espaço acima dos botões
        mainPanel.add(buttonPanel, gbc);

        // Adiciona o painel principal ao frame
        getContentPane().add(mainPanel, BorderLayout.CENTER);

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

        btnStart.addActionListener(e -> {
            new GameFrame(finalProfile).setVisible(true);
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

package com.nepalnationalteam.blackjack;

import javax.swing.*;
import java.awt.*;

public class GameFrame extends JFrame {
    private final BlackjackGame game = new BlackjackGame();

    private final JPanel table = new JPanel() {
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            int w = getWidth(), h = getHeight();
            Color c1 = new Color(9, 122, 52);
            Color c2 = new Color(6, 92, 40);
            for (int y = 0; y < h; y++) {
                float t = y / (float) Math.max(1, h);
                int r = (int) (c1.getRed() * (1 - t) + c2.getRed() * t);
                int gcol = (int) (c1.getGreen() * (1 - t) + c2.getGreen() * t);
                int b = (int) (c1.getBlue() * (1 - t) + c2.getBlue() * t);
                g2.setColor(new Color(r, gcol, b));
                g2.drawLine(0, y, w, y);
            }
            g2.dispose();
        }
    };

    private final JLabel lblDealer = new JLabel("Dealer:", SwingConstants.LEFT);
    private final JLabel lblPlayer = new JLabel("Player:", SwingConstants.LEFT);
    private final JLabel lblTotals = new JLabel("Totals:", SwingConstants.LEFT);
    private final JLabel lblOutcome = new JLabel("", SwingConstants.CENTER);

    private final JButton btnStart = new JButton("Start Round");
    private final JButton btnHit = new JButton("Hit");
    private final JButton btnStand = new JButton("Stand");
    private final JButton btnRestart = new JButton("Restart");

    public GameFrame() {
        setTitle("Blackjack");
        setSize(720, 480);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(Color.BLACK);
        setLayout(null);

        table.setBounds(20, 20, 680, 320);
        table.setLayout(null);
        add(table);

        lblDealer.setForeground(Color.BLACK);
        lblPlayer.setForeground(Color.BLACK);
        lblTotals.setForeground(Color.BLACK);
        lblOutcome.setForeground(Color.BLACK);

        lblDealer.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblPlayer.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTotals.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        lblOutcome.setFont(new Font("Segoe UI", Font.BOLD, 22));

        lblDealer.setBounds(20, 20, 640, 30);
        lblPlayer.setBounds(20, 70, 640, 30);
        lblTotals.setBounds(20, 120, 640, 26);
        lblOutcome.setBounds(20, 165, 640, 36);

        table.add(lblDealer);
        table.add(lblPlayer);
        table.add(lblTotals);
        table.add(lblOutcome);

        style(btnStart); style(btnHit); style(btnStand); style(btnRestart);
        int y = 360;
        btnStart.setBounds(20, y, 160, 38);
        btnHit.setBounds(200, y, 120, 38);
        btnStand.setBounds(330, y, 120, 38);
        btnRestart.setBounds(460, y, 120, 38);
        add(btnStart); add(btnHit); add(btnStand); add(btnRestart);

        btnStart.addActionListener(e -> { game.startRound(); updateView(); });
        btnHit.addActionListener(e -> { game.hit(); updateView(); });
        btnStand.addActionListener(e -> { game.stand(); updateView(); });
        btnRestart.addActionListener(e -> { game.startRound(); updateView(); });

        updateView();
    }

    private void style(JButton b) {
        b.setBackground(new Color(245, 245, 245));
        b.setForeground(Color.BLACK);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70,70,70)),
                BorderFactory.createEmptyBorder(6,14,6,14)
        ));
    }

    private void updateView() {
        lblDealer.setText("Dealer: " + game.dealerDisplay());
        lblPlayer.setText("Player: " + game.handToString(game.player));
        String totals = "Player " + game.player.total();
        if (!game.dealerHoleHidden) totals += "  |  Dealer " + game.dealer.total();
        lblTotals.setText("Totals: " + totals);
        lblOutcome.setText(game.outcome);

        boolean inPlay = game.state == BlackjackGame.State.PLAYER_TURN;
        btnHit.setEnabled(inPlay);
        btnStand.setEnabled(inPlay);
    }
}

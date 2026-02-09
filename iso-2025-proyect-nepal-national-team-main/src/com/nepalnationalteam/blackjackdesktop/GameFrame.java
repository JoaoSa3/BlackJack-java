
package com.nepalnationalteam.blackjackdesktop;

import javax.swing.*;
import java.awt.*;

public class GameFrame extends JFrame {
    private final BlackjackGame game = new BlackjackGame();

    private final JLabel lblDealer = new JLabel("Dealer: ");
    private final JLabel lblPlayer = new JLabel("Player: ");
    private final JLabel lblTotals = new JLabel("Totals: ");
    private final JLabel lblOutcome = new JLabel("");

    private final JButton btnStart = new JButton("Start Round");
    private final JButton btnHit = new JButton("Hit");
    private final JButton btnStand = new JButton("Stand");
    private final JButton btnRestart = new JButton("Restart");

    public GameFrame() {
        setTitle("Blackjack");
        setSize(640, 420);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(Color.BLACK);
        setLayout(null);

        lblDealer.setForeground(Color.WHITE);
        lblPlayer.setForeground(Color.WHITE);
        lblTotals.setForeground(Color.LIGHT_GRAY);
        lblOutcome.setForeground(new Color(255, 215, 0));

        lblDealer.setBounds(40, 30, 560, 28);
        lblPlayer.setBounds(40, 70, 560, 28);
        lblTotals.setBounds(40, 110, 560, 24);
        lblOutcome.setBounds(40, 145, 560, 28);

        add(lblDealer);
        add(lblPlayer);
        add(lblTotals);
        add(lblOutcome);

        style(btnStart);
        style(btnHit);
        style(btnStand);
        style(btnRestart);

        btnStart.setBounds(40, 200, 150, 36);
        btnHit.setBounds(210, 200, 120, 36);
        btnStand.setBounds(340, 200, 120, 36);
        btnRestart.setBounds(470, 200, 120, 36);

        add(btnStart);
        add(btnHit);
        add(btnStand);
        add(btnRestart);

        btnStart.addActionListener(e -> {
            game.startRound();
            updateView();
        });
        btnHit.addActionListener(e -> {
            game.hit();
            updateView();
        });
        btnStand.addActionListener(e -> {
            game.stand();
            updateView();
        });
        btnRestart.addActionListener(e -> {
            game.startRound();
            updateView();
        });

        updateView();
    }

    private void style(JButton b) {
        b.setBackground(new Color(30,30,30));
        b.setForeground(new Color(220,220,220));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(new Color(70,70,70)));
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

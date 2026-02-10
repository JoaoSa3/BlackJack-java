
package com.nepalnationalteam.blackjackdesktop;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Mesa principal de Blackjack em modo casino:
 * - Fundo verde com textura (table_green.png no diretório do projeto)
 * - Dealer em cima
 * - 3 bots no meio
 * - Jogador em baixo
 * - Saldo inicial por conta vindo do Firebase (users/{localId}/balance)
 * - Apostas configuráveis por spinner
 * - Resultado de cada ronda mostrado em cada lugar
 * - Integração com Firebase:
 *      - login / sessão como antes (SessionManager + FirebaseService)
 *      - log de cada ronda em gameLogs/{localId}/{timestamp}
 */
public class GameFrame extends JFrame {

    private enum State { WAITING_BET, PLAYER_TURN, DEALER_TURN, ROUND_OVER }

    private Deck deck;
    private Hand dealer;
    private Hand[] bots;
    private Hand player;

    private State state = State.WAITING_BET;

    private int balance = 1000;
    private int currentBet = 50;

    private final FirebaseService firebase = new FirebaseService();
    private final SessionManager.Session session = SessionManager.load();

    private ApiClient apiClient;
    private UserProfile profile;

    // UI
    private PlayerPanel dealerPanel;
    private PlayerPanel[] botPanels;
    private PlayerPanel playerPanel;

    private JLabel statusLabel;
    private JLabel balanceLabel;
    private JSpinner betSpinner;
    private JButton btnNewRound;
    private JButton btnHit;
    private JButton btnStand;

    private BufferedImage tableImage;

    public GameFrame() {
        setTitle("Blackjack – Nepal National Team Casino");
        setSize(1280, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        loadTableImage();
        initGameState();
        buildUi();
        loadBalanceFromFirebase();
    }

    private void loadTableImage() {
        try {
            File f = new File("table_green.png");
            if (f.exists()) {
                tableImage = ImageIO.read(f);
            }
        } catch (IOException e) {
            tableImage = null;
        }
    }


    public GameFrame(ApiClient apiClient, UserProfile profile) {
        this();
        this.apiClient = apiClient;
        this.profile = profile;
    }

    private void initGameState() {
        deck = new Deck();
        dealer = new Hand();
        bots = new Hand[3];
        for (int i = 0; i < bots.length; i++) {
            bots[i] = new Hand();
        }
        player = new Hand();
    }

    private void loadBalanceFromFirebase() {
        int initial = 1000;
        if (session != null) {
            try {
                initial = firebase.fetchOrInitBalance(session);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        balance = initial;
        if (balanceLabel != null) {
            balanceLabel.setText("Saldo: " + balance);
        }
    }

    private void buildUi() {
        // Mesa de jogo com fundo verde/textura
        JPanel tablePanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                int w = getWidth();
                int h = getHeight();
                if (tableImage != null) {
                    g2.drawImage(tableImage, 0, 0, w, h, null);
                } else {
                    Color c1 = new Color(0, 90, 0);
                    Color c2 = new Color(0, 50, 0);
                    g2.setPaint(new GradientPaint(0, 0, c1, 0, h, c2));
                    g2.fillRect(0, 0, w, h);
                }
            }
        };
        tablePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Painel de topo com título e saldo
        JPanel topInfo = new JPanel(new BorderLayout());
        topInfo.setOpaque(false);

        JLabel title = new JLabel("NEPAL NATIONAL TEAM  |  BLACKJACK CASINO", SwingConstants.CENTER);
        title.setForeground(new Color(255, 215, 0));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        topInfo.add(title, BorderLayout.CENTER);

        JPanel rightInfo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        rightInfo.setOpaque(false);
        String userLabel = (session != null && session.email != null) ? session.email : "Guest";
        JLabel lblUser = new JLabel("Jogador: " + userLabel);
        lblUser.setForeground(Color.WHITE);
        balanceLabel = new JLabel("Saldo: " + balance);
        balanceLabel.setForeground(new Color(144, 238, 144));
        balanceLabel.setFont(balanceLabel.getFont().deriveFont(Font.BOLD, 16f));
        rightInfo.add(lblUser);
        rightInfo.add(balanceLabel);
        topInfo.add(rightInfo, BorderLayout.EAST);

        tablePanel.add(topInfo, BorderLayout.NORTH);

        // Dealer em cima (centrado)
        dealerPanel = new PlayerPanel("DEALER", new Color(250, 230, 200));
        dealerPanel.setOpaque(false);
        JPanel dealerContainer = new JPanel(new BorderLayout());
        dealerContainer.setOpaque(false);
        dealerContainer.add(dealerPanel, BorderLayout.CENTER);

        // 3 bots no centro
        JPanel botsRow = new JPanel(new GridLayout(1, 3, 30, 0));
        botsRow.setOpaque(false);
        botPanels = new PlayerPanel[3];
        String[] botNames = {"BOT 1", "BOT 2", "BOT 3"};
        for (int i = 0; i < 3; i++) {
            botPanels[i] = new PlayerPanel(botNames[i], new Color(200, 220, 255));
            botPanels[i].setOpaque(false);
            botsRow.add(botPanels[i]);
        }
        JPanel botsContainer = new JPanel(new BorderLayout());
        botsContainer.setOpaque(false);
        botsContainer.add(botsRow, BorderLayout.CENTER);

        // Painel superior combinado (dealer + bots)
        JPanel upperTable = new JPanel(new BorderLayout());
        upperTable.setOpaque(false);
        upperTable.add(dealerContainer, BorderLayout.NORTH);
        upperTable.add(botsContainer, BorderLayout.CENTER);

        // Jogador em baixo
        playerPanel = new PlayerPanel("VOCÊ", new Color(210, 255, 210));
        playerPanel.setOpaque(false);

        // Painel de controlo (estado, aposta, botões)
        JPanel controls = new JPanel(new BorderLayout());
        controls.setOpaque(false);

        statusLabel = new JLabel("Escolha a aposta e clique em \"Novo Jogo\".", SwingConstants.CENTER);
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 16f));
        controls.add(statusLabel, BorderLayout.CENTER);

        JPanel bottomControls = new JPanel(new BorderLayout());
        bottomControls.setOpaque(false);

        // Painel de saldo / aposta à esquerda
        JPanel betPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        betPanel.setOpaque(false);
        JLabel betLabel = new JLabel("Aposta:");
        betLabel.setForeground(Color.WHITE);
        betSpinner = new JSpinner(new SpinnerNumberModel(50, 10, 10000, 10));
        styleSpinner(betSpinner);
        betPanel.add(betLabel);
        betPanel.add(betSpinner);

        bottomControls.add(betPanel, BorderLayout.WEST);

        // Botões à direita
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        buttonsPanel.setOpaque(false);
        btnNewRound = createPrimaryButton("Novo Jogo");
        btnHit = createSecondaryButton("Hit");
        btnStand = createSecondaryButton("Stand");
        buttonsPanel.add(btnNewRound);
        buttonsPanel.add(btnHit);
        buttonsPanel.add(btnStand);

        bottomControls.add(buttonsPanel, BorderLayout.EAST);

        controls.add(bottomControls, BorderLayout.SOUTH);

        // Painel inferior: jogador + controlos
        JPanel lowerTable = new JPanel(new BorderLayout());
        lowerTable.setOpaque(false);
        lowerTable.add(playerPanel, BorderLayout.CENTER);
        lowerTable.add(controls, BorderLayout.SOUTH);

        // Split principal: parte de cima (dealer+bots) e parte de baixo (player+controlos)
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, upperTable, lowerTable);
        split.setOpaque(false);
        split.setDividerSize(6);
        split.setResizeWeight(0.55);
        split.setBorder(null);

        tablePanel.add(split, BorderLayout.CENTER);

        setContentPane(tablePanel);

        // Estado inicial dos botões
        btnHit.setEnabled(false);
        btnStand.setEnabled(false);

        // Listeners
        btnNewRound.addActionListener(this::onNewRound);
        btnHit.addActionListener(this::onHit);
        btnStand.addActionListener(this::onStand);
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setOpaque(false);
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor) editor).getTextField().setBackground(new Color(20, 20, 20));
            ((JSpinner.DefaultEditor) editor).getTextField().setForeground(Color.WHITE);
            ((JSpinner.DefaultEditor) editor).getTextField().setCaretColor(Color.WHITE);
        }
    }

    private JButton createPrimaryButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBackground(new Color(80, 0, 120));
        b.setForeground(Color.WHITE);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 14f));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 215, 0), 1),
                BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));
        return b;
    }

    private JButton createSecondaryButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBackground(new Color(30, 30, 30));
        b.setForeground(Color.WHITE);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 13f));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(120, 120, 120), 1),
                BorderFactory.createEmptyBorder(6, 18, 6, 18)
        ));
        return b;
    }

    // ==========================
    //      LÓGICA DO JOGO
    // ==========================

    private void onNewRound(ActionEvent e) {
        int bet = (Integer) betSpinner.getValue();
        if (bet <= 0) {
            statusLabel.setText("Aposta inválida.");
            return;
        }
        if (bet > balance) {
            statusLabel.setText("Saldo insuficiente para essa aposta.");
            return;
        }
        currentBet = bet;

        // Reset estado
        deck = new Deck();
        dealer.clear();
        for (Hand h : bots) h.clear();
        player.clear();
        state = State.PLAYER_TURN;

        // Limpar UI
        dealerPanel.clear();
        for (PlayerPanel p : botPanels) p.clear();
        playerPanel.clear();

        // Dar 2 cartas a cada bot e ao jogador, 2 cartas ao dealer
        for (int i = 0; i < 2; i++) {
            for (Hand h : bots) h.add(deck.draw());
            player.add(deck.draw());
            dealer.add(deck.draw());
        }

        // Bots jogam automaticamente (hit até 16)
        for (Hand bot : bots) {
            while (bot.total() < 17) {
                bot.add(deck.draw());
            }
        }

        updateAllHands(false);
        statusLabel.setText("A sua vez: escolha HIT ou STAND.");
        btnHit.setEnabled(true);
        btnStand.setEnabled(true);
    }

    private void onHit(ActionEvent e) {
        if (state != State.PLAYER_TURN) return;
        player.add(deck.draw());
        if (player.isBust()) {
            state = State.ROUND_OVER;
            statusLabel.setText("Rebentou! Dealer a jogar...");
            btnHit.setEnabled(false);
            btnStand.setEnabled(false);
            dealerTurnAndFinish();
        } else {
            updateAllHands(false);
            statusLabel.setText("HIT feito. Pode HIT novamente ou STAND.");
        }
    }

    private void onStand(ActionEvent e) {
        if (state != State.PLAYER_TURN) return;
        btnHit.setEnabled(false);
        btnStand.setEnabled(false);
        statusLabel.setText("STAND escolhido. Dealer a jogar...");
        dealerTurnAndFinish();
    }

    private void dealerTurnAndFinish() {
        state = State.DEALER_TURN;
        while (dealer.total() < 17) {
            dealer.add(deck.draw());
        }
        state = State.ROUND_OVER;
        settleBetsAndShowOutcome();
    }

    private void settleBetsAndShowOutcome() {
        updateAllHands(true);

        String playerResult = computeResult(player);
        int oldBalance = balance;

        if (playerResult.startsWith("WIN")) {
            balance += currentBet;
        } else if (playerResult.startsWith("LOSE") || playerResult.contains("BUST")) {
            balance -= currentBet;
            if (balance < 0) balance = 0;
        }
        balanceLabel.setText("Saldo: " + balance);

        statusLabel.setText("Ronda terminada: " + playerResult + ". Clique em \"Novo Jogo\".");

        // Atualizar saldo no Firebase e registar ronda
        try {
            if (session != null) {
                firebase.updateBalance(session, balance);
                firebase.logGame(session, oldBalance, balance, currentBet, playerResult);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void updateAllHands(boolean showResults) {
        // Dealer
        String dealerText = showResults ? "Dealer (" + dealer.total() + ")" : "Dealer";
        dealerPanel.showHand(dealer, dealerText, showResults ? null : null);

        // Bots
        for (int i = 0; i < bots.length; i++) {
            String resultText = null;
            if (showResults) {
                resultText = computeResult(bots[i]);
            }
            botPanels[i].showHand(bots[i], "BOT " + (i + 1), resultText);
        }

        // Jogador
        String playerResult = null;
        if (showResults) {
            playerResult = computeResult(player);
        }
        playerPanel.showHand(player, "VOCÊ (" + player.total() + ")", playerResult);
    }

    private String computeResult(Hand hand) {
        if (hand.isBust()) {
            return "BUST (LOSE)";
        }
        if (dealer.isBust()) {
            return "WIN";
        }
        int hv = hand.total();
        int dv = dealer.total();
        if (hv > dv) return "WIN";
        if (hv < dv) return "LOSE";
        return "PUSH";
    }

    // ==========================
    //       PAINEL JOGADOR
    // ==========================

    private static class PlayerPanel extends JPanel {
        private final JLabel nameLabel;
        private final JLabel totalLabel;
        private final JLabel resultLabel;
        private final JPanel cardsPanel;

        public PlayerPanel(String name, Color nameColor) {
            super(new BorderLayout());
            setOpaque(false);

            nameLabel = new JLabel(name, SwingConstants.CENTER);
            nameLabel.setForeground(nameColor);
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 18f));

            totalLabel = new JLabel("Total: 0", SwingConstants.CENTER);
            totalLabel.setForeground(Color.WHITE);

            resultLabel = new JLabel(" ", SwingConstants.CENTER);
            resultLabel.setForeground(Color.WHITE);
            resultLabel.setFont(resultLabel.getFont().deriveFont(Font.BOLD, 16f));

            cardsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
            cardsPanel.setOpaque(false);

            JPanel top = new JPanel(new GridLayout(1, 2));
            top.setOpaque(false);
            top.add(nameLabel);
            top.add(totalLabel);

            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0, 0, 0, 120), 1),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));

            add(top, BorderLayout.NORTH);
            add(cardsPanel, BorderLayout.CENTER);
            add(resultLabel, BorderLayout.SOUTH);
        }

        public void clear() {
            cardsPanel.removeAll();
            totalLabel.setText("Total: 0");
            resultLabel.setText(" ");
            resultLabel.setForeground(Color.WHITE);
            revalidate();
            repaint();
        }

        public void showHand(Hand hand, String nameOverride, String resultText) {
            if (nameOverride != null) {
                nameLabel.setText(nameOverride);
            }
            cardsPanel.removeAll();
            java.util.List<Card> cards = hand.getCards();
            for (Card c : cards) {
                cardsPanel.add(new CardComponent(c));
            }
            totalLabel.setText("Total: " + hand.total());
            if (resultText != null && !resultText.isEmpty()) {
                resultLabel.setText(resultText);
                if (resultText.startsWith("WIN")) {
                    resultLabel.setForeground(new Color(0, 220, 0));
                } else if (resultText.startsWith("LOSE") || resultText.contains("BUST")) {
                    resultLabel.setForeground(new Color(220, 0, 0));
                } else {
                    resultLabel.setForeground(Color.YELLOW);
                }
            } else {
                resultLabel.setText(" ");
                resultLabel.setForeground(Color.WHITE);
            }
            revalidate();
            repaint();
        }
    }

    // ==========================
    //      COMPONENTE CARTA
    // ==========================

    private static class CardComponent extends JComponent {
        private final Card card;

        public CardComponent(Card card) {
            this.card = card;
            setPreferredSize(new Dimension(60, 90));
            setMinimumSize(new Dimension(60, 90));
            setMaximumSize(new Dimension(60, 90));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // fundo da carta
            g2.setColor(new Color(250, 250, 250));
            g2.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);

            // borda
            g2.setColor(new Color(60, 60, 60));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);

            boolean red = (card.suit == Card.Suit.HEARTS || card.suit == Card.Suit.DIAMONDS);
            g2.setColor(red ? new Color(200, 0, 0) : Color.BLACK);

            // rank no canto
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
            g2.drawString(card.rank, 8, 18);

            // símbolo grande do naipe
            String suitSymbol;
            switch (card.suit) {
                case CLUBS: suitSymbol = "♣"; break;
                case DIAMONDS: suitSymbol = "♦"; break;
                case HEARTS: suitSymbol = "♥"; break;
                default: suitSymbol = "♠"; break;
            }
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 26f));
            FontMetrics fm = g2.getFontMetrics();
            int sw = fm.stringWidth(suitSymbol);
            int sh = fm.getAscent();
            int cx = (w - sw) / 2;
            int cy = (h + sh) / 2;
            g2.drawString(suitSymbol, cx, cy);

            g2.dispose();
        }
    }
}

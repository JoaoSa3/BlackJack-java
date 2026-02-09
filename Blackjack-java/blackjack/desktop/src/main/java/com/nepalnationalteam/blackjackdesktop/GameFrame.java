
package com.nepalnationalteam.blackjackdesktop;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    private List<Hand> playerHands;
    private List<Integer> playerBets;
    private int playerActiveHandIndex;

    private State state = State.WAITING_BET;
    private int balance;
    private int currentBet = 50;

    private final FirebaseService firebase = new FirebaseService();
    private final SessionManager.Session session = SessionManager.load();

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
    private JButton btnSplit;
    private JButton btnDoubleDown;

    private BufferedImage tableImage;

    public GameFrame(UserProfile profile) {
        setTitle("Blackjack Casino - Project by João");
        setSize(1280, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Inicializa o saldo a partir do perfil recebido do Lobby
        this.balance = (profile != null) ? profile.getWallet() : 1000;

        loadTableImage();
        initGameState();
        buildUi();
    }

    private void initGameState() {
        deck = new Deck();
        dealer = new Hand();
        bots = new Hand[3];
        for (int i = 0; i < bots.length; i++) {
            bots[i] = new Hand();
        }
        playerHands = new ArrayList<>();
        playerBets = new ArrayList<>();
        playerHands.add(new Hand());
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

        JLabel title = new JLabel("BLACKJACK CASINO  |  JAVA & NODE.JS PROJECT", SwingConstants.CENTER);
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
        btnSplit = createSecondaryButton("Split");
        btnStand = createSecondaryButton("Stand");
        buttonsPanel.add(btnNewRound);
        buttonsPanel.add(btnHit);
        buttonsPanel.add(btnSplit);
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
        btnDoubleDown.setEnabled(false);
        btnSplit.setEnabled(false);

        // Listeners
        btnNewRound.addActionListener(this::onNewRound);
        btnHit.addActionListener(this::onHit);
        btnDoubleDown.addActionListener(this::onDoubleDown);
        btnSplit.addActionListener(this::onSplit);
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
        if (bet > balance) {
            statusLabel.setText("Saldo insuficiente para essa aposta.");
            return;
        }
        currentBet = bet;

        // Subtrai a aposta inicial do saldo
        balance -= currentBet;
        balanceLabel.setText("Saldo: " + balance);

        // Reset estado
        deck = new Deck();
        state = State.PLAYER_TURN;
        dealer.clear();
        for (Hand h : bots) h.clear();
        playerHands.clear();
        playerHands.add(new Hand());
        playerBets.clear();
        playerBets.add(currentBet);
        playerActiveHandIndex = 0;

        // Limpar UI
        dealerPanel.clear();
        for (PlayerPanel p : botPanels) p.clear(); // TODO: check if this is right
        playerPanel.clear();

        // Dar 2 cartas a cada bot e ao jogador, 2 cartas ao dealer
        for (int i = 0; i < 2; i++) {
            for (Hand h : bots) h.add(deck.draw());
            playerHands.get(0).add(deck.draw());
            dealer.add(deck.draw());
        }

        // Bots jogam automaticamente (hit até 16)
        for (Hand bot : bots) {
            while (bot.total() < 17) {
                bot.add(deck.draw());
            }
        }

        updateButtonStates();
        updateAllHands(false, null);

        // Auto-stand no caso de Blackjack do jogador
        if (isBlackjack(playerHands.get(0))) {
            onStand(null);
        }
    }

    private boolean isBlackjack(Hand hand) {
        return hand.getCards().size() == 2 && hand.total() == 21;
    }

    private void onHit(ActionEvent e) {
        if (state != State.PLAYER_TURN || playerHands.isEmpty()) return;

        Hand activeHand = playerHands.get(playerActiveHandIndex);
        activeHand.add(deck.draw());

        updateButtonStates();

        if (activeHand.isBust()) {
            statusLabel.setText("Mão " + (playerActiveHandIndex + 1) + " rebentou!");
            advancePlayerHand();
        } else {
            statusLabel.setText("HIT feito. Pode HIT novamente ou STAND.");
            updateAllHands(false, null);
        }
    }

    private void onDoubleDown(ActionEvent e) {
        if (state != State.PLAYER_TURN) return;

        Hand activeHand = playerHands.get(playerActiveHandIndex);
        int handBet = playerBets.get(playerActiveHandIndex);

        if (activeHand.getCards().size() != 2 || balance < handBet) {
            return;
        }

        // Atualiza saldo e aposta para esta mão
        balance -= handBet;
        balanceLabel.setText("Saldo: " + balance);
        playerBets.set(playerActiveHandIndex, handBet * 2);

        // Dá a última carta
        activeHand.add(deck.draw());
        updateAllHands(false, null);

        // Termina o turno para esta mão
        statusLabel.setText("Mão " + (playerActiveHandIndex + 1) + " dobrada. Total: " + activeHand.total());
        advancePlayerHand();
    }

    private void onSplit(ActionEvent e) {
        if (state != State.PLAYER_TURN) return;

        Hand handToSplit = playerHands.get(playerActiveHandIndex);
        if (!isSplittable(handToSplit) || balance < currentBet) {
            return;
        }

        // Desativar mais splits e atualizar saldo
        balance -= currentBet;
        balanceLabel.setText("Saldo: " + balance);
        playerBets.add(playerActiveHandIndex + 1, currentBet);

        // Criar duas novas mãos a partir da original
        Hand hand1 = new Hand();
        hand1.add(handToSplit.getCards().get(0));
        Hand hand2 = new Hand();
        hand2.add(handToSplit.getCards().get(1));

        // Substituir a mão original pelas duas novas
        playerHands.set(playerActiveHandIndex, hand1);
        playerHands.add(playerActiveHandIndex + 1, hand2);

        // Dar uma nova carta a cada mão
        hand1.add(deck.draw());
        hand2.add(deck.draw());

        // Regra especial para dividir Ases: o jogador só recebe uma carta e tem de parar.
        if (handToSplit.getCards().get(0).getRankValue() == 11) {
            // O turno para ambas as mãos termina imediatamente.
            playerActiveHandIndex = playerHands.size(); // Coloca o índice para além do fim
            onStand(null); // Aciona o turno do dealer
        } else {
            // Para outros splits, continua a jogar a primeira mão
            updateButtonStates();
            updateAllHands(false, null);
            if (hand1.total() == 21) { // Se a primeira mão for 21, passa à próxima
                advancePlayerHand();
            }
        }
    }

    private void onStand(ActionEvent e) {
        if (state != State.PLAYER_TURN) return;
        advancePlayerHand();
    }

    /**
     * Avança para a próxima mão do jogador ou, se não houver mais, para o turno do dealer.
     */
    private void advancePlayerHand() {
        playerActiveHandIndex++;
        if (playerActiveHandIndex >= playerHands.size()) {
            // Todas as mãos do jogador foram jogadas
            updateButtonStates(); // Desativa todos os botões
            statusLabel.setText("A sua vez terminou. Dealer a jogar...");
            dealerTurnAndFinish();
        } else {
            // Passa para a próxima mão
            statusLabel.setText("A jogar a Mão " + (playerActiveHandIndex + 1));
            updateButtonStates();
            updateAllHands(false, null);
            // Verifica se a nova mão ativa é um Blackjack
            if (isBlackjack(playerHands.get(playerActiveHandIndex))) {
                advancePlayerHand();
            }
        }
    }

    private void updateButtonStates() {
        if (state != State.PLAYER_TURN || playerActiveHandIndex >= playerHands.size()) {
            btnHit.setEnabled(false);
            btnStand.setEnabled(false);
            btnSplit.setEnabled(false);
            btnDoubleDown.setEnabled(false);
            return;
        }

        Hand activeHand = playerHands.get(playerActiveHandIndex);
        boolean canAffordAction = balance >= currentBet;

        btnHit.setEnabled(!activeHand.isBust());
        btnStand.setEnabled(true);

        if (activeHand.getCards().size() == 2) {
            btnSplit.setEnabled(isSplittable(activeHand) && canAffordAction);
            btnDoubleDown.setEnabled(canAffordAction);
        } else {
            btnSplit.setEnabled(false);
            btnDoubleDown.setEnabled(false);
        }
    }

    private void dealerTurnAndFinish() {
        // Revela a carta escondida do dealer
        dealerPanel.showHand(dealer, "DEALER (" + dealer.total() + ")", null, false);

        // Usa um Timer para uma sequência de compra não bloqueante e mais visual
        state = State.DEALER_TURN;
        Timer dealerTimer = new Timer(800, null); // Atraso de 800ms entre ações
        dealerTimer.addActionListener(e -> {
            if (dealer.total() < 17) {
                dealer.add(deck.draw());
                // Atualiza a mão do dealer a cada nova carta
                dealerPanel.showHand(dealer, "DEALER (" + dealer.total() + ")", null, false);
            } else {
                // Para o timer e termina a ronda
                ((Timer)e.getSource()).stop();
                state = State.ROUND_OVER;
                settleBetsAndShowOutcome();
            }
        });
        dealerTimer.setRepeats(true);
        dealerTimer.start();
    }

    private void settleBetsAndShowOutcome() {

        int oldBalance = balance;
        int totalBet = playerBets.stream().mapToInt(Integer::intValue).sum();

        List<String> playerResults = new ArrayList<>();
        int totalWinnings = 0;

        for (int i = 0; i < playerHands.size(); i++) {
            Hand hand = playerHands.get(i);
            int wager = playerBets.get(i);

            String result = computeResult(hand);
            playerResults.add(result);
            if (result.startsWith("BLACKJACK")) {
                // O jogador recebe a aposta de volta + 1.5x a aposta como prémio
                totalWinnings += wager + (int)(wager * 1.5);
            } else if (result.startsWith("WIN")) {
                // O jogador recebe a aposta de volta + 1x a aposta como prémio
                totalWinnings += wager * 2;
            } else if (result.startsWith("PUSH")) {
                // O jogador recebe a aposta de volta
                totalWinnings += wager;
            }
            // Numa derrota, os ganhos são 0.
        }

        // As apostas já foram subtraídas, por isso apenas adicionamos os ganhos.
        balance += totalWinnings;
        if (balance < 0) balance = 0;
        balanceLabel.setText("Saldo: " + balance);

        String summaryResult = String.join(" / ", playerResults);

        // Mostra o resultado final de todas as mãos
        updateAllHands(true, playerResults);

        statusLabel.setText("Ronda terminada: " + summaryResult + ". Clique em \"Novo Jogo\".");

        // Atualizar saldo no Firebase e registar ronda
        if (session != null) {
            // Executar a atualização de dados em background para não bloquear a UI
            SwingWorker<Void, Void> dataSyncWorker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    firebase.updateBalance(session, balance);
                    firebase.logGame(session, oldBalance, balance, totalBet, summaryResult);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get(); // Chamar get() para detetar exceções que ocorreram em doInBackground
                        // Se correu bem, garante que a cor do saldo está normal
                        balanceLabel.setForeground(new Color(144, 238, 144));
                        balanceLabel.setToolTipText("Saldo atual. Sincronizado com o servidor.");
                    } catch (Exception ex) {
                        System.err.println("Falha ao sincronizar dados da ronda com o Firebase.");
                        ex.printStackTrace();
                        balanceLabel.setForeground(Color.ORANGE);
                        balanceLabel.setToolTipText("Atenção: Ocorreu uma falha ao guardar o saldo no servidor.");
                    }
                }
            };
            dataSyncWorker.execute();
        }
    }

    private void updateAllHands(boolean showResults, List<String> playerResults) {
        // Dealer
        String dealerText = showResults ? "DEALER (" + dealer.total() + ")" : "DEALER";
        dealerPanel.showHand(dealer, dealerText, null, !showResults);

        // Bots
        for (int i = 0; i < bots.length; i++) {
            String resultText = null;
            if (showResults) {
                resultText = computeResult(bots[i]);
            }
            botPanels[i].showHand(bots[i], "BOT " + (i + 1), resultText, false); // Bots never hide cards
        }

        // Jogador
        playerPanel.showHands(playerHands, playerResults, showResults ? -1 : playerActiveHandIndex);
    }

    private String computeResult(Hand hand) {
        boolean playerHasBlackjack = isBlackjack(hand);
        boolean dealerHasBlackjack = isBlackjack(dealer);

        // A verificação de Blackjack tem prioridade
        if (playerHasBlackjack) {
            // Se o jogador tem Blackjack, só não ganha se o dealer também tiver.
            return dealerHasBlackjack ? "PUSH (Blackjack)" : "BLACKJACK! (WIN 3:2)";
        }

        // Se o dealer tem Blackjack e o jogador não, o jogador perde.
        if (dealerHasBlackjack) {
            return "LOSE (Dealer has Blackjack)";
        }
        if (hand.isBust()) {
            return "BUST (LOSE)";
        }
        if (dealer.isBust()) {
            return "WIN (Dealer Bust)";
        }
        if (hand.total() > dealer.total()) return "WIN";
        if (hand.total() < dealer.total()) return "LOSE";
        return "PUSH";
    }
    
    private boolean isSplittable(Hand hand) {
        if (hand.getCards().size() != 2) return false;
        return hand.getCards().get(0).getRankValue() == hand.getCards().get(1).getRankValue();
    }

    // ==========================
    //       PAINEL JOGADOR
    // ==========================

    private static class PlayerPanel extends JPanel {
        private final JLabel nameLabel;
        private final JPanel cardsPanel;

        public PlayerPanel(String name, Color nameColor) {
            super(new BorderLayout());
            setOpaque(false);

            nameLabel = new JLabel(name, SwingConstants.CENTER);
            nameLabel.setForeground(nameColor);
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 18f));

            cardsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 8));
            cardsPanel.setOpaque(false);

            // Adiciona um scroll horizontal para o caso de muitas mãos (splits)
            JScrollPane scrollPane = new JScrollPane(cardsPanel);
            scrollPane.setOpaque(false);
            scrollPane.getViewport().setOpaque(false);
            scrollPane.setBorder(null);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0, 0, 0, 120), 1),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));

            add(nameLabel, BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);
        }

        public void clear() {
            cardsPanel.removeAll();
            revalidate();
            repaint();
        }

        // Método para mostrar uma única mão (usado pelo Dealer e Bots)
        public void showHand(Hand hand, String nameOverride, String resultText, boolean hideHoleCard) {
            List<Hand> hands = new ArrayList<>();
            hands.add(hand);
            List<String> results = new ArrayList<>();
            results.add(resultText);
            showHands(hands, results, -1, hideHoleCard);
        }

        // Método principal para mostrar uma ou mais mãos (usado pelo Jogador)
        public void showHands(List<Hand> hands, List<String> results, int activeHandIndex) {
            showHands(hands, results, activeHandIndex, false);
        }

        private void showHands(List<Hand> hands, List<String> results, int activeHandIndex, boolean hideHoleCard) {
            cardsPanel.removeAll();

            for (int i = 0; i < hands.size(); i++) {
                Hand hand = hands.get(i);
                String resultText = (results != null && i < results.size()) ? results.get(i) : null;

                JPanel handContainer = new JPanel(new BorderLayout(5, 5));
                handContainer.setOpaque(false);

                // Destaque para a mão ativa
                if (i == activeHandIndex) {
                    handContainer.setBorder(BorderFactory.createLineBorder(new Color(255, 215, 0), 2));
                }

                JPanel singleHandCardsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
                singleHandCardsPanel.setOpaque(false);

                for (int j = 0; j < hand.getCards().size(); j++) {
                    if (hideHoleCard && j == 1) {
                        singleHandCardsPanel.add(new CardComponent(true)); // Verso da carta
                    } else {
                        singleHandCardsPanel.add(new CardComponent(hand.getCards().get(j)));
                    }
                }

                JLabel totalLabel = new JLabel("Total: " + hand.total(), SwingConstants.CENTER);
                totalLabel.setForeground(Color.LIGHT_GRAY);
                handContainer.add(totalLabel, BorderLayout.NORTH);
                handContainer.add(singleHandCardsPanel, BorderLayout.CENTER);

                if (resultText != null) {
                    JLabel resultLabel = new JLabel(resultText, SwingConstants.CENTER);
                    resultLabel.setFont(resultLabel.getFont().deriveFont(Font.BOLD));
                    setResultColor(resultLabel, resultText);
                    handContainer.add(resultLabel, BorderLayout.SOUTH);
                }

                cardsPanel.add(handContainer);
            }

            revalidate();
            repaint();
        }

        private void setResultColor(JLabel label, String resultText) {
            if (resultText.startsWith("WIN") || resultText.startsWith("BLACKJACK")) {
                label.setForeground(new Color(0, 220, 0));
            } else if (resultText.startsWith("LOSE") || resultText.contains("BUST")) {
                label.setForeground(new Color(220, 0, 0));
            } else {
                label.setForeground(Color.YELLOW);
            }
        }

        public void showHand(Hand hand, String nameOverride, String resultText) {
            if (nameOverride != null) {
                nameLabel.setText(nameOverride);
            }
            showHand(hand, nameOverride, resultText, false);
        }
    }

    // ==========================
    //      COMPONENTE CARTA
    // ==========================

    private static class CardComponent extends JComponent {
        private final Card card;
        private final boolean isHidden;

        public CardComponent(Card card) {
            this.card = card;
            this.isHidden = false;
            setPreferredSize(new Dimension(60, 90));
            setMinimumSize(new Dimension(60, 90));
            setMaximumSize(new Dimension(60, 90));
            setOpaque(false);
        }

        public CardComponent(boolean isHidden) {
            this.card = null;
            this.isHidden = isHidden;
            setPreferredSize(new Dimension(60, 90));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (isHidden || card == null) {
                // Desenha o verso da carta
                g2.setColor(new Color(180, 0, 0));
                g2.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);
                g2.setColor(new Color(100, 0, 0));
                g2.drawRoundRect(2, 2, w - 5, h - 5, 8, 8);
                g2.dispose();
                return;
            }

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

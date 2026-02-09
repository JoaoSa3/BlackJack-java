
package com.nepalnationalteam.blackjackdesktop;

public class BlackjackGame {
    public enum State { READY, PLAYER_TURN, DEALER_TURN, ROUND_OVER }

    private final Deck deck = new Deck();
    public final Hand player = new Hand();
    public final Hand dealer = new Hand();
    public State state = State.READY;
    public boolean dealerHoleHidden = true;
    public String outcome = "";

    public void startRound() {
        player.clear();
        dealer.clear();
        dealerHoleHidden = true;
        outcome = "";
        state = State.PLAYER_TURN;
        // deal
        player.add(deck.draw());
        dealer.add(deck.draw());
        player.add(deck.draw());
        dealer.add(deck.draw());
        // natural blackjack check
        if (player.isBlackjack() || dealer.isBlackjack()) {
            stand(); // resolve immediately
        }
    }

    public void hit() {
        if (state != State.PLAYER_TURN) return;
        player.add(deck.draw());
        if (player.isBust()) {
            dealerHoleHidden = false;
            state = State.ROUND_OVER;
            outcome = "Dealer wins";
        }
    }

    public void stand() {
        if (state == State.ROUND_OVER) return;
        dealerHoleHidden = false;
        // dealer plays
        state = State.DEALER_TURN;
        while (dealer.total() < 17) {
            dealer.add(deck.draw());
        }
        // resolve
        int pt = player.total();
        int dt = dealer.total();
        if (player.isBlackjack() && !dealer.isBlackjack()) outcome = "Player blackjack";
        else if (!player.isBlackjack() && dealer.isBlackjack()) outcome = "Dealer blackjack";
        else if (pt > 21) outcome = "Dealer wins";
        else if (dt > 21) outcome = "Player wins";
        else if (pt > dt) outcome = "Player wins";
        else if (pt < dt) outcome = "Dealer wins";
        else outcome = "Push";
        state = State.ROUND_OVER;
    }

    public String dealerDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dealer.getCards().size(); i++) {
            Card c = dealer.getCards().get(i);
            if (i == 1 && dealerHoleHidden) sb.append("?? ");
            else sb.append(c.toString()).append(" ");
        }
        return sb.toString().trim();
    }

    public String handToString(Hand h) {
        StringBuilder sb = new StringBuilder();
        for (Card c : h.getCards()) sb.append(c.toString()).append(" ");
        return sb.toString().trim();
    }
}

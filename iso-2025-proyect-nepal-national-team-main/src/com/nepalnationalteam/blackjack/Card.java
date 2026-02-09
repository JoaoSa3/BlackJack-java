package com.nepalnationalteam.blackjack;

public class Card {
    public enum Suit { CLUBS, DIAMONDS, HEARTS, SPADES }
    public final Suit suit;
    public final String rank;

    public Card(Suit suit, String rank) { this.suit = suit; this.rank = rank; }

    public int value() {
        switch (rank) {
            case "A": return 11;
            case "K": case "Q": case "J": return 10;
            default: return Integer.parseInt(rank);
        }
    }
    public String toString() {
        String s = switch (suit) { case CLUBS -> "♣"; case DIAMONDS -> "♦"; case HEARTS -> "♥"; default -> "♠"; };
        return rank + s;
    }
}

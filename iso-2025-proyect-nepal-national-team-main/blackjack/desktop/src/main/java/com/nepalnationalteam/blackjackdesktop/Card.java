
package com.nepalnationalteam.blackjackdesktop;

public class Card {
    public enum Suit { CLUBS, DIAMONDS, HEARTS, SPADES }
    public final Suit suit;
    public final String rank; // "A", "2"... "10", "J", "Q", "K"

    public Card(Suit suit, String rank) {
        this.suit = suit;
        this.rank = rank;
    }

    public int value() {
        switch (rank) {
            case "A": return 11;
            case "K":
            case "Q":
            case "J":
                return 10;
            default:
                return Integer.parseInt(rank);
        }
    }

    @Override
    public String toString() {
        String s;
        switch (suit) {
            case CLUBS: s = "♣"; break;
            case DIAMONDS: s = "♦"; break;
            case HEARTS: s = "♥"; break;
            default: s = "♠"; break;
        }
        return rank + s;
    }
}

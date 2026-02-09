
package com.nepalnationalteam.blackjackdesktop;

import java.util.*;

public class Deck {
    private final List<Card> cards = new ArrayList<>();
    private final Random rnd = new Random();

    public Deck() {
        String[] ranks = {"A","2","3","4","5","6","7","8","9","10","J","Q","K"};
        for (Card.Suit s : Card.Suit.values()) {
            for (String r : ranks) cards.add(new Card(s, r));
        }
        shuffle();
    }

    public void shuffle() {
        Collections.shuffle(cards, rnd);
    }

    public Card draw() {
        if (cards.isEmpty()) {
            // auto-reshuffle a fresh deck
            Deck fresh = new Deck();
            cards.addAll(fresh.cards);
            shuffle();
        }
        return cards.remove(cards.size() - 1);
    }
}

package com.nepalnationalteam.blackjack;

import java.util.*;

public class Hand {
    private final List<Card> cards = new ArrayList<>();
    public void clear() { cards.clear(); }
    public void add(Card c) { cards.add(c); }
    public List<Card> getCards() { return Collections.unmodifiableList(cards); }

    public int total() {
        int sum = 0, aces = 0;
        for (Card c : cards) {
            int v = c.value();
            if ("A".equals(c.rank)) aces++;
            sum += v;
        }
        while (sum > 21 && aces > 0) { sum -= 10; aces--; }
        return sum;
    }
    public boolean isBlackjack() { return cards.size() == 2 && total() == 21; }
    public boolean isBust() { return total() > 21; }
}

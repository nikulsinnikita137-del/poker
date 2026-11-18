package ru.vsu.poker.card;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import org.junit.jupiter.api.Test;

class CardAndDeckTest {

    private static List<Card> drawAll(Deck d) {
        List<Card> res = new ArrayList<>();
        while (d.size() > 0) {
            res.add(d.draw());
        }
        return res;
    }

    @Test
    void parseAndToStringRoundTrip() {
        Card c = Card.parse("Td");
        assertEquals(Rank.TEN, c.rank());
        assertEquals(Suit.DIAMONDS, c.suit());
        assertEquals("Td", c.toString());
    }

    @Test
    void parseRejectsGarbage() {
        assertThrows(IllegalArgumentException.class, () -> Card.parse("1s"));
        assertThrows(IllegalArgumentException.class, () -> Card.parse("Ax"));
        assertThrows(IllegalArgumentException.class, () -> Card.parse("Ace"));
        assertThrows(IllegalArgumentException.class, () -> Card.parse(null));
    }

    @Test
    void aceIsHigherThanKing() {
        assertTrue(Card.parse("As").compareTo(Card.parse("Kh")) > 0);
    }

    @Test
    void deckHas52UniqueCards() {
        Deck d = new Deck(new Random(1));
        List<Card> all = drawAll(d);
        assertEquals(52, all.size());
        assertEquals(52, new HashSet<>(all).size());
    }

    @Test
    void sameSeedGivesSameOrder() {
        assertEquals(drawAll(new Deck(new Random(42))), drawAll(new Deck(new Random(42))));
    }

    @Test
    void differentSeedsGiveDifferentOrder() {
        assertNotEquals(drawAll(new Deck(new Random(1))), drawAll(new Deck(new Random(2))));
    }

    @Test
    void drawFromEmptyDeckThrows() {
        Deck d = new Deck(List.of(Card.parse("As")));
        d.draw();
        assertThrows(NoSuchElementException.class, d::draw);
    }
}

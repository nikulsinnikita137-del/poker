package ru.vsu.poker.game;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import ru.vsu.poker.card.Card;

class PlayerTest {

    @Test
    void payMovesChipsFromStackToContribution() {
        Player p = new Player(0, "A", 100);
        assertEquals(30, p.pay(30));
        assertEquals(70, p.stack());
        assertEquals(30, p.contributed());
    }

    @Test
    void payMoreThanStackGoesAllIn() {
        Player p = new Player(0, "A", 50);
        p.startHand();
        assertEquals(50, p.pay(80));
        assertEquals(0, p.stack());
        assertTrue(p.isAllIn());
    }

    @Test
    void negativeAmountsAreRejected() {
        Player p = new Player(0, "A", 50);
        assertThrows(IllegalArgumentException.class, () -> p.pay(-1));
        assertThrows(IllegalArgumentException.class, () -> p.win(-1));
        assertThrows(IllegalArgumentException.class, () -> new Player(0, "B", -5));
    }

    @Test
    void thirdHoleCardIsRejected() {
        Player p = new Player(0, "A", 50);
        p.receive(Card.parse("As"));
        p.receive(Card.parse("Kd"));
        assertThrows(IllegalStateException.class, () -> p.receive(Card.parse("2c")));
    }

    @Test
    void foldedPlayerIsNotAllIn() {
        Player p = new Player(0, "A", 10);
        p.startHand();
        p.pay(10);
        p.fold();
        assertFalse(p.isAllIn());
    }

    @Test
    void startHandResetsHandStateAndSkipsBrokePlayers() {
        Player p = new Player(0, "A", 100);
        p.startHand();
        p.receive(Card.parse("As"));
        p.pay(40);
        p.fold();
        p.startHand();
        assertTrue(p.holeCards().isEmpty());
        assertEquals(0, p.contributed());
        assertFalse(p.isFolded());
        assertTrue(p.isInHand());

        Player broke = new Player(1, "B", 0);
        broke.startHand();
        assertFalse(broke.isInHand());
    }
}

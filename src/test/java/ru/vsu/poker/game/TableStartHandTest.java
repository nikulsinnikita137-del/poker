package ru.vsu.poker.game;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import ru.vsu.poker.card.Card;
import ru.vsu.poker.card.Deck;
import ru.vsu.poker.event.GameEvent;

class TableStartHandTest {

    private static final Blinds BLINDS = new Blinds(10, 20);

    private static Table table(int seed, int... stacks) {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < stacks.length; i++) {
            players.add(new Player(i, "P" + i, stacks[i]));
        }
        return new Table(players, BLINDS, new Random(seed));
    }

    @Test
    void everyPlayerGetsTwoDistinctCards() {
        Table t = table(1, 1000, 1000, 1000, 1000);
        t.startHand();
        Set<Card> all = new HashSet<>();
        for (Player p : t.players()) {
            assertEquals(2, p.holeCards().size());
            all.addAll(p.holeCards());
        }
        assertEquals(8, all.size());
    }

    @Test
    void blindsAreTakenFromSmallAndBigBlindPlayers() {
        Table t = table(1, 1000, 1000, 1000, 1000);
        t.startHand();
        // Первая раздача: кнопка на месте 0, малый блайнд — 1, большой — 2
        assertEquals(0, t.button());
        assertEquals(990, t.players().get(1).stack());
        assertEquals(980, t.players().get(2).stack());
        assertEquals(1000, t.players().get(3).stack());
        assertEquals(30, t.pot());
    }

    @Test
    void positionsFollowTheButton() {
        Table t = table(1, 1000, 1000, 1000, 1000);
        t.startHand();
        assertEquals(Position.BUTTON, t.positionOf(t.players().get(0)));
        assertEquals(Position.SMALL_BLIND, t.positionOf(t.players().get(1)));
        assertEquals(Position.BIG_BLIND, t.positionOf(t.players().get(2)));
        assertEquals(Position.UNDER_THE_GUN, t.positionOf(t.players().get(3)));
    }

    @Test
    void headsUpButtonPostsSmallBlind() {
        Table t = table(1, 1000, 1000);
        t.startHand();
        assertEquals(990, t.players().get(0).stack());
        assertEquals(980, t.players().get(1).stack());
        assertEquals(Position.BUTTON_SMALL_BLIND, t.positionOf(t.players().get(0)));
    }

    @Test
    void buttonMovesClockwiseEachHand() {
        Table t = table(1, 1000, 1000, 1000);
        t.startHand();
        assertEquals(0, t.button());
        t.startHand();
        assertEquals(1, t.button());
        t.startHand();
        assertEquals(2, t.button());
        t.startHand();
        assertEquals(0, t.button());
        assertEquals(4, t.handNumber());
    }

    @Test
    void buttonSkipsPlayersWithoutChips() {
        Table t = table(1, 1000, 0, 1000);
        t.startHand();
        t.startHand();
        assertEquals(2, t.button());
        assertFalse(t.players().get(1).isInHand());
        assertThrows(IllegalArgumentException.class, () -> t.positionOf(t.players().get(1)));
    }

    @Test
    void shortStackPostsPartialBlindAndIsAllIn() {
        Table t = table(1, 1000, 1000, 15, 1000);
        List<GameEvent> events = t.startHand();
        Player bb = t.players().get(2);
        assertEquals(0, bb.stack());
        assertEquals(15, bb.contributed());
        assertTrue(bb.isAllIn());
        GameEvent.BlindPosted posted = (GameEvent.BlindPosted) events.get(2);
        assertEquals(GameEvent.BlindKind.BIG, posted.kind());
        assertEquals(15, posted.amount());
    }

    @Test
    void needsTwoPlayersWithChips() {
        Table t = table(1, 1000, 0, 0);
        assertThrows(IllegalStateException.class, t::startHand);
    }

    @Test
    void tableRejectsWrongSeatsAndPlayerCount() {
        assertThrows(IllegalArgumentException.class, () -> new Table(
                List.of(new Player(0, "A", 100), new Player(2, "B", 100)), BLINDS, new Random(1)));
        assertThrows(IllegalArgumentException.class, () -> new Table(
                List.of(new Player(0, "A", 100)), BLINDS, new Random(1)));
    }

    @Test
    void dealingStartsLeftOfButtonOneCardAtATime() {
        Table t = table(7, 1000, 1000, 1000);
        t.startHand();
        // Ожидаемый порядок сдачи считаем по такой же колоде: места 1, 2, 0, затем второй круг
        Deck d = new Deck(new Random(7));
        List<Card> first = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            first.add(d.draw());
        }
        assertEquals(List.of(first.get(0), first.get(3)), t.players().get(1).holeCards());
        assertEquals(List.of(first.get(1), first.get(4)), t.players().get(2).holeCards());
        assertEquals(List.of(first.get(2), first.get(5)), t.players().get(0).holeCards());
    }

    @Test
    void headsUpDealingStartsWithBigBlind() {
        Table t = table(7, 1000, 1000);
        t.startHand();
        Deck d = new Deck(new Random(7));
        List<Card> first = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            first.add(d.draw());
        }
        assertEquals(List.of(first.get(0), first.get(2)), t.players().get(1).holeCards());
        assertEquals(List.of(first.get(1), first.get(3)), t.players().get(0).holeCards());
    }

    @Test
    void sameSeedGivesSameDeal() {
        Table a = table(99, 1000, 1000, 1000);
        Table b = table(99, 1000, 1000, 1000);
        assertEquals(a.startHand(), b.startHand());
    }

    @Test
    void differentSeedsGiveDifferentDeals() {
        assertNotEquals(table(1, 1000, 1000, 1000).startHand(), table(2, 1000, 1000, 1000).startHand());
    }

    @Test
    void eventsComeInOrder() {
        List<GameEvent> events = table(1, 1000, 1000, 1000).startHand();
        assertEquals(1 + 2 + 3, events.size());
        assertTrue(events.get(0) instanceof GameEvent.HandStarted);
        assertTrue(events.get(1) instanceof GameEvent.BlindPosted);
        assertTrue(events.get(2) instanceof GameEvent.BlindPosted);
        assertTrue(events.get(3) instanceof GameEvent.HoleCardsDealt);
        GameEvent.HandStarted started = (GameEvent.HandStarted) events.get(0);
        assertEquals(1, started.handNumber());
        assertEquals(List.of(0, 1, 2), started.seats());
    }

    @Test
    void chipsAreConservedAndSecondHandResetsState() {
        Table t = table(5, 500, 700, 900);
        t.startHand();
        t.players().get(0).win(t.pot()); // банк первой раздачи выдан (вскрытие появится позже)
        t.startHand();
        int total = t.players().stream().mapToInt(Player::stack).sum() + t.pot();
        assertEquals(2100, total);
        assertEquals(30, t.pot(), "вклады прошлой раздачи сброшены");
    }

    @Test
    void mechanicsNeverWriteToConsole() {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer));
        try {
            Table t = table(3, 1000, 1000, 1000, 1000);
            t.startHand();
            t.startHand();
        } finally {
            System.setOut(original);
        }
        assertEquals(0, buffer.size());
    }
}

package ru.vsu.poker.sim;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import ru.vsu.poker.action.Action;
import ru.vsu.poker.bot.Bot;
import ru.vsu.poker.bot.CallingStationBot;
import ru.vsu.poker.bot.RandomBot;
import ru.vsu.poker.event.GameEvent;
import ru.vsu.poker.game.Blinds;
import ru.vsu.poker.game.Player;
import ru.vsu.poker.game.Table;

class GameRunnerTest {

    private static final Blinds BLINDS = new Blinds(10, 20);

    private static Table table(Random random, int... stacks) {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < stacks.length; i++) {
            players.add(new Player(i, "P" + i, stacks[i]));
        }
        return new Table(players, BLINDS, random);
    }

    private static List<Bot> stations(int n) {
        List<Bot> bots = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            bots.add(new CallingStationBot());
        }
        return bots;
    }

    /** Бот, который всегда идёт ва-банк. */
    private static final class AllInBot implements Bot {
        @Override
        public Action decide(Table table, Player player) {
            return new Action.AllIn();
        }
    }

    /** Бот, который ходит заведомо неверно (повышение до 1), пока его не «вразумят». */
    private static final class StubbornBot implements Bot {
        final boolean interactive;
        final int stopAfter;
        int rejections;

        StubbornBot(boolean interactive, int stopAfter) {
            this.interactive = interactive;
            this.stopAfter = stopAfter;
        }

        @Override
        public Action decide(Table table, Player player) {
            if (rejections < stopAfter) {
                return new Action.Raise(1);
            }
            return table.amountToCall(player) == 0 ? new Action.Check() : new Action.Call();
        }

        @Override
        public void rejected(String reason) {
            rejections++;
        }

        @Override
        public boolean isInteractive() {
            return interactive;
        }
    }

    @Test
    void playsOneFullHandAndReportsEvents() {
        Table t = table(new Random(1), 1000, 1000, 1000);
        List<GameEvent> log = new ArrayList<>();
        GameRunner runner = new GameRunner(t, stations(3), log::add);
        assertTrue(runner.playHand());
        assertTrue(t.isHandOver());
        assertTrue(log.get(0) instanceof GameEvent.HandStarted);
        assertTrue(log.get(log.size() - 1) instanceof GameEvent.HandFinished);
        assertEquals(3000, t.players().stream().mapToInt(Player::stack).sum());
    }

    @Test
    void stopsWhenOnlyOnePlayerHasChips() {
        List<Bot> bots = List.of(new AllInBot(), new AllInBot());
        Table t = table(new Random(1), 100, 100);
        GameRunner runner = new GameRunner(t, bots, e -> { });
        int played = runner.playHands(1000);
        assertTrue(played >= 1 && played < 1000);
        assertEquals(200, t.players().stream().mapToInt(Player::stack).sum());
        assertTrue(t.players().stream().anyMatch(p -> p.stack() == 0));
        assertFalse(runner.playHand(), "играть уже не с кем");
    }

    @Test
    void playHandsRespectsTheLimit() {
        Table t = table(new Random(1), 1000, 1000, 1000);
        assertEquals(3, new GameRunner(t, stations(3), e -> { }).playHands(3));
        assertEquals(3, t.handNumber());
    }

    @Test
    void sameSeedGivesIdenticalLogWithRandomBots() {
        assertEquals(randomLog(11), randomLog(11));
        assertNotEquals(randomLog(11), randomLog(12));
    }

    private static List<GameEvent> randomLog(long seed) {
        Random random = new Random(seed);   // один генератор на колоду и на ботов
        Table t = table(random, 1000, 1000, 1000, 1000);
        List<Bot> bots = List.of(new RandomBot(random), new RandomBot(random),
                new RandomBot(random), new CallingStationBot());
        List<GameEvent> log = new ArrayList<>();
        new GameRunner(t, bots, log::add).playHands(30);
        return log;
    }

    @Test
    void totalChipsStayTheSameOverManyRandomHands() {
        Random random = new Random(5);
        Table t = table(random, 1000, 1000, 1000, 1000);
        List<Bot> bots = List.of(new RandomBot(random), new RandomBot(random),
                new RandomBot(random), new RandomBot(random));
        new GameRunner(t, bots, e -> { }).playHands(200);
        assertEquals(4000, t.players().stream().mapToInt(Player::stack).sum());
    }

    @Test
    void botThatBreaksRulesIsReplacedBySafeMoveAfterSeveralAttempts() {
        StubbornBot stubborn = new StubbornBot(false, Integer.MAX_VALUE);
        Table t = table(new Random(1), 1000, 1000);
        GameRunner runner = new GameRunner(t, List.of(stubborn, new CallingStationBot()), e -> { });
        assertTrue(runner.playHand());
        assertTrue(t.isHandOver());
        assertTrue(stubborn.rejections > 0);
        assertEquals(0, stubborn.rejections % GameRunner.MAX_ATTEMPTS);
    }

    @Test
    void interactiveBotMayRetryAsLongAsItWants() {
        StubbornBot human = new StubbornBot(true, 10);
        Table t = table(new Random(1), 1000, 1000);
        new GameRunner(t, List.of(human, new CallingStationBot()), e -> { }).playHand();
        assertEquals(10, human.rejections, "никто не подменяет ход человека");
    }

    @Test
    void runnerRejectsWrongNumberOfBots() {
        Table t = table(new Random(1), 1000, 1000, 1000);
        assertThrows(IllegalArgumentException.class, () -> new GameRunner(t, stations(2), e -> { }));
    }

    @Test
    void runnerAndMechanicsNeverPrint() {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer));
        try {
            Random random = new Random(9);
            Table t = table(random, 1000, 1000, 1000);
            List<Bot> bots = List.of(new RandomBot(random), new RandomBot(random), new CallingStationBot());
            new GameRunner(t, bots, e -> { }).playHands(50);
        } finally {
            System.setOut(original);
        }
        assertEquals(0, buffer.size());
    }
}

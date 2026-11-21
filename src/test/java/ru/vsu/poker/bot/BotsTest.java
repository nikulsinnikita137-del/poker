package ru.vsu.poker.bot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import ru.vsu.poker.action.Action;
import ru.vsu.poker.game.Blinds;
import ru.vsu.poker.game.Player;
import ru.vsu.poker.game.Table;

class BotsTest {

    private static Table started() {
        Table t = new Table(List.of(new Player(0, "A", 1000), new Player(1, "B", 1000), new Player(2, "C", 1000)),
                new Blinds(10, 20), new Random(1));
        t.startHand();
        return t;
    }

    @Test
    void callingStationCallsWhenFacingBetAndChecksOtherwise() {
        Table t = started();
        CallingStationBot bot = new CallingStationBot();
        assertEquals(new Action.Call(), bot.decide(t, t.currentPlayer()));
        t.act(new Action.Call());
        t.act(new Action.Call());
        assertEquals(new Action.Check(), bot.decide(t, t.currentPlayer()), "у большого блайнда нет ставки для уравнивания");
    }

    @Test
    void randomBotIsReproducibleForTheSameSeed() {
        Table t = started();
        List<Action> first = new ArrayList<>();
        List<Action> second = new ArrayList<>();
        RandomBot a = new RandomBot(new Random(5));
        RandomBot b = new RandomBot(new Random(5));
        for (int i = 0; i < 30; i++) {
            first.add(a.decide(t, t.currentPlayer()));
            second.add(b.decide(t, t.currentPlayer()));
        }
        assertEquals(first, second);
    }

    @Test
    void randomBotUsesMoreThanOneKindOfAction() {
        Table t = started();
        RandomBot bot = new RandomBot(new Random(3));
        long kinds = java.util.stream.IntStream.range(0, 200)
                .mapToObj(i -> bot.decide(t, t.currentPlayer()).getClass())
                .distinct().count();
        assertTrue(kinds >= 3);
    }

    @Test
    void botsAreNotInteractiveByDefault() {
        assertFalse(new CallingStationBot().isInteractive());
        assertFalse(new RandomBot(new Random(1)).isInteractive());
    }
}

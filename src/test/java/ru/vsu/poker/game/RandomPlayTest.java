package ru.vsu.poker.game;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import ru.vsu.poker.action.Action;
import ru.vsu.poker.action.IllegalActionException;
import ru.vsu.poker.event.GameEvent;

/** Партии со случайными (но воспроизводимыми) ходами: проверяют механику целиком. */
class RandomPlayTest {

    private static final int TOTAL = 4 * 1000;

    private static Action choose(Table t, Random r) {
        int roll = r.nextInt(10);
        if (roll == 0) {
            return new Action.Fold();
        }
        if (roll == 1) {
            return new Action.AllIn();
        }
        if (roll <= 3) {
            return new Action.Raise(t.minRaiseTo());
        }
        return safe(t);
    }

    private static Action safe(Table t) {
        return t.amountToCall(t.currentPlayer()) == 0 ? new Action.Check() : new Action.Call();
    }

    /** Играет раздачи, пока у двух и более игроков есть фишки; возвращает журнал событий. */
    private static List<GameEvent> play(long seed, int maxHands) {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            players.add(new Player(i, "P" + i, 1000));
        }
        Table t = new Table(players, new Blinds(10, 20), new Random(seed));
        Random chooser = new Random(seed + 1);
        List<GameEvent> log = new ArrayList<>();
        for (int hand = 0; hand < maxHands; hand++) {
            if (players.stream().filter(p -> p.stack() > 0).count() < 2) {
                break;
            }
            log.addAll(t.startHand());
            while (!t.isHandOver()) {
                Action action = choose(t, chooser);
                try {
                    log.addAll(t.act(action));
                } catch (IllegalActionException e) {
                    log.addAll(t.act(safe(t)));
                }
            }
            assertEquals(TOTAL, players.stream().mapToInt(Player::stack).sum(), "фишки не должны пропадать");
        }
        return log;
    }

    @Test
    void totalChipsNeverChangeOverManyHands() {
        for (long seed = 1; seed <= 20; seed++) {
            assertFalse(play(seed, 200).isEmpty());
        }
    }

    @Test
    void sameSeedGivesIdenticalGameLog() {
        assertEquals(play(7, 100), play(7, 100));
    }

    @Test
    void differentSeedsGiveDifferentGames() {
        assertNotEquals(play(7, 100), play(8, 100));
    }
}

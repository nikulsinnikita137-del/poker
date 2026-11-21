package ru.vsu.poker.game;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.vsu.poker.action.Action;
import ru.vsu.poker.card.Card;
import ru.vsu.poker.card.Deck;
import ru.vsu.poker.eval.HandCategory;
import ru.vsu.poker.event.GameEvent;

/** Вскрытие и выдача банков на колодах с заданным порядком карт. Блайнды 10/20, кнопка на месте 0. */
class ShowdownTest {

    /**
     * Стол, где у места i карманные карты hole[i], а общие карты — board.
     * Первая раздача: кнопка на месте 0, сдача идёт с места 1 по кругу, по одной карте, два круга.
     */
    private static Table rigged(String[] hole, String board, int... stacks) {
        int n = hole.length;
        List<Card> order = new ArrayList<>();
        for (int round = 0; round < 2; round++) {
            for (int i = 1; i <= n; i++) {
                order.add(Card.parse(hole[i % n].split(" ")[round]));
            }
        }
        Arrays.stream(board.split(" ")).map(Card::parse).forEach(order::add);

        List<Player> players = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            players.add(new Player(i, "P" + i, stacks[i]));
        }
        Table t = new Table(players, new Blinds(10, 20), () -> new Deck(order));
        t.startHand();
        return t;
    }

    private static List<GameEvent> checkDown(Table t) {
        List<GameEvent> all = new ArrayList<>();
        while (!t.isHandOver()) {
            Player p = t.currentPlayer();
            all.addAll(t.act(t.amountToCall(p) == 0 ? new Action.Check() : new Action.Call()));
        }
        return all;
    }

    private static List<Integer> stacks(Table t) {
        return t.players().stream().map(Player::stack).toList();
    }

    @Test
    void bestHandTakesTheWholePot() {
        Table t = rigged(new String[] {"2c 3d", "As Ah", "7c 8d"}, "Kd 9s 4h Jc 5d", 1000, 1000, 1000);
        List<GameEvent> events = checkDown(t);
        assertEquals(List.of(980, 1040, 980), stacks(t));

        List<GameEvent> reveals = events.stream().filter(e -> e instanceof GameEvent.ShowdownReveal).toList();
        assertEquals(3, reveals.size());
        GameEvent.ShowdownReveal winner = (GameEvent.ShowdownReveal) reveals.stream()
                .filter(e -> ((GameEvent.ShowdownReveal) e).seat() == 1).findFirst().orElseThrow();
        assertEquals(HandCategory.PAIR, winner.category());
        assertTrue(events.contains(new GameEvent.PotAwarded(0, 1, 60)));
        assertEquals(new GameEvent.HandFinished(1), events.get(events.size() - 1));
    }

    @Test
    void revealsComeBeforeAwards() {
        Table t = rigged(new String[] {"2c 3d", "As Ah", "7c 8d"}, "Kd 9s 4h Jc 5d", 1000, 1000, 1000);
        List<GameEvent> events = checkDown(t);
        int lastReveal = -1;
        int firstAward = Integer.MAX_VALUE;
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i) instanceof GameEvent.ShowdownReveal) {
                lastReveal = i;
            }
            if (events.get(i) instanceof GameEvent.PotAwarded) {
                firstAward = Math.min(firstAward, i);
            }
        }
        assertTrue(lastReveal < firstAward);
    }

    @Test
    void boardCardsCountForTheWinner() {
        // Флеш собран из карманных и общих карт
        Table t = rigged(new String[] {"As 2s", "Ad Ac"}, "Ks 9s 4s 7d Jc", 1000, 1000);
        List<GameEvent> events = checkDown(t);
        assertEquals(List.of(1020, 980), stacks(t));
        GameEvent.ShowdownReveal flush = (GameEvent.ShowdownReveal) events.stream()
                .filter(e -> e instanceof GameEvent.ShowdownReveal && ((GameEvent.ShowdownReveal) e).seat() == 0)
                .findFirst().orElseThrow();
        assertEquals(HandCategory.FLUSH, flush.category());
    }

    @Test
    void tiedHandsSplitThePot() {
        // Роял-флеш лежит на столе: у всех одинаковая рука
        Table t = rigged(new String[] {"2c 3d", "4c 5d", "6c 7d"}, "Ts Js Qs Ks As", 1000, 1000, 1000);
        checkDown(t);
        assertEquals(List.of(1000, 1000, 1000), stacks(t));
    }

    @Test
    void oddChipGoesToFirstWinnerLeftOfButton() {
        // Банк 303 делят места 1 и 2 (A-K-J-9-7 у обоих), место 0 слабее
        Table t = rigged(new String[] {"4h 5h", "As Ks", "Ad Kd"}, "2c 3d 7h 9s Jc", 101, 101, 101);
        t.act(new Action.AllIn());
        t.act(new Action.AllIn());
        t.act(new Action.AllIn());
        assertTrue(t.isHandOver());
        assertEquals(List.of(0, 152, 151), stacks(t));
    }

    @Test
    void sidePotsGoToDifferentWinners() {
        // Стеки 1000 / 300 / 100. Лучшая рука у самого короткого стека, вторая — у среднего
        Table t = rigged(new String[] {"2c 3d", "Kc Kd", "As Ah"}, "7h 8s 9d Jc 4h", 1000, 300, 100);
        t.act(new Action.AllIn());
        t.act(new Action.AllIn());
        t.act(new Action.AllIn());
        assertTrue(t.isHandOver());
        // Основной банк 300 — месту 2, побочный 400 — месту 1, остаток 700 возвращается месту 0
        assertEquals(List.of(700, 400, 300), stacks(t));
    }

    @Test
    void uncalledPartOfBetReturnsEvenToTheLoser() {
        Table t = rigged(new String[] {"2c 3d", "As Ah"}, "Kd 9s 4h Jc 5d", 1000, 300);
        t.act(new Action.AllIn());   // место 0, кнопка и малый блайнд
        List<GameEvent> events = t.act(new Action.AllIn());
        assertEquals(List.of(700, 600), stacks(t));
        assertTrue(events.contains(new GameEvent.PotAwarded(1, 0, 700)), "возврат — отдельный банк");
        assertTrue(events.contains(new GameEvent.PotAwarded(0, 1, 600)));
    }

    @Test
    void lastPlayerWinsWithoutShowdown() {
        Table t = rigged(new String[] {"2c 3d", "4c 5d", "6c 7d"}, "Ts Js Qs Ks As", 1000, 1000, 1000);
        t.act(new Action.Fold());
        List<GameEvent> events = t.act(new Action.Fold());
        assertEquals(List.of(1000, 990, 1010), stacks(t));
        assertTrue(events.stream().noneMatch(e -> e instanceof GameEvent.ShowdownReveal), "карты не вскрываются");
        assertTrue(events.contains(new GameEvent.PotAwarded(0, 2, 30)));
    }

    @Test
    void foldedPlayersChipsStayInPot() {
        Table t = rigged(new String[] {"2c 3d", "4c 5d", "6c 7d"}, "Ts Js Qs Ks As", 1000, 1000, 1000);
        t.act(new Action.Raise(60));  // место 0
        t.act(new Action.Fold());     // малый блайнд потерял 10
        t.act(new Action.Call());     // большой блайнд
        t.act(new Action.Raise(20));  // флоп: первым ходит большой блайнд
        t.act(new Action.Fold());     // место 0 сбросил
        assertTrue(t.isHandOver());
        assertEquals(List.of(940, 990, 1070), stacks(t));
    }

    @Test
    void allInHeadsUpRunsOutAndPaysWinner() {
        Table t = rigged(new String[] {"As Ad", "7c 2d"}, "Kh 9s 4c 5d Jc", 100, 100);
        t.act(new Action.AllIn());
        t.act(new Action.Call());
        assertEquals(List.of(200, 0), stacks(t));
    }

    @Test
    void nextHandCannotStartBeforeCurrentOneEnds() {
        Table t = rigged(new String[] {"2c 3d", "4c 5d", "6c 7d"}, "Ts Js Qs Ks As", 1000, 1000, 1000);
        assertThrows(IllegalStateException.class, t::startHand);
        t.act(new Action.Fold());
        t.act(new Action.Fold());
        t.startHand();
        assertEquals(2, t.handNumber());
    }
}

package ru.vsu.poker.console;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import ru.vsu.poker.action.Action;
import ru.vsu.poker.card.Card;
import ru.vsu.poker.eval.HandCategory;
import ru.vsu.poker.event.GameEvent;
import ru.vsu.poker.game.Player;

class EventFormatterTest {

    private final EventFormatter formatter = new EventFormatter(
            List.of(new Player(0, "Анна", 1000), new Player(1, "Борис", 1000)), Set.of(0));

    private static List<Card> cards(String text) {
        return java.util.Arrays.stream(text.split(" ")).map(Card::parse).toList();
    }

    private String one(GameEvent event) {
        List<String> lines = formatter.format(event);
        assertEquals(1, lines.size());
        return lines.get(0);
    }

    @Test
    void handStartedHasHeaderWithButton() {
        assertEquals(List.of("", "=== Раздача №3, кнопка: Борис ==="),
                formatter.format(new GameEvent.HandStarted(3, 1, List.of(1, 0))));
    }

    @Test
    void blindsAreDescribed() {
        assertEquals("Анна ставит малый блайнд: 10",
                one(new GameEvent.BlindPosted(0, GameEvent.BlindKind.SMALL, 10)));
        assertEquals("Борис ставит большой блайнд: 20",
                one(new GameEvent.BlindPosted(1, GameEvent.BlindKind.BIG, 20)));
    }

    @Test
    void holeCardsAreShownOnlyForVisibleSeats() {
        assertEquals("Анна получает As Kd", one(new GameEvent.HoleCardsDealt(0, cards("As Kd"))));
        assertEquals("Борис получает закрытые карты", one(new GameEvent.HoleCardsDealt(1, cards("2c 3d"))));
    }

    @Test
    void actionsAreDescribed() {
        assertEquals("Борис: fold", one(new GameEvent.PlayerActed(1, new Action.Fold(), 0)));
        assertEquals("Борис: check", one(new GameEvent.PlayerActed(1, new Action.Check(), 0)));
        assertEquals("Борис: call 20", one(new GameEvent.PlayerActed(1, new Action.Call(), 20)));
        assertEquals("Борис: raise to 60 (внесено 60)", one(new GameEvent.PlayerActed(1, new Action.Raise(60), 60)));
        assertEquals("Борис: all-in (внесено 50)", one(new GameEvent.PlayerActed(1, new Action.AllIn(), 50)));
    }

    @Test
    void flopShowsBoardOnceAndTurnShowsWholeBoard() {
        assertEquals("--- Флоп: Ts Ad 5c ---",
                one(new GameEvent.BoardDealt("Флоп", cards("Ts Ad 5c"), cards("Ts Ad 5c"))));
        assertEquals("--- Тёрн: 6d | стол: Ts Ad 5c 6d ---",
                one(new GameEvent.BoardDealt("Тёрн", cards("6d"), cards("Ts Ad 5c 6d"))));
    }

    @Test
    void showdownAndAwardsAreDescribed() {
        assertEquals("Борис открывает Th Td: Фулл-хаус [Th Td Ts 5c 5d]",
                one(new GameEvent.ShowdownReveal(1, cards("Th Td"), HandCategory.FULL_HOUSE, cards("Th Td Ts 5c 5d"))));
        assertEquals("Борис получает 80 (основной банк)", one(new GameEvent.PotAwarded(0, 1, 80)));
        assertEquals("Анна получает 700 (побочный банк №2)", one(new GameEvent.PotAwarded(2, 0, 700)));
    }

    @Test
    void handFinishedListsStacks() {
        assertEquals("Раздача №3 окончена. Стеки: Анна 1000, Борис 1000", one(new GameEvent.HandFinished(3)));
    }
}

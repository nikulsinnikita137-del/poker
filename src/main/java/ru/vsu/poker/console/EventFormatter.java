package ru.vsu.poker.console;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import ru.vsu.poker.action.Action;
import ru.vsu.poker.card.Card;
import ru.vsu.poker.event.GameEvent;
import ru.vsu.poker.game.Player;

/**
 * Превращает события партии в строки журнала. Это слой вывода: механика про текст ничего не знает.
 * Форматтер только читает имена и стеки игроков и ничего не меняет.
 */
public final class EventFormatter {
    private final List<Player> players;
    private final Set<Integer> visibleHoleCardSeats;

    /**
     * @param players              игроки стола (индекс равен месту)
     * @param visibleHoleCardSeats места, чьи карманные карты показываются при раздаче;
     *                             остальным пишется «закрытые карты» (на вскрытии карты видны всем)
     */
    public EventFormatter(List<Player> players, Set<Integer> visibleHoleCardSeats) {
        this.players = players;
        this.visibleHoleCardSeats = Set.copyOf(visibleHoleCardSeats);
    }

    /**
     * Форматирует событие.
     *
     * @param event событие партии
     * @return одна или несколько строк журнала
     */
    public List<String> format(GameEvent event) {
        if (event instanceof GameEvent.HandStarted e) {
            return List.of("", "=== Раздача №" + e.handNumber() + ", кнопка: " + name(e.button()) + " ===");
        }
        if (event instanceof GameEvent.BlindPosted e) {
            String kind = e.kind() == GameEvent.BlindKind.SMALL ? "малый" : "большой";
            return List.of(name(e.seat()) + " ставит " + kind + " блайнд: " + e.amount());
        }
        if (event instanceof GameEvent.HoleCardsDealt e) {
            String cards = visibleHoleCardSeats.contains(e.seat()) ? cards(e.cards()) : "закрытые карты";
            return List.of(name(e.seat()) + " получает " + cards);
        }
        if (event instanceof GameEvent.PlayerActed e) {
            return List.of(name(e.seat()) + ": " + action(e.action(), e.paid()));
        }
        if (event instanceof GameEvent.BoardDealt e) {
            boolean wholeBoard = e.newCards().size() == e.board().size();
            return List.of("--- " + e.phase() + ": " + cards(e.newCards())
                    + (wholeBoard ? "" : " | стол: " + cards(e.board())) + " ---");
        }
        if (event instanceof GameEvent.ShowdownReveal e) {
            return List.of(name(e.seat()) + " открывает " + cards(e.holeCards()) + ": "
                    + e.category().title() + " [" + cards(e.bestFive()) + "]");
        }
        if (event instanceof GameEvent.PotAwarded e) {
            String pot = e.potIndex() == 0 ? "основной банк" : "побочный банк №" + e.potIndex();
            return List.of(name(e.seat()) + " получает " + e.amount() + " (" + pot + ")");
        }
        if (event instanceof GameEvent.HandFinished e) {
            return List.of("Раздача №" + e.handNumber() + " окончена. Стеки: " + stacks());
        }
        return List.of(event.toString());
    }

    private static String action(Action action, int paid) {
        if (action instanceof Action.Call) {
            return "call " + paid;
        }
        if (action instanceof Action.Raise raise) {
            return "raise to " + raise.to() + " (внесено " + paid + ")";
        }
        if (action instanceof Action.AllIn) {
            return "all-in (внесено " + paid + ")";
        }
        return action.toString();
    }

    private String name(int seat) {
        return players.get(seat).name();
    }

    private String stacks() {
        return players.stream().map(p -> p.name() + " " + p.stack()).collect(Collectors.joining(", "));
    }

    private static String cards(List<Card> cards) {
        return cards.stream().map(Card::toString).collect(Collectors.joining(" "));
    }
}

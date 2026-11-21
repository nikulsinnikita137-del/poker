package ru.vsu.poker.sim;

import java.util.List;
import java.util.function.Consumer;
import ru.vsu.poker.action.Action;
import ru.vsu.poker.action.IllegalActionException;
import ru.vsu.poker.bot.Bot;
import ru.vsu.poker.event.GameEvent;
import ru.vsu.poker.game.Player;
import ru.vsu.poker.game.Table;

/**
 * Прогоняет раздачи: спрашивает у ботов ходы и передаёт события слушателю. Сам ничего не печатает —
 * журнал выводит тот, кто подписался на события.
 */
public final class GameRunner {
    /** Сколько раз бот может ошибиться в ходе, прежде чем за него сходят check/call. */
    static final int MAX_ATTEMPTS = 3;

    private final Table table;
    private final List<Bot> bots;
    private final Consumer<GameEvent> listener;

    /**
     * @param table      стол
     * @param botsBySeat по одному боту на каждое место, индекс равен месту
     * @param listener   получает все события партии по порядку
     * @throws IllegalArgumentException если число ботов не равно числу игроков
     */
    public GameRunner(Table table, List<Bot> botsBySeat, Consumer<GameEvent> listener) {
        if (botsBySeat.size() != table.players().size()) {
            throw new IllegalArgumentException("Ботов (" + botsBySeat.size()
                    + ") должно быть столько же, сколько игроков (" + table.players().size() + ")");
        }
        this.table = table;
        this.bots = List.copyOf(botsBySeat);
        this.listener = listener;
    }

    /**
     * Играет одну раздачу до конца.
     *
     * @return false, если играть уже не с кем (фишки есть меньше чем у двух игроков)
     */
    public boolean playHand() {
        if (table.players().stream().filter(p -> p.stack() > 0).count() < 2) {
            return false;
        }
        emit(table.startHand());
        while (!table.isHandOver()) {
            Player player = table.currentPlayer();
            emit(act(player, bots.get(player.seat())));
        }
        return true;
    }

    /**
     * Играет раздачи, пока не сыграно {@code maxHands} или не остался один игрок с фишками.
     *
     * @param maxHands предел числа раздач
     * @return сколько раздач сыграно
     */
    public int playHands(int maxHands) {
        int played = 0;
        while (played < maxHands && playHand()) {
            played++;
        }
        return played;
    }

    /** Ход бота; при нарушении правил бот пробует снова, а после нескольких ошибок за него ходит check/call. */
    private List<GameEvent> act(Player player, Bot bot) {
        for (int attempt = 1; ; attempt++) {
            Action action = !bot.isInteractive() && attempt > MAX_ATTEMPTS
                    ? safeAction(player)
                    : bot.decide(table, player);
            try {
                return table.act(action);
            } catch (IllegalActionException e) {
                bot.rejected(e.getMessage());
            }
        }
    }

    private Action safeAction(Player player) {
        return table.amountToCall(player) == 0 ? new Action.Check() : new Action.Call();
    }

    private void emit(List<GameEvent> events) {
        events.forEach(listener);
    }
}

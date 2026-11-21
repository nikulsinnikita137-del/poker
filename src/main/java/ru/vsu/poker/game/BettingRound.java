package ru.vsu.poker.game;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import ru.vsu.poker.action.Action;
import ru.vsu.poker.action.IllegalActionException;
import ru.vsu.poker.event.GameEvent;

/**
 * Один круг торговли. Следит за текущей ставкой, размером минимального повышения
 * и тем, кому ещё нужно ходить. Правила ставок собраны здесь, а не в столе.
 *
 * <p>Круг закончен, когда не осталось игроков, которым нужно ходить: каждый либо уравнял ставку,
 * либо сбросил карты, либо поставил всё. Любое повышение возвращает в очередь всех остальных.
 */
public final class BettingRound {
    private final List<Player> order;
    private final Set<Player> pending = new LinkedHashSet<>();
    /** Игроки, которым нельзя повышать: до них не дошло полное повышение (после короткого all-in). */
    private final Set<Player> raiseClosed = new HashSet<>();
    private int currentBet;
    private int minRaise;
    private int pointer = -1;

    /**
     * Создаёт круг торговли.
     *
     * @param order      игроки раздачи в порядке хода, начиная с первого ходящего
     * @param currentBet ставка, которую нужно уравнять (на префлопе — большой блайнд, дальше 0)
     * @param minRaise   минимальный размер повышения (в начале круга — большой блайнд)
     */
    BettingRound(List<Player> order, int currentBet, int minRaise) {
        this.order = List.copyOf(order);
        this.currentBet = currentBet;
        this.minRaise = minRaise;
        for (Player p : order) {
            if (isActive(p)) {
                pending.add(p);
            }
        }
        // Остался один игрок с фишками и ему нечего уравнивать: торговаться не с кем
        if (pending.size() == 1 && amountToCall(pending.iterator().next()) == 0) {
            pending.clear();
        }
        advanceFrom(-1);
    }

    private static boolean isActive(Player p) {
        return p.isInHand() && !p.isFolded() && p.stack() > 0;
    }

    /** @return true, если торговаться в этом круге больше не нужно */
    public boolean isComplete() {
        return pending.isEmpty();
    }

    /**
     * @return игрок, чей сейчас ход
     * @throws IllegalStateException если круг закончен
     */
    public Player currentPlayer() {
        if (isComplete()) {
            throw new IllegalStateException("Круг торговли завершён");
        }
        return order.get(pointer);
    }

    /** @return ставка, которую нужно уравнять */
    public int currentBet() {
        return currentBet;
    }

    /**
     * @param player игрок
     * @return сколько фишек игроку нужно доплатить до текущей ставки (может превышать его стек)
     */
    public int amountToCall(Player player) {
        return Math.max(0, currentBet - player.roundBet());
    }

    /** @return минимальный размер ставки, до которого можно повысить («raise to») */
    public int minRaiseTo() {
        return currentBet + minRaise;
    }

    /**
     * Применяет действие игрока, чей сейчас ход.
     *
     * @param player игрок, который ходит
     * @param action действие
     * @return события круга (ход игрока)
     * @throws IllegalActionException если действие нарушает правила
     * @throws IllegalStateException  если круг уже закончен
     */
    public List<GameEvent> apply(Player player, Action action) {
        Player expected = currentPlayer();
        if (player != expected) {
            throw new IllegalActionException("Сейчас ходит " + expected.name() + ", а не " + player.name());
        }
        int index = pointer;
        int paid;

        if (action instanceof Action.Fold) {
            player.fold();
            paid = 0;
        } else if (action instanceof Action.Check) {
            if (amountToCall(player) > 0) {
                throw new IllegalActionException("Нельзя сделать check: нужно доплатить " + amountToCall(player));
            }
            paid = 0;
        } else if (action instanceof Action.Call) {
            int toCall = amountToCall(player);
            if (toCall == 0) {
                throw new IllegalActionException("Уравнивать нечего: используйте check");
            }
            paid = player.pay(toCall);
        } else if (action instanceof Action.Raise raise) {
            paid = raise(player, raise.to());
        } else if (action instanceof Action.AllIn) {
            paid = allIn(player);
        } else {
            throw new IllegalActionException("Неизвестное действие: " + action);
        }

        pending.remove(player);
        pending.removeIf(p -> !isActive(p));
        advanceFrom(index);
        return List.of(new GameEvent.PlayerActed(player.seat(), action, paid));
    }

    private int raise(Player player, int to) {
        if (raiseClosed.contains(player)) {
            throw new IllegalActionException("Повышать нельзя: полного повышения после вашего хода не было");
        }
        if (to < minRaiseTo()) {
            throw new IllegalActionException("Минимальное повышение — до " + minRaiseTo() + ", а не до " + to);
        }
        int amount = to - player.roundBet();
        if (amount > player.stack()) {
            throw new IllegalActionException("Не хватает фишек для повышения до " + to
                    + " (есть " + player.stack() + "): используйте all-in");
        }
        int paid = player.pay(amount);
        reopen(to - currentBet, to, player);
        return paid;
    }

    private int allIn(Player player) {
        int total = player.roundBet() + player.stack();
        if (total > currentBet && raiseClosed.contains(player)) {
            throw new IllegalActionException("Повышать нельзя: можно только уравнять или сбросить");
        }
        int paid = player.pay(player.stack());
        if (total > currentBet) {
            int raiseSize = total - currentBet;
            if (raiseSize >= minRaise) {
                reopen(raiseSize, total, player);
            } else {
                // Короткий all-in: ставка выросла, но полного повышения нет. Ходившие раньше могут только уравнять
                currentBet = total;
                for (Player other : order) {
                    if (other != player && isActive(other) && other.roundBet() < currentBet) {
                        if (!pending.contains(other)) {
                            raiseClosed.add(other);
                        }
                        pending.add(other);
                    }
                }
            }
        }
        return paid;
    }

    /** Полное повышение: новая ставка и минимальный рейз, все остальные должны ответить. */
    private void reopen(int raiseSize, int newBet, Player raiser) {
        minRaise = raiseSize;
        currentBet = newBet;
        raiseClosed.clear();
        pending.clear();
        for (Player other : order) {
            if (other != raiser && isActive(other)) {
                pending.add(other);
            }
        }
    }

    /** Передаёт ход следующему по кругу игроку, которому ещё нужно ходить. */
    private void advanceFrom(int index) {
        pointer = -1;
        if (pending.isEmpty()) {
            return;
        }
        for (int i = 1; i <= order.size(); i++) {
            int candidate = Math.floorMod(index + i, order.size());
            if (pending.contains(order.get(candidate))) {
                pointer = candidate;
                return;
            }
        }
    }
}

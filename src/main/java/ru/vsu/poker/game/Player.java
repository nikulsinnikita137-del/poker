package ru.vsu.poker.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import ru.vsu.poker.card.Card;

/**
 * Игрок за столом: стек, карманные карты и вклад в текущую раздачу.
 * Сам ничего не решает и не печатает — ходы принимает механика стола.
 */
public final class Player {
    private final int seat;
    private final String name;
    private int stack;
    private int contributed;
    private boolean folded;
    private boolean inHand;
    private final List<Card> holeCards = new ArrayList<>(2);

    /**
     * Создаёт игрока.
     *
     * @param seat  номер места (место 0 — первое по часовой стрелке)
     * @param name  имя
     * @param stack начальный стек (не меньше нуля)
     */
    public Player(int seat, String name, int stack) {
        if (seat < 0) {
            throw new IllegalArgumentException("Отрицательный номер места: " + seat);
        }
        if (stack < 0) {
            throw new IllegalArgumentException("Отрицательный стек: " + stack);
        }
        this.seat = seat;
        this.name = Objects.requireNonNull(name, "name");
        this.stack = stack;
    }

    /** @return номер места */
    public int seat() {
        return seat;
    }

    /** @return имя */
    public String name() {
        return name;
    }

    /** @return сколько фишек осталось у игрока */
    public int stack() {
        return stack;
    }

    /** @return сколько фишек игрок вложил в банк за текущую раздачу */
    public int contributed() {
        return contributed;
    }

    /** @return true, если игрок сбросил карты */
    public boolean isFolded() {
        return folded;
    }

    /** @return true, если игрок участвует в текущей раздаче (имел фишки на её начало) */
    public boolean isInHand() {
        return inHand;
    }

    /** @return true, если игрок в раздаче, не сбросил карты и у него не осталось фишек */
    public boolean isAllIn() {
        return inHand && !folded && stack == 0;
    }

    /** @return карманные карты (только для чтения) */
    public List<Card> holeCards() {
        return Collections.unmodifiableList(holeCards);
    }

    /**
     * Вносит фишки в банк. Если фишек не хватает, вносится весь стек (игрок идёт ва-банк).
     *
     * @param amount желаемая сумма (не меньше нуля)
     * @return сколько фишек реально внесено
     * @throws IllegalArgumentException если сумма отрицательна
     */
    public int pay(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Отрицательная сумма: " + amount);
        }
        int paid = Math.min(amount, stack);
        stack -= paid;
        contributed += paid;
        return paid;
    }

    /**
     * Добавляет выигрыш в стек.
     *
     * @param amount сумма выигрыша (не меньше нуля)
     */
    public void win(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Отрицательный выигрыш: " + amount);
        }
        stack += amount;
    }

    /** Сбрасывает карты: игрок выходит из раздачи, фишки остаются в банке. */
    public void fold() {
        folded = true;
    }

    /**
     * Получает карманную карту.
     *
     * @param card карта
     * @throws IllegalStateException если у игрока уже две карты
     */
    public void receive(Card card) {
        if (holeCards.size() >= 2) {
            throw new IllegalStateException("У игрока " + name + " уже две карты");
        }
        holeCards.add(Objects.requireNonNull(card, "card"));
    }

    /** Готовит игрока к новой раздаче: очищает карты и вклады. В раздаче участвуют только игроки с фишками. */
    void startHand() {
        holeCards.clear();
        contributed = 0;
        folded = false;
        inHand = stack > 0;
    }

    @Override
    public String toString() {
        return name + " (место " + seat + ", стек " + stack + ")";
    }
}

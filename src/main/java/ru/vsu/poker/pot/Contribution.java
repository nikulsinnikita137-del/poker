package ru.vsu.poker.pot;

/**
 * Сколько фишек игрок вложил в банк за всю раздачу.
 *
 * @param seat   номер места игрока
 * @param amount общий вклад (не меньше нуля)
 * @param folded true, если игрок сбросил карты и не претендует на банк
 */
public record Contribution(int seat, int amount, boolean folded) {
    public Contribution {
        if (amount < 0) {
            throw new IllegalArgumentException("Отрицательный вклад: " + amount);
        }
    }
}

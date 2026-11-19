package ru.vsu.poker.game;

import java.util.ArrayList;
import java.util.List;

/** Позиция игрока за столом относительно кнопки дилера. */
public enum Position {
    /** Кнопка дилера. */
    BUTTON,
    /** Малый блайнд (слева от кнопки). */
    SMALL_BLIND,
    /** Большой блайнд. */
    BIG_BLIND,
    /** Первый, кто ходит на префлопе (следующий за большим блайндом). */
    UNDER_THE_GUN,
    /** Средние позиции. */
    MIDDLE,
    /** Игрок перед кнопкой. */
    CUTOFF,
    /** Игра вдвоём: кнопка одновременно ставит малый блайнд. */
    BUTTON_SMALL_BLIND;

    /** Минимум игроков за столом. */
    public static final int MIN_PLAYERS = 2;
    /** Максимум игроков за столом. */
    public static final int MAX_PLAYERS = 10;

    /**
     * Раскладывает позиции для заданного числа игроков.
     * Первый элемент списка — кнопка, дальше по часовой стрелке.
     *
     * @param count число игроков в раздаче (от 2 до 10)
     * @return позиции начиная с кнопки
     * @throws IllegalArgumentException если игроков меньше 2 или больше 10
     */
    public static List<Position> layout(int count) {
        if (count < MIN_PLAYERS || count > MAX_PLAYERS) {
            throw new IllegalArgumentException(
                    "Число игроков должно быть от " + MIN_PLAYERS + " до " + MAX_PLAYERS + ": " + count);
        }
        List<Position> result = new ArrayList<>();
        if (count == 2) {
            result.add(BUTTON_SMALL_BLIND);
            result.add(BIG_BLIND);
            return result;
        }
        result.add(BUTTON);
        result.add(SMALL_BLIND);
        result.add(BIG_BLIND);
        for (int i = 3; i < count; i++) {
            if (i == 3) {
                result.add(UNDER_THE_GUN);
            } else if (i == count - 1) {
                result.add(CUTOFF);
            } else {
                result.add(MIDDLE);
            }
        }
        return result;
    }
}

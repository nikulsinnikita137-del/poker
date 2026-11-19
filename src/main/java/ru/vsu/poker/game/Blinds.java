package ru.vsu.poker.game;

/**
 * Размеры блайндов.
 *
 * @param smallBlind малый блайнд (больше нуля)
 * @param bigBlind   большой блайнд (не меньше малого)
 */
public record Blinds(int smallBlind, int bigBlind) {

    public Blinds {
        if (smallBlind <= 0) {
            throw new IllegalArgumentException("Малый блайнд должен быть положительным: " + smallBlind);
        }
        if (bigBlind < smallBlind) {
            throw new IllegalArgumentException(
                    "Большой блайнд (" + bigBlind + ") меньше малого (" + smallBlind + ")");
        }
    }
}

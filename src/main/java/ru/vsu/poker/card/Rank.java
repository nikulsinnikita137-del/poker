package ru.vsu.poker.card;

/** Достоинство карты. Порядок констант совпадает с порядком старшинства (туз старший). */
public enum Rank {
    TWO('2'), THREE('3'), FOUR('4'), FIVE('5'), SIX('6'), SEVEN('7'), EIGHT('8'),
    NINE('9'), TEN('T'), JACK('J'), QUEEN('Q'), KING('K'), ACE('A');

    private final char symbol;

    Rank(char symbol) {
        this.symbol = symbol;
    }

    /** @return однобуквенное обозначение достоинства (2-9, T, J, Q, K, A) */
    public char symbol() {
        return symbol;
    }

    /**
     * Находит достоинство по обозначению.
     *
     * @param c символ достоинства
     * @return достоинство
     * @throws IllegalArgumentException если символ неизвестен
     */
    public static Rank fromSymbol(char c) {
        for (Rank r : values()) {
            if (r.symbol == Character.toUpperCase(c)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Неизвестное достоинство: " + c);
    }
}

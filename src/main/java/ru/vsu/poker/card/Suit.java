package ru.vsu.poker.card;

/** Масть карты. Масти в покере не имеют старшинства. */
public enum Suit {
    CLUBS('c'), DIAMONDS('d'), HEARTS('h'), SPADES('s');

    private final char symbol;

    Suit(char symbol) {
        this.symbol = symbol;
    }

    /** @return однобуквенное обозначение масти (c, d, h, s) */
    public char symbol() {
        return symbol;
    }

    /**
     * Находит масть по однобуквенному обозначению.
     *
     * @param c символ масти
     * @return масть
     * @throws IllegalArgumentException если символ не соответствует ни одной масти
     */
    public static Suit fromSymbol(char c) {
        for (Suit s : values()) {
            if (s.symbol == Character.toLowerCase(c)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Неизвестная масть: " + c);
    }
}

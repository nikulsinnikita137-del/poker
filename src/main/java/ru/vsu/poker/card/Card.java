package ru.vsu.poker.card;

import java.util.Comparator;
import java.util.Objects;

/**
 * Игральная карта. Неизменяема.
 * Естественный порядок — по достоинству, затем по масти (масть нужна только для детерминированности).
 */
public record Card(Rank rank, Suit suit) implements Comparable<Card> {

    /** Сравнение только по достоинству — именно оно важно при определении комбинаций. */
    public static final Comparator<Card> BY_RANK = Comparator.comparing(Card::rank);

    public Card {
        Objects.requireNonNull(rank, "rank");
        Objects.requireNonNull(suit, "suit");
    }

    /**
     * Разбирает карту из строки вида «As», «Td», «2c».
     *
     * @param text две буквы: достоинство и масть
     * @return карта
     * @throws IllegalArgumentException если формат неверен
     */
    public static Card parse(String text) {
        if (text == null || text.length() != 2) {
            throw new IllegalArgumentException("Ожидалась строка из двух символов, получено: " + text);
        }
        return new Card(Rank.fromSymbol(text.charAt(0)), Suit.fromSymbol(text.charAt(1)));
    }

    @Override
    public int compareTo(Card other) {
        int c = rank.compareTo(other.rank);
        return c != 0 ? c : suit.compareTo(other.suit);
    }

    @Override
    public String toString() {
        return "" + rank.symbol() + suit.symbol();
    }
}

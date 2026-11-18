package ru.vsu.poker.eval;

import java.util.List;
import ru.vsu.poker.card.Card;

/**
 * Результат оценки: сила комбинации и те пять карт, из которых она собрана.
 *
 * @param value сила комбинации
 * @param cards пять карт комбинации
 */
public record HandResult(HandValue value, List<Card> cards) implements Comparable<HandResult> {

    public HandResult {
        cards = List.copyOf(cards);
    }

    /** @return категория комбинации */
    public HandCategory category() {
        return value.category();
    }

    @Override
    public int compareTo(HandResult other) {
        return value.compareTo(other.value);
    }
}

package ru.vsu.poker.eval;

import java.util.Comparator;
import java.util.List;
import ru.vsu.poker.card.Rank;

/**
 * Сила пятикарточной комбинации: категория и список достоинств для разрешения ничьих.
 * Достоинства идут в порядке значимости (например, для фулл-хауса: тройка, затем пара).
 * Две руки равны по силе, если совпадают категория и все достоинства — масти не важны.
 */
public record HandValue(HandCategory category, List<Rank> tiebreakers) implements Comparable<HandValue> {

    /** Сравнение сил: сначала категория, затем достоинства слева направо. */
    public static final Comparator<HandValue> COMPARATOR =
            Comparator.comparing(HandValue::category)
                    .thenComparing(HandValue::tiebreakers, HandValue::compareRankLists);

    public HandValue {
        tiebreakers = List.copyOf(tiebreakers);
    }

    private static int compareRankLists(List<Rank> a, List<Rank> b) {
        for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
            int c = a.get(i).compareTo(b.get(i));
            if (c != 0) {
                return c;
            }
        }
        return Integer.compare(a.size(), b.size());
    }

    @Override
    public int compareTo(HandValue other) {
        return COMPARATOR.compare(this, other);
    }
}

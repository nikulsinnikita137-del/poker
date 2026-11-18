package ru.vsu.poker.eval;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import ru.vsu.poker.card.Card;
import ru.vsu.poker.card.Rank;

/**
 * Определяет лучшую комбинацию из 5–7 карт перебором всех пятикарточных сочетаний
 * (для 7 карт их 21). Перебор прост и заведомо корректен, а скорости для учебной задачи хватает.
 */
public final class HandEvaluator {

    private HandEvaluator() {
    }

    /**
     * Находит лучшую пятикарточную комбинацию.
     *
     * @param cards от 5 до 7 различных карт (две карманные и до пяти общих)
     * @return лучшая комбинация и составляющие её карты
     * @throws IllegalArgumentException если карт меньше 5, больше 7 или есть повторы
     */
    public static HandResult evaluate(List<Card> cards) {
        if (cards.size() < 5 || cards.size() > 7) {
            throw new IllegalArgumentException("Нужно от 5 до 7 карт, получено: " + cards.size());
        }
        if (cards.stream().distinct().count() != cards.size()) {
            throw new IllegalArgumentException("Повторяющиеся карты: " + cards);
        }
        HandResult best = null;
        for (List<Card> five : combinations(cards, 5)) {
            HandResult r = evaluateFive(five);
            if (best == null || r.compareTo(best) > 0) {
                best = r;
            }
        }
        return best;
    }

    /**
     * Оценивает ровно пять карт.
     *
     * @param five пять карт
     * @return результат оценки
     */
    static HandResult evaluateFive(List<Card> five) {
        List<Card> sorted = new ArrayList<>(five);
        sorted.sort(Card.BY_RANK.reversed());

        boolean flush = sorted.stream().map(Card::suit).distinct().count() == 1;

        // Группы по достоинству: сначала более многочисленные, внутри — более старшие
        Map<Rank, Integer> counts = new EnumMap<>(Rank.class);
        for (Card c : sorted) {
            counts.merge(c.rank(), 1, Integer::sum);
        }
        List<Rank> groups = new ArrayList<>(counts.keySet());
        groups.sort(Comparator.<Rank>comparingInt(counts::get).reversed()
                .thenComparing(Comparator.reverseOrder()));
        int top = counts.get(groups.get(0));
        int second = groups.size() > 1 ? counts.get(groups.get(1)) : 0;

        Rank straightHigh = straightHigh(sorted);

        HandCategory category;
        List<Rank> tiebreakers = groups;
        if (straightHigh != null && flush) {
            category = straightHigh == Rank.ACE ? HandCategory.ROYAL_FLUSH : HandCategory.STRAIGHT_FLUSH;
            tiebreakers = List.of(straightHigh);
        } else if (top == 4) {
            category = HandCategory.FOUR_OF_A_KIND;
        } else if (top == 3 && second == 2) {
            category = HandCategory.FULL_HOUSE;
        } else if (flush) {
            category = HandCategory.FLUSH;
        } else if (straightHigh != null) {
            category = HandCategory.STRAIGHT;
            tiebreakers = List.of(straightHigh);
        } else if (top == 3) {
            category = HandCategory.THREE_OF_A_KIND;
        } else if (top == 2 && second == 2) {
            category = HandCategory.TWO_PAIR;
        } else if (top == 2) {
            category = HandCategory.PAIR;
        } else {
            category = HandCategory.HIGH_CARD;
        }
        return new HandResult(new HandValue(category, tiebreakers), sorted);
    }

    /**
     * @param sorted пять карт по убыванию достоинства
     * @return старшая карта стрита или null, если стрита нет. Колесо (A-2-3-4-5) — стрит со старшей пятёркой.
     */
    private static Rank straightHigh(List<Card> sorted) {
        List<Rank> r = sorted.stream().map(Card::rank).toList();
        if (r.stream().distinct().count() != 5) {
            return null;
        }
        if (r.get(0).ordinal() - r.get(4).ordinal() == 4) {
            return r.get(0);
        }
        if (r.equals(List.of(Rank.ACE, Rank.FIVE, Rank.FOUR, Rank.THREE, Rank.TWO))) {
            return Rank.FIVE;
        }
        return null;
    }

    /** Все сочетания по k элементов. */
    static <T> List<List<T>> combinations(List<T> items, int k) {
        List<List<T>> result = new ArrayList<>();
        build(items, k, 0, new ArrayList<>(), result);
        return result;
    }

    private static <T> void build(List<T> items, int k, int start, List<T> cur, List<List<T>> out) {
        if (cur.size() == k) {
            out.add(new ArrayList<>(cur));
            return;
        }
        for (int i = start; i < items.size(); i++) {
            cur.add(items.get(i));
            build(items, k, i + 1, cur, out);
            cur.remove(cur.size() - 1);
        }
    }
}

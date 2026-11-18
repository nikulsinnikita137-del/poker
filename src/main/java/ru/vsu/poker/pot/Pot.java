package ru.vsu.poker.pot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Один банк (основной или побочный).
 * Банк, в котором претендент один, — это возврат неуравненной ставки её владельцу.
 *
 * @param amount   число фишек в банке
 * @param eligible места игроков, которые могут выиграть этот банк
 */
public record Pot(int amount, Set<Integer> eligible) {

    public Pot {
        eligible = Set.copyOf(eligible);
    }

    /**
     * Делит банк поровну между победителями. Неделимый остаток (по одной фишке) достаётся
     * победителям по очереди в порядке списка — по правилам это порядок за кнопкой по часовой стрелке.
     *
     * @param winnersInPayoutOrder места победителей в порядке получения лишних фишек
     * @return места победителей и их выигрыш
     * @throws IllegalArgumentException если победителей нет или кто-то из них не претендует на этот банк
     */
    public Map<Integer, Integer> split(List<Integer> winnersInPayoutOrder) {
        if (winnersInPayoutOrder.isEmpty()) {
            throw new IllegalArgumentException("Нет победителей");
        }
        for (int seat : winnersInPayoutOrder) {
            if (!eligible.contains(seat)) {
                throw new IllegalArgumentException("Место " + seat + " не претендует на этот банк");
            }
        }
        int n = winnersInPayoutOrder.size();
        int share = amount / n;
        int extra = amount % n;
        Map<Integer, Integer> result = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            result.merge(winnersInPayoutOrder.get(i), share + (i < extra ? 1 : 0), Integer::sum);
        }
        return result;
    }
}

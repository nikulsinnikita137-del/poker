package ru.vsu.poker.pot;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Разбивает вклады игроков на основной и побочные банки.
 * Чистая функция: ничего не хранит и не печатает, поэтому легко тестируется.
 *
 * <p>Идея: уровни — это различные вклады игроков, оставшихся в раздаче. На каждом уровне
 * все (и сбросившие тоже) вкладывают до уровня включительно, а претендуют на получившийся
 * банк те, кто не сбросил и вложил не меньше уровня.
 */
public final class SidePotCalculator {

    private SidePotCalculator() {
    }

    /**
     * Строит банки по вкладам.
     *
     * @param contributions вклады всех игроков раздачи
     * @return банки от основного к последнему побочному; сумма их размеров равна сумме вкладов
     */
    public static List<Pot> calculate(List<Contribution> contributions) {
        TreeSet<Integer> levels = contributions.stream()
                .filter(c -> !c.folded() && c.amount() > 0)
                .map(Contribution::amount)
                .collect(Collectors.toCollection(TreeSet::new));

        List<Pot> pots = new ArrayList<>();
        int previous = 0;
        for (int level : levels) {
            int amount = 0;
            for (Contribution c : contributions) {
                amount += Math.min(c.amount(), level) - Math.min(c.amount(), previous);
            }
            int lvl = level;
            Set<Integer> eligible = contributions.stream()
                    .filter(c -> !c.folded() && c.amount() >= lvl)
                    .map(Contribution::seat)
                    .collect(Collectors.toSet());
            pots.add(new Pot(amount, eligible));
            previous = level;
        }

        // Фишки сбросивших, превышающие высший уровень, добавляются в последний банк
        int leftover = 0;
        for (Contribution c : contributions) {
            leftover += Math.max(0, c.amount() - previous);
        }
        if (leftover > 0 && !pots.isEmpty()) {
            Pot last = pots.remove(pots.size() - 1);
            pots.add(new Pot(last.amount() + leftover, last.eligible()));
        }
        return pots;
    }
}

package ru.vsu.poker.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.vsu.poker.card.Card;
import ru.vsu.poker.eval.HandEvaluator;
import ru.vsu.poker.eval.HandResult;
import ru.vsu.poker.event.GameEvent;
import ru.vsu.poker.pot.Contribution;
import ru.vsu.poker.pot.Pot;
import ru.vsu.poker.pot.SidePotCalculator;

/**
 * Подводит итог раздачи: делит вклады на основной и побочные банки, определяет победителя
 * каждого банка и выдаёт выигрыш. Не хранит состояния и ничего не печатает.
 */
final class HandResolver {

    private HandResolver() {
    }

    /**
     * Выдаёт банки победителям.
     *
     * @param table     все игроки стола (индекс равен месту)
     * @param inHand    игроки раздачи
     * @param community общие карты
     * @param button    место кнопки (от него считается порядок выдачи нечётных фишек)
     * @return события вскрытия и выдачи банков
     */
    static List<GameEvent> resolve(List<Player> table, List<Player> inHand, List<Card> community, int button) {
        List<Contribution> contributions = inHand.stream()
                .map(p -> new Contribution(p.seat(), p.contributed(), p.isFolded()))
                .toList();
        List<Pot> pots = SidePotCalculator.calculate(contributions);
        List<Player> live = inHand.stream().filter(p -> !p.isFolded()).toList();

        // Если остался один игрок, карты не вскрываются: все банки забирает он
        boolean showdown = live.size() > 1;
        Map<Integer, HandResult> hands = new HashMap<>();
        List<GameEvent> events = new ArrayList<>();
        if (showdown) {
            for (Player p : live) {
                List<Card> seven = new ArrayList<>(p.holeCards());
                seven.addAll(community);
                HandResult result = HandEvaluator.evaluate(seven);
                hands.put(p.seat(), result);
                events.add(new GameEvent.ShowdownReveal(p.seat(), p.holeCards(), result.category(), result.cards()));
            }
        }

        for (int i = 0; i < pots.size(); i++) {
            Pot pot = pots.get(i);
            List<Integer> winners = showdown ? bestAmong(pot.eligible(), hands) : List.of(live.get(0).seat());
            winners = fromLeftOfButton(winners, button, table.size());
            for (Map.Entry<Integer, Integer> share : pot.split(winners).entrySet()) {
                table.get(share.getKey()).win(share.getValue());
                events.add(new GameEvent.PotAwarded(i, share.getKey(), share.getValue()));
            }
        }
        return events;
    }

    /** Места претендентов с сильнейшей рукой (несколько — при равенстве). */
    private static List<Integer> bestAmong(Set<Integer> eligible, Map<Integer, HandResult> hands) {
        HandResult best = eligible.stream().map(hands::get).max(Comparator.naturalOrder()).orElseThrow();
        return eligible.stream().filter(seat -> hands.get(seat).compareTo(best) == 0).toList();
    }

    /** Упорядочивает места по часовой стрелке начиная с первого слева от кнопки. */
    private static List<Integer> fromLeftOfButton(List<Integer> seats, int button, int tableSize) {
        return seats.stream()
                .sorted(Comparator.comparingInt(seat -> Math.floorMod(seat - button - 1, tableSize)))
                .toList();
    }
}

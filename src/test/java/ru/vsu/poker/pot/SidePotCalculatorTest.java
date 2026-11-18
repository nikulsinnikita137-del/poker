package ru.vsu.poker.pot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SidePotCalculatorTest {

    private static Contribution in(int seat, int amount) {
        return new Contribution(seat, amount, false);
    }

    private static Contribution out(int seat, int amount) {
        return new Contribution(seat, amount, true);
    }

    @Test
    void equalContributionsMakeOnePot() {
        List<Pot> pots = SidePotCalculator.calculate(List.of(in(0, 100), in(1, 100), in(2, 100)));
        assertEquals(1, pots.size());
        assertEquals(300, pots.get(0).amount());
        assertEquals(Set.of(0, 1, 2), pots.get(0).eligible());
    }

    @Test
    void oneShortAllInMakesMainAndSidePot() {
        List<Pot> pots = SidePotCalculator.calculate(List.of(in(0, 50), in(1, 100), in(2, 100)));
        assertEquals(2, pots.size());
        assertEquals(150, pots.get(0).amount());
        assertEquals(Set.of(0, 1, 2), pots.get(0).eligible());
        assertEquals(100, pots.get(1).amount());
        assertEquals(Set.of(1, 2), pots.get(1).eligible());
    }

    @Test
    void threeDifferentStacksMakeThreePots() {
        List<Pot> pots = SidePotCalculator.calculate(List.of(in(0, 30), in(1, 80), in(2, 200), in(3, 200)));
        assertEquals(List.of(120, 150, 240), pots.stream().map(Pot::amount).toList());
        assertEquals(Set.of(0, 1, 2, 3), pots.get(0).eligible());
        assertEquals(Set.of(1, 2, 3), pots.get(1).eligible());
        assertEquals(Set.of(2, 3), pots.get(2).eligible());
    }

    @Test
    void foldedChipsStayInPotButFoldedPlayerIsNotEligible() {
        List<Pot> pots = SidePotCalculator.calculate(List.of(in(0, 100), in(1, 100), out(2, 40)));
        assertEquals(1, pots.size());
        assertEquals(240, pots.get(0).amount());
        assertEquals(Set.of(0, 1), pots.get(0).eligible());
    }

    @Test
    void foldedPlayerFeedsSeveralLevels() {
        // Сбросивший вложил 70: 30 уходит в основной банк, 40 — в побочный
        List<Pot> pots = SidePotCalculator.calculate(List.of(in(0, 30), in(1, 100), in(2, 100), out(3, 70)));
        assertEquals(List.of(120, 180), pots.stream().map(Pot::amount).toList());
        assertEquals(Set.of(1, 2), pots.get(1).eligible());
    }

    @Test
    void uncalledBetFormsSingleEligiblePot() {
        List<Pot> pots = SidePotCalculator.calculate(List.of(in(0, 100), in(1, 60)));
        assertEquals(2, pots.size());
        assertEquals(120, pots.get(0).amount());
        assertEquals(40, pots.get(1).amount());
        assertEquals(Set.of(0), pots.get(1).eligible());
    }

    @Test
    void totalOfPotsEqualsTotalOfContributions() {
        List<Contribution> cs = List.of(in(0, 13), in(1, 57), out(2, 101), in(3, 57), in(4, 250), out(5, 9));
        int total = cs.stream().mapToInt(Contribution::amount).sum();
        assertEquals(total, SidePotCalculator.calculate(cs).stream().mapToInt(Pot::amount).sum());
    }

    @Test
    void noContributionsMeansNoPots() {
        assertTrue(SidePotCalculator.calculate(List.of(in(0, 0), in(1, 0))).isEmpty());
    }

    @Test
    void negativeContributionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Contribution(0, -1, false));
    }

    @Test
    void splitDividesEvenly() {
        Pot pot = new Pot(300, Set.of(0, 1));
        assertEquals(Map.of(0, 150, 1, 150), pot.split(List.of(0, 1)));
    }

    @Test
    void oddChipGoesToFirstWinnerInPayoutOrder() {
        Pot pot = new Pot(101, Set.of(2, 4, 5));
        Map<Integer, Integer> res = pot.split(List.of(4, 5));
        assertEquals(51, res.get(4).intValue());
        assertEquals(50, res.get(5).intValue());
    }

    @Test
    void splitAmongThreeDistributesRemainderOneByOne() {
        Pot pot = new Pot(100, Set.of(0, 1, 2));
        Map<Integer, Integer> res = pot.split(List.of(2, 0, 1));
        assertEquals(34, res.get(2).intValue());
        assertEquals(33, res.get(0).intValue());
        assertEquals(33, res.get(1).intValue());
        assertEquals(100, res.values().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    void splitRejectsNoWinnersAndIneligibleWinners() {
        Pot pot = new Pot(100, Set.of(0, 1));
        assertThrows(IllegalArgumentException.class, () -> pot.split(List.of()));
        assertThrows(IllegalArgumentException.class, () -> pot.split(List.of(2)));
    }
}

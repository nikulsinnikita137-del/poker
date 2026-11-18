package ru.vsu.poker.eval;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.vsu.poker.card.Card;
import ru.vsu.poker.card.Rank;

class HandEvaluatorTest {

    private static List<Card> cards(String s) {
        return Arrays.stream(s.split(" ")).map(Card::parse).toList();
    }

    private static HandResult eval(String s) {
        return HandEvaluator.evaluate(cards(s));
    }

    private static void assertCategory(HandCategory expected, String hand) {
        assertEquals(expected, eval(hand).category(), hand);
    }

    @Test
    void recognisesEveryCategory() {
        assertCategory(HandCategory.HIGH_CARD, "As Kd 9c 7h 4s");
        assertCategory(HandCategory.PAIR, "As Ad 9c 7h 4s");
        assertCategory(HandCategory.TWO_PAIR, "As Ad 9c 9h 4s");
        assertCategory(HandCategory.THREE_OF_A_KIND, "As Ad Ac 7h 4s");
        assertCategory(HandCategory.STRAIGHT, "5s 6d 7c 8h 9s");
        assertCategory(HandCategory.FLUSH, "As Ks 9s 7s 4s");
        assertCategory(HandCategory.FULL_HOUSE, "As Ad Ac 7h 7s");
        assertCategory(HandCategory.FOUR_OF_A_KIND, "As Ad Ac Ah 4s");
        assertCategory(HandCategory.STRAIGHT_FLUSH, "5s 6s 7s 8s 9s");
        assertCategory(HandCategory.ROYAL_FLUSH, "Ts Js Qs Ks As");
    }

    @Test
    void categoriesAreOrderedByStrength() {
        List<String> ladder = List.of(
                "As Kd 9c 7h 4s", "As Ad 9c 7h 4s", "As Ad 9c 9h 4s", "As Ad Ac 7h 4s",
                "5s 6d 7c 8h 9s", "As Ks 9s 7s 4s", "As Ad Ac 7h 7s", "As Ad Ac Ah 4s",
                "5s 6s 7s 8s 9s", "Ts Js Qs Ks As");
        for (int i = 1; i < ladder.size(); i++) {
            assertTrue(eval(ladder.get(i)).compareTo(eval(ladder.get(i - 1))) > 0,
                    ladder.get(i) + " должна быть сильнее " + ladder.get(i - 1));
        }
    }

    @Test
    void wheelIsFiveHighStraight() {
        HandResult wheel = eval("As 2d 3c 4h 5s");
        assertEquals(HandCategory.STRAIGHT, wheel.category());
        assertEquals(List.of(Rank.FIVE), wheel.value().tiebreakers());
        assertTrue(eval("2s 3d 4c 5h 6s").compareTo(wheel) > 0, "6-high бьёт колесо");
    }

    @Test
    void wheelStraightFlushIsNotRoyal() {
        assertCategory(HandCategory.STRAIGHT_FLUSH, "As 2s 3s 4s 5s");
    }

    @Test
    void aceDoesNotWrapAround() {
        assertCategory(HandCategory.HIGH_CARD, "Qs Kd As 2c 3h");
    }

    @Test
    void kickerDecidesBetweenEqualPairs() {
        assertTrue(eval("As Ad Kc 7h 4s").compareTo(eval("Ac Ah Qc 7d 4h")) > 0);
    }

    @Test
    void highPairBeatsLowPairInTwoPair() {
        assertTrue(eval("Ks Kd 2c 2h 4s").compareTo(eval("Qs Qd Jc Jh As")) > 0);
    }

    @Test
    void fullHouseComparesTripsFirst() {
        assertTrue(eval("3s 3d 3c 2h 2s").compareTo(eval("2c 2d 2h As Ad")) > 0);
    }

    @Test
    void suitsDoNotMatterForEquality() {
        assertEquals(0, eval("As Kd 9c 7h 4s").compareTo(eval("Ac Kh 9d 7s 4c")));
    }

    @Test
    void picksBestFiveOfSeven() {
        // Фулл-хаус на тройке тузов и паре королей; лишние две карты не мешают
        HandResult r = eval("As Ad Ac Kh Ks 2d 3c");
        assertEquals(HandCategory.FULL_HOUSE, r.category());
        assertEquals(List.of(Rank.ACE, Rank.KING), r.value().tiebreakers());
        assertEquals(5, r.cards().size());
    }

    @Test
    void flushBeatsStraightWhenBothAvailable() {
        assertCategory(HandCategory.FLUSH, "5h 6h 7h 8h 9d Kh 2h".replace("9d", "9c"));
    }

    @Test
    void twoTripsMakeFullHouse() {
        HandResult r = eval("Ks Kd Kc 5h 5s 5d 2c");
        assertEquals(HandCategory.FULL_HOUSE, r.category());
        assertEquals(List.of(Rank.KING, Rank.FIVE), r.value().tiebreakers());
    }

    @Test
    void threePairsUsesBestKicker() {
        // Три пары: берутся две старшие, кикер — старшая из оставшихся (в т.ч. из третьей пары)
        HandResult r = eval("Ks Kd Qc Qh 7s 7d 2c");
        assertEquals(HandCategory.TWO_PAIR, r.category());
        assertEquals(List.of(Rank.KING, Rank.QUEEN, Rank.SEVEN), r.value().tiebreakers());
    }

    @Test
    void boardPlaysForBothPlayers() {
        String board = "As Ks Qs Js Ts";
        assertEquals(0, eval(board + " 2c 3d").compareTo(eval(board + " 4h 5h")));
    }

    @Test
    void invalidInputsThrow() {
        assertThrows(IllegalArgumentException.class, () -> eval("As Kd 9c 7h"));
        assertThrows(IllegalArgumentException.class, () -> eval("As Kd 9c 7h 4s 3d 2c 2h"));
        assertThrows(IllegalArgumentException.class, () -> eval("As As 9c 7h 4s"));
    }
}

package ru.vsu.poker.game;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class PositionAndBlindsTest {

    @Test
    void headsUpButtonIsSmallBlind() {
        assertEquals(List.of(Position.BUTTON_SMALL_BLIND, Position.BIG_BLIND), Position.layout(2));
    }

    @Test
    void threePlayersHaveOnlyBlindsAndButton() {
        assertEquals(List.of(Position.BUTTON, Position.SMALL_BLIND, Position.BIG_BLIND), Position.layout(3));
    }

    @Test
    void fourPlayersHaveUnderTheGunBeforeButton() {
        assertEquals(List.of(Position.BUTTON, Position.SMALL_BLIND, Position.BIG_BLIND,
                Position.UNDER_THE_GUN), Position.layout(4));
    }

    @Test
    void sixPlayersHaveMiddleAndCutoff() {
        assertEquals(List.of(Position.BUTTON, Position.SMALL_BLIND, Position.BIG_BLIND,
                Position.UNDER_THE_GUN, Position.MIDDLE, Position.CUTOFF), Position.layout(6));
    }

    @Test
    void layoutSizeMatchesPlayerCount() {
        for (int n = Position.MIN_PLAYERS; n <= Position.MAX_PLAYERS; n++) {
            assertEquals(n, Position.layout(n).size());
        }
    }

    @Test
    void layoutRejectsBadPlayerCount() {
        assertThrows(IllegalArgumentException.class, () -> Position.layout(1));
        assertThrows(IllegalArgumentException.class, () -> Position.layout(11));
    }

    @Test
    void blindsValidation() {
        assertEquals(20, new Blinds(10, 20).bigBlind());
        assertThrows(IllegalArgumentException.class, () -> new Blinds(0, 20));
        assertThrows(IllegalArgumentException.class, () -> new Blinds(20, 10));
    }
}

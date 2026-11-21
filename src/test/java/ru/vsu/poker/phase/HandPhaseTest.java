package ru.vsu.poker.phase;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class HandPhaseTest {

    @Test
    void phasesFollowEachOtherInOrder() {
        List<String> names = new ArrayList<>();
        HandPhase phase = new PreFlopPhase();
        while (true) {
            names.add(phase.name());
            if (phase instanceof ShowdownPhase) {
                break;
            }
            phase = phase.next();
        }
        assertEquals(List.of("Префлоп", "Флоп", "Тёрн", "Ривер", "Вскрытие"), names);
    }

    @Test
    void boardCardsAddUpToFive() {
        int total = 0;
        HandPhase phase = new PreFlopPhase();
        while (!(phase instanceof ShowdownPhase)) {
            total += phase.boardCardsToDeal();
            phase = phase.next();
        }
        assertEquals(5, total);
        assertEquals(3, new FlopPhase().boardCardsToDeal());
        assertEquals(1, new TurnPhase().boardCardsToDeal());
        assertEquals(1, new RiverPhase().boardCardsToDeal());
    }

    @Test
    void preFlopFirstActorIsAfterBigBlindExceptHeadsUp() {
        HandPhase pre = new PreFlopPhase();
        assertEquals(0, pre.firstToActIndex(2), "вдвоём первой ходит кнопка");
        assertEquals(0, pre.firstToActIndex(3), "втроём первой ходит кнопка");
        assertEquals(3, pre.firstToActIndex(4));
        assertEquals(3, pre.firstToActIndex(9));
    }

    @Test
    void postFlopFirstActorIsLeftOfButton() {
        for (HandPhase phase : List.of(new FlopPhase(), new TurnPhase(), new RiverPhase())) {
            assertEquals(1, phase.firstToActIndex(2), phase.name());
            assertEquals(1, phase.firstToActIndex(3), phase.name());
            assertEquals(1, phase.firstToActIndex(9), phase.name());
        }
    }

    @Test
    void showdownIsTerminalAndHasNoBetting() {
        HandPhase showdown = new ShowdownPhase();
        assertFalse(showdown.isBetting());
        assertTrue(new FlopPhase().isBetting());
        assertThrows(IllegalStateException.class, showdown::next);
        assertThrows(UnsupportedOperationException.class, () -> showdown.firstToActIndex(3));
    }
}

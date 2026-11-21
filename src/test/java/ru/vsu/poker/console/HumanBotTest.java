package ru.vsu.poker.console;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import ru.vsu.poker.action.Action;
import ru.vsu.poker.game.Blinds;
import ru.vsu.poker.game.Player;
import ru.vsu.poker.game.Table;

class HumanBotTest {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final PrintStream out = new PrintStream(buffer);

    private Table startedTable() {
        Table t = new Table(List.of(new Player(0, "A", 1000), new Player(1, "B", 1000), new Player(2, "C", 1000)),
                new Blinds(10, 20), new Random(1));
        t.startHand();   // первым ходит место 0, ему нужно уравнять 20
        return t;
    }

    private HumanBot bot(String input) {
        return new HumanBot(new BufferedReader(new StringReader(input)), out);
    }

    @Test
    void parsesSimpleCommands() {
        assertEquals(new Action.Fold(), HumanBot.parse("fold", false));
        assertEquals(new Action.Fold(), HumanBot.parse("  F ", false));
        assertEquals(new Action.AllIn(), HumanBot.parse("allin", false));
        assertEquals(new Action.AllIn(), HumanBot.parse("A", false));
    }

    @Test
    void callCommandMeansCheckWhenNothingToCall() {
        assertEquals(new Action.Check(), HumanBot.parse("c", true));
        assertEquals(new Action.Call(), HumanBot.parse("c", false));
        assertEquals(new Action.Call(), HumanBot.parse("check", false));
    }

    @Test
    void parsesRaiseWithAmount() {
        assertEquals(new Action.Raise(60), HumanBot.parse("raise 60", false));
        assertEquals(new Action.Raise(100), HumanBot.parse("r 100", false));
    }

    @Test
    void helpReturnsNull() {
        assertNull(HumanBot.parse("help", false));
    }

    @Test
    void badCommandsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> HumanBot.parse("raise", false));
        assertThrows(IllegalArgumentException.class, () -> HumanBot.parse("raise abc", false));
        assertThrows(IllegalArgumentException.class, () -> HumanBot.parse("dance", false));
        assertThrows(IllegalArgumentException.class, () -> HumanBot.parse("", false));
    }

    @Test
    void asksAgainAfterBadInput() {
        Table t = startedTable();
        Action action = bot("dance\nraise\nr 40\n").decide(t, t.currentPlayer());
        assertEquals(new Action.Raise(40), action);
        String text = buffer.toString();
        assertTrue(text.contains("Ваш ход"));
        assertTrue(text.contains("уравнять: 20"));
        assertEquals(2, text.split("Не понял команду", -1).length - 1);
    }

    @Test
    void helpPrintsHintAndAsksAgain() {
        Table t = startedTable();
        Action action = bot("help\nc\n").decide(t, t.currentPlayer());
        assertEquals(new Action.Call(), action);
        assertTrue(buffer.toString().contains("fold — сбросить"));
    }

    @Test
    void endOfInputIsReported() {
        Table t = startedTable();
        assertThrows(IllegalStateException.class, () -> bot("").decide(t, t.currentPlayer()));
    }

    @Test
    void rejectionIsShownToThePlayer() {
        bot("").rejected("минимальное повышение до 40");
        assertTrue(buffer.toString().contains("Так нельзя: минимальное повышение до 40"));
    }

    @Test
    void humanIsInteractive() {
        assertTrue(bot("").isInteractive());
    }
}

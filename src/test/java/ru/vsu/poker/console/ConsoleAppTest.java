package ru.vsu.poker.console;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ConsoleAppTest {

    private record Result(int code, String output) {
    }

    private static Result run(String input, String... args) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int code = ConsoleApp.run(args, new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(buffer, true, StandardCharsets.UTF_8));
        return new Result(code, buffer.toString(StandardCharsets.UTF_8));
    }

    @Test
    void simulationPrintsLogAndSummary() {
        Result r = run("", "--seed", "1", "--hands", "2");
        assertEquals(0, r.code());
        assertTrue(r.output().contains("зерно: 1"));
        assertTrue(r.output().contains("=== Раздача №1"));
        assertTrue(r.output().contains("Раздача №2 окончена"));
        assertTrue(r.output().contains("Итоги после 2 раздач"));
    }

    @Test
    void sameSeedGivesSameOutputAndDifferentSeedDoesNot() {
        assertEquals(run("", "--seed", "7", "--hands", "3").output(), run("", "--seed", "7", "--hands", "3").output());
        assertNotEquals(run("", "--seed", "7", "--hands", "3").output(), run("", "--seed", "8", "--hands", "3").output());
    }

    @Test
    void defaultsAreUsedWithoutArguments() {
        Result r = run("");
        assertEquals(0, r.code());
        assertTrue(r.output().contains("Игроков: 4"));
        assertTrue(r.output().contains("зерно: 42"));
    }

    @Test
    void simulationShowsEveryonesCards() {
        assertFalse(run("", "--seed", "1", "--hands", "1").output().contains("закрытые карты"));
    }

    @Test
    void badArgumentsGiveUsageAndExitCodeTwo() {
        for (String[] args : new String[][] {
                {"--hands", "0"}, {"--players", "1"}, {"--players", "10"}, {"--seed", "abc"},
                {"--seed"}, {"--stack", "-5"}, {"--bogus"}}) {
            Result r = run("", args);
            assertEquals(2, r.code(), String.join(" ", args));
            assertTrue(r.output().contains("Ошибка:"), String.join(" ", args));
            assertTrue(r.output().contains("Использование"), String.join(" ", args));
        }
    }

    @Test
    void helpPrintsUsage() {
        Result r = run("", "--help");
        assertEquals(0, r.code());
        assertTrue(r.output().contains("Использование"));
    }

    @Test
    void humanSeesOnlyOwnCardsAndCanFold() {
        Result r = run("fold\n", "--human", "--players", "2", "--hands", "1", "--seed", "3");
        assertEquals(0, r.code());
        assertTrue(r.output().contains("Ваш ход"));
        assertTrue(r.output().contains("Игрок: fold"));
        assertTrue(r.output().contains("Борис получает закрытые карты"));
    }

    @Test
    void humanMistakeIsExplainedAndAskedAgain() {
        // Вдвоём человек — кнопка: ему нужно доплатить 10, check нельзя; ввод «dance» непонятен
        Result r = run("dance\nraise 25\nfold\n", "--human", "--players", "2", "--hands", "1", "--seed", "3");
        assertEquals(0, r.code());
        assertTrue(r.output().contains("Не понял команду"));
        assertTrue(r.output().contains("Так нельзя: Минимальное повышение — до 40"));
    }

    @Test
    void endOfInputStopsTheGameWithCodeOne() {
        Result r = run("", "--human", "--players", "2", "--hands", "1");
        assertEquals(1, r.code());
        assertTrue(r.output().contains("Ввод закончился"));
    }
}

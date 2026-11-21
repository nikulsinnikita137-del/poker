package ru.vsu.poker.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.stream.Collectors;
import ru.vsu.poker.action.Action;
import ru.vsu.poker.bot.Bot;
import ru.vsu.poker.card.Card;
import ru.vsu.poker.game.Player;
import ru.vsu.poker.game.Table;

/** Человек за консолью: показывает ситуацию, читает команду и превращает её в действие. */
public final class HumanBot implements Bot {
    private final BufferedReader in;
    private final PrintStream out;

    /**
     * @param in  откуда читать команды
     * @param out куда выводить подсказки
     */
    public HumanBot(BufferedReader in, PrintStream out) {
        this.in = in;
        this.out = out;
    }

    @Override
    public Action decide(Table table, Player me) {
        out.println("Ваш ход. Карты: " + cards(me.holeCards().stream().toList())
                + " | стол: " + (table.community().isEmpty() ? "-" : cards(table.community()))
                + " | банк: " + table.pot() + " | ваш стек: " + me.stack()
                + " | уравнять: " + table.amountToCall(me)
                + " | минимальное повышение до: " + table.minRaiseTo());
        out.println("Команды: fold, check/call (c), raise N (r N), allin (a), help");
        while (true) {
            out.print("> ");
            out.flush();
            String line = readLine();
            try {
                Action action = parse(line, table.amountToCall(me) == 0);
                if (action != null) {
                    return action;
                }
                out.println("fold — сбросить, c — check или call, r 60 — повысить до 60, a — ва-банк");
            } catch (IllegalArgumentException e) {
                out.println("Не понял команду: " + e.getMessage());
            }
        }
    }

    @Override
    public void rejected(String reason) {
        out.println("Так нельзя: " + reason);
    }

    @Override
    public boolean isInteractive() {
        return true;
    }

    /**
     * Разбирает команду игрока.
     *
     * @param line      введённая строка
     * @param nothingToCall true, если уравнивать нечего (тогда «c» означает check)
     * @return действие или null, если введена команда help
     * @throws IllegalArgumentException если команда непонятна
     */
    static Action parse(String line, boolean nothingToCall) {
        String[] words = line.trim().toLowerCase(Locale.ROOT).split("\\s+");
        switch (words[0]) {
            case "fold", "f":
                return new Action.Fold();
            case "check", "call", "c", "x":
                return nothingToCall ? new Action.Check() : new Action.Call();
            case "allin", "all-in", "a":
                return new Action.AllIn();
            case "raise", "r":
                if (words.length < 2) {
                    throw new IllegalArgumentException("после raise нужна сумма, например: raise 60");
                }
                try {
                    return new Action.Raise(Integer.parseInt(words[1]));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("сумма должна быть целым числом: " + words[1]);
                }
            case "help", "?":
                return null;
            default:
                throw new IllegalArgumentException("«" + line.trim() + "»; введите help для списка команд");
        }
    }

    private String readLine() {
        try {
            String line = in.readLine();
            if (line == null) {
                throw new IllegalStateException("Ввод закончился");
            }
            return line;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String cards(java.util.List<Card> cards) {
        return cards.stream().map(Card::toString).collect(Collectors.joining(" "));
    }
}

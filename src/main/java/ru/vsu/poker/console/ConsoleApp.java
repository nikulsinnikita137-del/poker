package ru.vsu.poker.console;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import ru.vsu.poker.bot.Bot;
import ru.vsu.poker.bot.CallingStationBot;
import ru.vsu.poker.bot.RandomBot;
import ru.vsu.poker.game.Blinds;
import ru.vsu.poker.game.Player;
import ru.vsu.poker.game.Position;
import ru.vsu.poker.game.Table;
import ru.vsu.poker.sim.GameRunner;

/**
 * Консольное приложение: прогон партии с журналом. Играют встроенные боты, а при ключе {@code --human}
 * на месте 0 сидит человек.
 *
 * <p>Ключи: {@code --seed N} (зерно, по умолчанию 42), {@code --hands N} (число раздач, по умолчанию 5),
 * {@code --players N} (от 2 до 9, по умолчанию 4), {@code --stack N} (стартовый стек, по умолчанию 1000),
 * {@code --human}, {@code --help}.
 */
public final class ConsoleApp {
    private static final String[] NAMES =
            {"Анна", "Борис", "Вика", "Глеб", "Дима", "Егор", "Жанна", "Зоя", "Игорь"};
    private static final Blinds BLINDS = new Blinds(10, 20);

    private ConsoleApp() {
    }

    /**
     * Точка входа.
     *
     * @param args ключи командной строки
     */
    public static void main(String[] args) {
        System.exit(run(args, System.in, System.out));
    }

    /**
     * Запускает приложение; вынесено из {@code main}, чтобы его можно было проверять тестами.
     *
     * @param args ключи командной строки
     * @param in   откуда читать команды человека
     * @param out  куда писать журнал
     * @return код возврата: 0 — успех, 1 — ввод закончился, 2 — неверные ключи
     */
    public static int run(String[] args, InputStream in, PrintStream out) {
        Options options;
        try {
            options = Options.parse(args);
        } catch (IllegalArgumentException e) {
            out.println("Ошибка: " + e.getMessage());
            out.println(Options.USAGE);
            return 2;
        }
        if (options.help) {
            out.println(Options.USAGE);
            return 0;
        }

        // Единственный генератор на всё: колода и решения ботов берутся из него
        Random random = new Random(options.seed);
        List<Player> players = new ArrayList<>();
        List<Bot> bots = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        for (int seat = 0; seat < options.players; seat++) {
            boolean human = options.human && seat == 0;
            players.add(new Player(seat, human ? "Игрок" : NAMES[seat], options.stack));
            if (human) {
                bots.add(new HumanBot(reader, out));
            } else {
                bots.add(seat % 2 == 0 ? new RandomBot(random) : new CallingStationBot());
            }
        }

        Table table = new Table(players, BLINDS, random);
        // Человеку показываем только его карты; в симуляции журнал открытый
        Set<Integer> visible = options.human ? Set.of(0)
                : players.stream().map(Player::seat).collect(java.util.stream.Collectors.toSet());
        EventFormatter formatter = new EventFormatter(players, visible);
        GameRunner runner = new GameRunner(table, bots, event -> formatter.format(event).forEach(out::println));

        out.println("Техасский холдем. Игроков: " + options.players + ", стек: " + options.stack
                + ", блайнды: " + BLINDS.smallBlind() + "/" + BLINDS.bigBlind() + ", зерно: " + options.seed);
        int played;
        try {
            played = runner.playHands(options.hands);
        } catch (IllegalStateException e) {
            out.println(e.getMessage());
            return 1;
        }

        out.println();
        out.println("Итоги после " + played + " раздач:");
        players.stream()
                .sorted(Comparator.comparingInt(Player::stack).reversed())
                .forEach(p -> out.println("  " + p.name() + ": " + p.stack()));
        if (played < options.hands) {
            out.println("Игра закончилась раньше: остался один игрок с фишками.");
        }
        return 0;
    }

    /** Разобранные ключи командной строки. */
    private static final class Options {
        static final String USAGE = """
                Использование: ConsoleApp [--seed N] [--hands N] [--players 2..9] [--stack N] [--human]
                  --seed N      зерно генератора (одно зерно — одна и та же партия), по умолчанию 42
                  --hands N     сколько раздач сыграть, по умолчанию 5
                  --players N   число игроков от 2 до 9, по умолчанию 4
                  --stack N     стартовый стек, по умолчанию 1000
                  --human       вы играете за место 0 (команды вводятся в консоли)""";

        long seed = 42;
        int hands = 5;
        int players = 4;
        int stack = 1000;
        boolean human;
        boolean help;

        static Options parse(String[] args) {
            Options o = new Options();
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--seed" -> o.seed = number(args, ++i, "--seed");
                    case "--hands" -> o.hands = (int) bounded(args, ++i, "--hands", 1, 100_000);
                    case "--players" -> o.players =
                            (int) bounded(args, ++i, "--players", Position.MIN_PLAYERS, NAMES.length);
                    case "--stack" -> o.stack = (int) bounded(args, ++i, "--stack", 1, 1_000_000);
                    case "--human" -> o.human = true;
                    case "--help", "-h" -> o.help = true;
                    default -> throw new IllegalArgumentException("неизвестный ключ: " + args[i]);
                }
            }
            return o;
        }

        private static long number(String[] args, int index, String key) {
            if (index >= args.length) {
                throw new IllegalArgumentException("после " + key + " нужно число");
            }
            try {
                return Long.parseLong(args[index]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(key + ": «" + args[index] + "» — не число");
            }
        }

        private static long bounded(String[] args, int index, String key, long min, long max) {
            long value = number(args, index, key);
            if (value < min || value > max) {
                throw new IllegalArgumentException(key + " должно быть от " + min + " до " + max);
            }
            return value;
        }
    }
}

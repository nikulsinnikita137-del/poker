package ru.vsu.poker.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import ru.vsu.poker.card.Card;
import ru.vsu.poker.card.Deck;
import ru.vsu.poker.event.GameEvent;
import ru.vsu.poker.event.GameEvent.BlindKind;

/**
 * Стол для игры в техасский холдем. Хранит игроков, кнопку, колоду и общие карты.
 * Ничего не печатает: каждый шаг возвращает список событий.
 *
 * <p>Места нумеруются по часовой стрелке с нуля, номер места равен индексу игрока в списке.
 */
public final class Table {
    private final List<Player> players;
    private final Blinds blinds;
    private final Random random;
    private final List<Card> community = new ArrayList<>(5);
    private final List<Player> inHandFromButton = new ArrayList<>();
    private List<Position> layout = List.of();
    private Deck deck;
    private int button = -1;
    private int handNumber = 0;

    /**
     * Создаёт стол.
     *
     * @param players игроки; место каждого должно совпадать с его индексом в списке
     * @param blinds  размеры блайндов
     * @param random  единственный источник случайности (из него тасуется колода)
     * @throws IllegalArgumentException если игроков меньше двух или больше десяти либо места не по порядку
     */
    public Table(List<Player> players, Blinds blinds, Random random) {
        if (players.size() < Position.MIN_PLAYERS || players.size() > Position.MAX_PLAYERS) {
            throw new IllegalArgumentException("За столом должно быть от " + Position.MIN_PLAYERS
                    + " до " + Position.MAX_PLAYERS + " игроков: " + players.size());
        }
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).seat() != i) {
                throw new IllegalArgumentException(
                        "Место игрока " + players.get(i) + " должно быть равно " + i);
            }
        }
        this.players = List.copyOf(players);
        this.blinds = Objects.requireNonNull(blinds, "blinds");
        this.random = Objects.requireNonNull(random, "random");
    }

    /**
     * Начинает новую раздачу: двигает кнопку, ставит блайнды, раздаёт по две карты.
     * Игроки без фишек в раздаче не участвуют, кнопка их перепрыгивает.
     * Банк предыдущей раздачи к этому моменту должен быть выдан победителям
     * (вскрытие добавляется отдельным шагом): вклады обнуляются.
     *
     * @return события раздачи по порядку
     * @throws IllegalStateException если фишки есть меньше чем у двух игроков
     */
    public List<GameEvent> startHand() {
        long funded = players.stream().filter(p -> p.stack() > 0).count();
        if (funded < Position.MIN_PLAYERS) {
            throw new IllegalStateException("Для раздачи нужно минимум два игрока с фишками");
        }

        handNumber++;
        button = nextFundedSeat(button);
        players.forEach(Player::startHand);
        community.clear();
        deck = new Deck(random);

        // Игроки раздачи, начиная с кнопки по часовой стрелке
        inHandFromButton.clear();
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get((button + i) % players.size());
            if (p.isInHand()) {
                inHandFromButton.add(p);
            }
        }
        layout = Position.layout(inHandFromButton.size());

        List<GameEvent> events = new ArrayList<>();
        events.add(new GameEvent.HandStarted(handNumber, button,
                inHandFromButton.stream().map(Player::seat).toList()));

        // Вдвоём кнопка ставит малый блайнд, иначе малый блайнд — следующий за кнопкой
        boolean headsUp = inHandFromButton.size() == 2;
        Player smallBlind = headsUp ? inHandFromButton.get(0) : inHandFromButton.get(1);
        Player bigBlind = headsUp ? inHandFromButton.get(1) : inHandFromButton.get(2);
        events.add(new GameEvent.BlindPosted(smallBlind.seat(), BlindKind.SMALL,
                smallBlind.pay(blinds.smallBlind())));
        events.add(new GameEvent.BlindPosted(bigBlind.seat(), BlindKind.BIG,
                bigBlind.pay(blinds.bigBlind())));

        // Сдача идёт с первого игрока слева от кнопки: две карты по кругу
        List<Player> dealingOrder = new ArrayList<>(inHandFromButton);
        Collections.rotate(dealingOrder, -1);
        for (int round = 0; round < 2; round++) {
            for (Player p : dealingOrder) {
                p.receive(deck.draw());
            }
        }
        for (Player p : dealingOrder) {
            events.add(new GameEvent.HoleCardsDealt(p.seat(), p.holeCards()));
        }
        return List.copyOf(events);
    }

    /** Ищет следующее после {@code from} место с фишками (по кругу). */
    private int nextFundedSeat(int from) {
        for (int i = 1; i <= players.size(); i++) {
            int seat = Math.floorMod(from + i, players.size());
            if (players.get(seat).stack() > 0) {
                return seat;
            }
        }
        throw new IllegalStateException("Нет игроков с фишками");
    }

    /**
     * Позиция игрока в текущей раздаче.
     *
     * @param player игрок
     * @return позиция
     * @throws IllegalArgumentException если игрок не участвует в раздаче
     */
    public Position positionOf(Player player) {
        int index = inHandFromButton.indexOf(player);
        if (index < 0) {
            throw new IllegalArgumentException("Игрок не участвует в раздаче: " + player);
        }
        return layout.get(index);
    }

    /** @return игроки (только для чтения), индекс равен месту */
    public List<Player> players() {
        return players;
    }

    /** @return игроки текущей раздачи, начиная с кнопки по часовой стрелке */
    public List<Player> playersInHandFromButton() {
        return Collections.unmodifiableList(inHandFromButton);
    }

    /** @return общие карты (пока пусто) */
    public List<Card> community() {
        return Collections.unmodifiableList(community);
    }

    /** @return размеры блайндов */
    public Blinds blinds() {
        return blinds;
    }

    /** @return место кнопки или -1, если раздача ещё не начиналась */
    public int button() {
        return button;
    }

    /** @return номер текущей раздачи (0 — ещё не начиналась) */
    public int handNumber() {
        return handNumber;
    }

    /** @return сколько фишек сейчас в банке (сумма вкладов всех игроков раздачи) */
    public int pot() {
        return players.stream().mapToInt(Player::contributed).sum();
    }
}

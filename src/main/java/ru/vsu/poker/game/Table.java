package ru.vsu.poker.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import ru.vsu.poker.action.Action;
import ru.vsu.poker.card.Card;
import ru.vsu.poker.card.Deck;
import ru.vsu.poker.event.GameEvent;
import ru.vsu.poker.event.GameEvent.BlindKind;
import ru.vsu.poker.phase.HandPhase;
import ru.vsu.poker.phase.PreFlopPhase;
import ru.vsu.poker.phase.ShowdownPhase;

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
    private HandPhase phase;
    private BettingRound round;
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

        // Начинается префлоп: ставку нужно уравнять до большого блайнда
        phase = new PreFlopPhase();
        round = new BettingRound(orderFrom(phase.firstToActIndex(inHandFromButton.size())),
                blinds.bigBlind(), blinds.bigBlind());
        events.addAll(settle());
        return List.copyOf(events);
    }

    /**
     * Выполняет действие игрока, чей сейчас ход, и продвигает раздачу: если круг торговли закончился,
     * открывает следующие карты и начинает новый круг. Если остался один игрок или торговаться больше
     * не с кем, раздача доходит до вскрытия.
     *
     * @param action действие
     * @return события: ход игрока, открытые карты и т. д.
     * @throws ru.vsu.poker.action.IllegalActionException если действие нарушает правила (состояние не меняется)
     * @throws IllegalStateException                      если раздача не идёт
     */
    public List<GameEvent> act(Action action) {
        Objects.requireNonNull(action, "action");
        requireHandInProgress();
        List<GameEvent> events = new ArrayList<>(round.apply(round.currentPlayer(), action));
        events.addAll(settle());
        return List.copyOf(events);
    }

    /**
     * Переводит раздачу вперёд, пока торговля не потребует хода игрока. Круг, в котором торговаться
     * не с кем (все поставили всё), проходится сразу: общие карты открываются до конца.
     */
    private List<GameEvent> settle() {
        List<GameEvent> events = new ArrayList<>();
        while (!(phase instanceof ShowdownPhase)) {
            if (liveCount() == 1) {
                phase = new ShowdownPhase(); // все остальные сбросили карты
                break;
            }
            if (!round.isComplete()) {
                return events;
            }
            phase = phase.next();
            if (phase instanceof ShowdownPhase) {
                break;
            }
            List<Card> dealt = new ArrayList<>();
            for (int i = 0; i < phase.boardCardsToDeal(); i++) {
                dealt.add(deck.draw());
            }
            community.addAll(dealt);
            events.add(new GameEvent.BoardDealt(phase.name(), dealt, community));
            players.forEach(Player::startBettingRound);
            round = new BettingRound(orderFrom(phase.firstToActIndex(inHandFromButton.size())),
                    0, blinds.bigBlind());
        }
        events.addAll(onShowdown());
        return events;
    }

    /** Вскрытие. Определение победителей и выдача банков добавляются отдельным шагом. */
    private List<GameEvent> onShowdown() {
        return List.of();
    }

    /** @return игроки раздачи в порядке хода: список от кнопки, повёрнутый так, что первым стоит {@code index}-й */
    private List<Player> orderFrom(int index) {
        List<Player> order = new ArrayList<>(inHandFromButton);
        Collections.rotate(order, -index);
        return order;
    }

    /** @return сколько игроков раздачи ещё не сбросили карты */
    private int liveCount() {
        return (int) inHandFromButton.stream().filter(p -> !p.isFolded()).count();
    }

    private void requireHandInProgress() {
        if (phase == null || phase instanceof ShowdownPhase) {
            throw new IllegalStateException("Раздача не идёт");
        }
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

    /**
     * @return игрок, чей сейчас ход
     * @throws IllegalStateException если раздача не идёт
     */
    public Player currentPlayer() {
        requireHandInProgress();
        return round.currentPlayer();
    }

    /**
     * @param player игрок
     * @return сколько фишек ему нужно доплатить, чтобы уравнять ставку круга
     * @throws IllegalStateException если раздача не идёт
     */
    public int amountToCall(Player player) {
        requireHandInProgress();
        return round.amountToCall(player);
    }

    /**
     * @return минимальный размер ставки, до которого сейчас можно повысить
     * @throws IllegalStateException если раздача не идёт
     */
    public int minRaiseTo() {
        requireHandInProgress();
        return round.minRaiseTo();
    }

    /**
     * @return ставка, которую нужно уравнять в текущем круге
     * @throws IllegalStateException если раздача не идёт
     */
    public int currentBet() {
        requireHandInProgress();
        return round.currentBet();
    }

    /** @return текущая фаза раздачи или null, если раздача ещё не начиналась */
    public HandPhase phase() {
        return phase;
    }

    /** @return true, если раздача дошла до вскрытия (торговля закончена) */
    public boolean isHandOver() {
        return phase instanceof ShowdownPhase;
    }

    /** @return общие карты на столе (0, 3, 4 или 5) */
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

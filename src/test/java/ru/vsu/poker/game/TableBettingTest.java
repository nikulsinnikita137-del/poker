package ru.vsu.poker.game;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import ru.vsu.poker.action.Action;
import ru.vsu.poker.action.IllegalActionException;
import ru.vsu.poker.event.GameEvent;
import ru.vsu.poker.phase.FlopPhase;
import ru.vsu.poker.phase.PreFlopPhase;
import ru.vsu.poker.phase.RiverPhase;
import ru.vsu.poker.phase.ShowdownPhase;
import ru.vsu.poker.phase.TurnPhase;

/** Торговля: блайнды 10/20, на первой раздаче кнопка на месте 0. */
class TableBettingTest {

    private static Table withStacks(int... stacks) {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < stacks.length; i++) {
            players.add(new Player(i, "P" + i, stacks[i]));
        }
        Table t = new Table(players, new Blinds(10, 20), new Random(1));
        t.startHand();
        return t;
    }

    private static Table started(int count, int stack) {
        int[] stacks = new int[count];
        java.util.Arrays.fill(stacks, stack);
        return withStacks(stacks);
    }

    private static List<GameEvent> callOrCheck(Table t) {
        Player p = t.currentPlayer();
        return t.act(t.amountToCall(p) == 0 ? new Action.Check() : new Action.Call());
    }

    private static int seat(Table t) {
        return t.currentPlayer().seat();
    }

    // ---------- очерёдность ходов ----------

    @Test
    void preFlopStartsLeftOfBigBlind() {
        assertEquals(3, seat(started(4, 1000)));
    }

    @Test
    void threePlayersButtonActsFirstPreFlop() {
        assertEquals(0, seat(started(3, 1000)));
    }

    @Test
    void headsUpButtonActsFirstPreFlopAndBigBlindFirstAfterFlop() {
        Table t = started(2, 1000);
        assertEquals(0, seat(t));
        t.act(new Action.Call());
        t.act(new Action.Check());
        assertTrue(t.phase() instanceof FlopPhase);
        assertEquals(1, seat(t));
    }

    @Test
    void limpAroundMovesToFlopWithThreeCards() {
        Table t = started(4, 1000);
        t.act(new Action.Call());   // место 3
        t.act(new Action.Call());   // кнопка
        t.act(new Action.Call());   // малый блайнд
        List<GameEvent> events = t.act(new Action.Check()); // большой блайнд
        assertTrue(t.phase() instanceof FlopPhase);
        assertEquals(3, t.community().size());
        assertEquals(1, seat(t));
        GameEvent.BoardDealt dealt = (GameEvent.BoardDealt) events.get(1);
        assertEquals("Флоп", dealt.phase());
        assertEquals(3, dealt.newCards().size());
    }

    @Test
    void bigBlindGetsOptionAfterLimps() {
        Table t = started(4, 1000);
        t.act(new Action.Call());
        t.act(new Action.Call());
        t.act(new Action.Call());
        assertTrue(t.phase() instanceof PreFlopPhase, "круг не закончен, пока не сходил большой блайнд");
        assertEquals(2, seat(t));
        t.act(new Action.Raise(40));
        assertEquals(40, t.currentBet());
    }

    @Test
    void foldedPlayersAreSkipped() {
        Table t = started(4, 1000);
        t.act(new Action.Fold());   // место 3
        t.act(new Action.Call());
        t.act(new Action.Call());
        t.act(new Action.Check());
        List<Integer> order = new ArrayList<>();
        while (t.phase() instanceof FlopPhase) {
            order.add(seat(t));
            callOrCheck(t);
        }
        assertEquals(List.of(1, 2, 0), order);
    }

    // ---------- недопустимые ходы ----------

    @Test
    void checkIsIllegalWhenFacingBet() {
        Table t = started(4, 1000);
        assertThrows(IllegalActionException.class, () -> t.act(new Action.Check()));
        assertEquals(3, seat(t), "после ошибки ход остаётся у того же игрока");
        assertEquals(0, t.players().get(3).contributed());
    }

    @Test
    void callIsIllegalWhenNothingToCall() {
        Table t = started(4, 1000);
        t.act(new Action.Call());
        t.act(new Action.Call());
        t.act(new Action.Call());
        assertThrows(IllegalActionException.class, () -> t.act(new Action.Call()));
    }

    @Test
    void raiseBelowMinimumIsRejectedAndExactMinimumAccepted() {
        Table t = started(4, 1000);
        assertEquals(40, t.minRaiseTo());
        assertThrows(IllegalActionException.class, () -> t.act(new Action.Raise(30)));
        t.act(new Action.Raise(40));
        assertEquals(40, t.currentBet());
    }

    @Test
    void minimumRaiseEqualsSizeOfPreviousRaise() {
        Table t = started(4, 1000);
        t.act(new Action.Raise(60));        // повышение на 40
        assertEquals(100, t.minRaiseTo());
        assertThrows(IllegalActionException.class, () -> t.act(new Action.Raise(90)));
        t.act(new Action.Raise(100));
        assertEquals(140, t.minRaiseTo());
    }

    @Test
    void raiseBeyondStackIsRejectedButAllInIsAllowed() {
        Table t = withStacks(1000, 1000, 1000, 50);
        assertThrows(IllegalActionException.class, () -> t.act(new Action.Raise(100)));
        t.act(new Action.AllIn());
        assertTrue(t.players().get(3).isAllIn());
        assertEquals(50, t.currentBet());
    }

    @Test
    void postFlopMinimumBetIsBigBlind() {
        Table t = started(4, 1000);
        for (int i = 0; i < 4; i++) {
            callOrCheck(t);
        }
        assertEquals(0, t.currentBet());
        assertEquals(20, t.minRaiseTo());
        assertThrows(IllegalActionException.class, () -> t.act(new Action.Raise(10)));
        t.act(new Action.Raise(20));
        assertEquals(20, t.currentBet());
    }

    @Test
    void raiseRecordRejectsNonPositiveSize() {
        assertThrows(IllegalArgumentException.class, () -> new Action.Raise(0));
    }

    @Test
    void wrongPlayerCannotActInBettingRound() {
        Player a = new Player(0, "A", 100);
        Player b = new Player(1, "B", 100);
        a.startHand();
        b.startHand();
        BettingRound round = new BettingRound(List.of(a, b), 0, 20);
        assertThrows(IllegalActionException.class, () -> round.apply(b, new Action.Check()));
        round.apply(a, new Action.Check());
        assertEquals(b, round.currentPlayer());
    }

    @Test
    void actWithoutRunningHandThrows() {
        Table fresh = new Table(List.of(new Player(0, "A", 100), new Player(1, "B", 100)),
                new Blinds(10, 20), new Random(1));
        assertThrows(IllegalStateException.class, () -> fresh.act(new Action.Fold()));
        assertThrows(IllegalStateException.class, fresh::currentPlayer);
    }

    // ---------- повышения и очередь ----------

    @Test
    void raiseSendsEveryoneElseBackToAct() {
        Table t = started(3, 1000);
        t.act(new Action.Raise(60));   // место 0
        t.act(new Action.Raise(100));  // место 1 перебивает
        t.act(new Action.Call());      // место 2
        assertTrue(t.phase() instanceof PreFlopPhase, "первый повысивший должен ответить на ре-рейз");
        assertEquals(0, seat(t));
        t.act(new Action.Call());
        assertTrue(t.phase() instanceof FlopPhase);
    }

    @Test
    void raiseCalledByEveryoneEndsTheRound() {
        Table t = started(3, 1000);
        t.act(new Action.Raise(60));
        t.act(new Action.Call());
        t.act(new Action.Call());
        assertTrue(t.phase() instanceof FlopPhase);
        assertEquals(180, t.pot());
    }

    // ---------- all-in ----------

    @Test
    void callForLessMakesPlayerAllIn() {
        Table t = withStacks(1000, 1000, 50);
        t.act(new Action.Raise(100));  // место 0
        t.act(new Action.Fold());      // малый блайнд
        t.act(new Action.Call());      // большой блайнд: осталось 30 из 80
        Player bb = t.players().get(2);
        assertTrue(bb.isAllIn());
        assertEquals(50, bb.contributed());
        // торговаться больше не с кем: общие карты открыты до конца
        assertTrue(t.isHandOver());
        assertEquals(5, t.community().size());
    }

    @Test
    void shortAllInRaiseDoesNotLetPlayersWhoAlreadyActedRaise() {
        Table t = withStacks(1000, 1000, 30);
        t.act(new Action.Call());      // место 0
        t.act(new Action.Call());      // малый блайнд доплачивает 10
        t.act(new Action.AllIn());     // большой блайнд: всего 30, это повышение лишь на 10 (< 20)
        assertEquals(30, t.currentBet());
        assertEquals(0, seat(t));
        assertThrows(IllegalActionException.class, () -> t.act(new Action.Raise(100)));
        assertThrows(IllegalActionException.class, () -> t.act(new Action.AllIn()));
        t.act(new Action.Call());
        t.act(new Action.Call());
        assertTrue(t.phase() instanceof FlopPhase);
    }

    @Test
    void fullAllInRaiseReopensBetting() {
        Table t = withStacks(1000, 1000, 50);
        t.act(new Action.Call());
        t.act(new Action.Call());
        t.act(new Action.AllIn());     // всего 50: повышение на 30 (>= 20) — полное
        assertEquals(50, t.currentBet());
        assertEquals(80, t.minRaiseTo());
        t.act(new Action.Raise(80));
        assertEquals(80, t.currentBet());
    }

    @Test
    void everyoneAllInRunsBoardOutImmediately() {
        Table t = withStacks(100, 100);
        t.act(new Action.AllIn());
        List<GameEvent> events = t.act(new Action.Call());
        assertTrue(t.isHandOver());
        assertEquals(5, t.community().size());
        List<String> phases = events.stream()
                .filter(e -> e instanceof GameEvent.BoardDealt)
                .map(e -> ((GameEvent.BoardDealt) e).phase())
                .toList();
        assertEquals(List.of("Флоп", "Тёрн", "Ривер"), phases);
    }

    @Test
    void blindsThatPutPlayerAllInSkipBettingFromTheStart() {
        // Малый блайнд (кнопка) всего с 10 фишками, у большого блайнда — много: торговаться некому
        Table t = withStacks(10, 1000);
        assertTrue(t.isHandOver());
        assertEquals(5, t.community().size());
    }

    // ---------- завершение раздачи ----------

    @Test
    void everyoneFoldingToOnePlayerEndsHandWithoutBoard() {
        Table t = started(3, 1000);
        t.act(new Action.Fold());
        List<GameEvent> events = t.act(new Action.Fold());
        assertTrue(t.isHandOver());
        assertTrue(t.phase() instanceof ShowdownPhase);
        assertTrue(t.community().isEmpty());
        assertEquals(1, events.stream().filter(e -> e instanceof GameEvent.PlayerActed).count());
        assertThrows(IllegalStateException.class, () -> t.act(new Action.Check()));
    }

    @Test
    void fullHandGoesThroughAllPhases() {
        Table t = started(3, 1000);
        List<Integer> boardSizes = new ArrayList<>();
        boardSizes.add(t.community().size());
        Class<?> last = t.phase().getClass();
        while (!t.isHandOver()) {
            callOrCheck(t);
            if (!t.phase().getClass().equals(last)) {
                last = t.phase().getClass();
                boardSizes.add(t.community().size());
            }
        }
        assertEquals(List.of(0, 3, 4, 5, 5), boardSizes);
        assertTrue(java.util.List.of(FlopPhase.class, TurnPhase.class, RiverPhase.class, ShowdownPhase.class)
                .contains(t.phase().getClass()));
    }

    // ---------- события, фишки, вывод ----------

    @Test
    void actionProducesPlayerActedEvent() {
        Table t = started(4, 1000);
        List<GameEvent> events = t.act(new Action.Call());
        assertEquals(new GameEvent.PlayerActed(3, new Action.Call(), 20), events.get(0));
    }

    @Test
    void bettingValuesAtStartOfPreFlop() {
        Table t = started(4, 1000);
        assertEquals(20, t.currentBet());
        assertEquals(20, t.amountToCall(t.players().get(3)));
        assertEquals(10, t.amountToCall(t.players().get(1)));
        assertEquals(0, t.amountToCall(t.players().get(2)));
    }

    @Test
    void chipsAreConservedDuringBetting() {
        Table t = withStacks(500, 700, 900, 1100);
        int total = 3200;
        t.act(new Action.Raise(60));
        t.act(new Action.Call());
        t.act(new Action.Raise(200));
        t.act(new Action.Fold());
        assertEquals(total, t.players().stream().mapToInt(Player::stack).sum() + t.pot());
    }

    @Test
    void bettingNeverWritesToConsole() {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer));
        try {
            Table t = started(3, 1000);
            t.act(new Action.Raise(60));
            t.act(new Action.Fold());
            t.act(new Action.Call());
            while (!t.isHandOver()) {
                callOrCheck(t);
            }
        } finally {
            System.setOut(original);
        }
        assertEquals(0, buffer.size());
    }
}

package ru.vsu.poker.event;

import java.util.List;
import ru.vsu.poker.action.Action;
import ru.vsu.poker.card.Card;
import ru.vsu.poker.eval.HandCategory;

/**
 * События партии. Механика возвращает их списком, а вывод (консоль, лог, тесты) решает, что с ними делать.
 * Так правила не зависят от вывода.
 */
public sealed interface GameEvent {

    /** Вид блайнда. */
    enum BlindKind { SMALL, BIG }

    /**
     * Началась новая раздача.
     *
     * @param handNumber номер раздачи (с единицы)
     * @param button     место кнопки дилера
     * @param seats      места игроков раздачи, начиная с кнопки по часовой стрелке
     */
    record HandStarted(int handNumber, int button, List<Integer> seats) implements GameEvent {
        public HandStarted {
            seats = List.copyOf(seats);
        }
    }

    /**
     * Игрок поставил блайнд.
     *
     * @param seat   место игрока
     * @param kind   малый или большой
     * @param amount внесённая сумма (меньше размера блайнда, если не хватило фишек)
     */
    record BlindPosted(int seat, BlindKind kind, int amount) implements GameEvent {
    }

    /**
     * Игроку сданы карманные карты.
     *
     * @param seat  место игрока
     * @param cards две карты
     */
    record HoleCardsDealt(int seat, List<Card> cards) implements GameEvent {
        public HoleCardsDealt {
            cards = List.copyOf(cards);
        }
    }

    /**
     * Игрок сделал ход.
     *
     * @param seat   место игрока
     * @param action действие
     * @param paid   сколько фишек он при этом внёс в банк
     */
    record PlayerActed(int seat, Action action, int paid) implements GameEvent {
    }

    /**
     * Открыты общие карты нового круга.
     *
     * @param phase    название фазы («Флоп», «Тёрн», «Ривер»)
     * @param newCards только что открытые карты
     * @param board    все общие карты на столе
     */
    record BoardDealt(String phase, List<Card> newCards, List<Card> board) implements GameEvent {
        public BoardDealt {
            newCards = List.copyOf(newCards);
            board = List.copyOf(board);
        }
    }

    /**
     * Игрок открыл карты на вскрытии.
     *
     * @param seat      место игрока
     * @param holeCards его карманные карты
     * @param category  собранная комбинация
     * @param bestFive  пять карт комбинации
     */
    record ShowdownReveal(int seat, List<Card> holeCards, HandCategory category, List<Card> bestFive)
            implements GameEvent {
        public ShowdownReveal {
            holeCards = List.copyOf(holeCards);
            bestFive = List.copyOf(bestFive);
        }
    }

    /**
     * Игрок получил долю банка. Банк с единственным претендентом — это возврат его неуравненной ставки.
     *
     * @param potIndex номер банка (0 — основной, дальше побочные)
     * @param seat     место игрока
     * @param amount   сколько фишек получено
     */
    record PotAwarded(int potIndex, int seat, int amount) implements GameEvent {
    }

    /**
     * Раздача закончена, банки выданы.
     *
     * @param handNumber номер раздачи
     */
    record HandFinished(int handNumber) implements GameEvent {
    }
}

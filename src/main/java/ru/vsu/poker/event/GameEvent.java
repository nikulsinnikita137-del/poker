package ru.vsu.poker.event;

import java.util.List;
import ru.vsu.poker.card.Card;

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
}

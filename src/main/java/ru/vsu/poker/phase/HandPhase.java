package ru.vsu.poker.phase;

/**
 * Состояние раздачи (паттерн «Состояние»). Каждая фаза сама знает, сколько общих карт открыть,
 * кто ходит первым и какая фаза следующая, поэтому в столе нет цепочки {@code if/switch} по номеру круга.
 * Чтобы добавить новый круг, достаточно написать новый класс.
 */
public interface HandPhase {

    /** @return название фазы для журнала */
    String name();

    /** @return сколько общих карт открывается при входе в фазу */
    int boardCardsToDeal();

    /**
     * Индекс первого ходящего в списке игроков, упорядоченном начиная с кнопки.
     *
     * @param playersCount число игроков в раздаче
     * @return индекс (0 — кнопка, 1 — следующий за ней и т. д.)
     */
    int firstToActIndex(int playersCount);

    /**
     * @return следующая фаза
     * @throws IllegalStateException если фаза последняя
     */
    HandPhase next();

    /** @return true, если в этой фазе идёт торговля */
    default boolean isBetting() {
        return true;
    }
}

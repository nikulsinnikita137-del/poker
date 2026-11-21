package ru.vsu.poker.bot;

import ru.vsu.poker.action.Action;
import ru.vsu.poker.game.Player;
import ru.vsu.poker.game.Table;

/** Тот, кто принимает решения за игрока: встроенный бот или человек за консолью. */
public interface Bot {

    /**
     * Выбирает действие.
     *
     * @param table  стол (карты, ставки, банк)
     * @param player игрок, за которого нужно сходить
     * @return выбранное действие
     */
    Action decide(Table table, Player player);

    /**
     * Сообщает, что выбранное действие нарушило правила. Человек может прочитать причину и походить заново.
     *
     * @param reason причина отказа
     */
    default void rejected(String reason) {
    }

    /** @return true, если за ботом сидит человек: ему можно ошибаться сколько угодно раз */
    default boolean isInteractive() {
        return false;
    }
}

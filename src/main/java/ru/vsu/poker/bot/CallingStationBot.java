package ru.vsu.poker.bot;

import ru.vsu.poker.action.Action;
import ru.vsu.poker.game.Player;
import ru.vsu.poker.game.Table;

/** Самый простой бот: всегда уравнивает или пропускает ход, никогда не повышает и не сбрасывает. */
public final class CallingStationBot implements Bot {

    @Override
    public Action decide(Table table, Player player) {
        return table.amountToCall(player) == 0 ? new Action.Check() : new Action.Call();
    }
}

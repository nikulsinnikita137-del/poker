package ru.vsu.poker.bot;

import java.util.Random;
import ru.vsu.poker.action.Action;
import ru.vsu.poker.game.Player;
import ru.vsu.poker.game.Table;

/**
 * Бот со случайными решениями. Использует тот же {@link Random}, что и стол, поэтому вся партия
 * определяется одним зерном.
 */
public final class RandomBot implements Bot {
    private final Random random;

    /**
     * @param random общий генератор случайных чисел партии
     */
    public RandomBot(Random random) {
        this.random = random;
    }

    @Override
    public Action decide(Table table, Player me) {
        int roll = random.nextInt(100);
        boolean canRaise = table.minRaiseTo() - me.roundBet() <= me.stack();
        if (table.amountToCall(me) == 0) {
            if (roll < 70 || (roll < 95 && !canRaise)) {
                return new Action.Check();
            }
            return roll < 95 ? new Action.Raise(table.minRaiseTo()) : new Action.AllIn();
        }
        if (roll < 25) {
            return new Action.Fold();
        }
        if (roll < 80 || (roll < 95 && !canRaise)) {
            return new Action.Call();
        }
        return roll < 95 ? new Action.Raise(table.minRaiseTo()) : new Action.AllIn();
    }
}

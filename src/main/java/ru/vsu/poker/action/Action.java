package ru.vsu.poker.action;

/** Действие игрока в круге торговли. Неизменяемое значение. */
public sealed interface Action {

    /** Сбросить карты и выйти из раздачи. */
    record Fold() implements Action {
        @Override
        public String toString() {
            return "fold";
        }
    }

    /** Пропустить ход. Возможно, только если уравнивать нечего. */
    record Check() implements Action {
        @Override
        public String toString() {
            return "check";
        }
    }

    /** Уравнять текущую ставку (если фишек не хватает — идёт на всё, что есть). */
    record Call() implements Action {
        @Override
        public String toString() {
            return "call";
        }
    }

    /**
     * Повысить ставку.
     *
     * @param to общий размер ставки игрока в этом круге после повышения (не «на сколько», а «до скольки»)
     */
    record Raise(int to) implements Action {
        public Raise {
            if (to <= 0) {
                throw new IllegalArgumentException("Размер ставки должен быть положительным: " + to);
            }
        }

        @Override
        public String toString() {
            return "raise to " + to;
        }
    }

    /** Поставить все оставшиеся фишки (ва-банк). */
    record AllIn() implements Action {
        @Override
        public String toString() {
            return "all-in";
        }
    }
}

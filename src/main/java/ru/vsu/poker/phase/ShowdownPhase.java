package ru.vsu.poker.phase;

/** Вскрытие: торговли нет, банк делится между победителями. Конечная фаза раздачи. */
public final class ShowdownPhase implements HandPhase {

    @Override
    public String name() {
        return "Вскрытие";
    }

    @Override
    public int boardCardsToDeal() {
        return 0;
    }

    @Override
    public int firstToActIndex(int playersCount) {
        throw new UnsupportedOperationException("На вскрытии никто не ходит");
    }

    @Override
    public HandPhase next() {
        throw new IllegalStateException("Вскрытие — последняя фаза раздачи");
    }

    @Override
    public boolean isBetting() {
        return false;
    }
}

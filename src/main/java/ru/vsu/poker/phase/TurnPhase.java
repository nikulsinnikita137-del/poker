package ru.vsu.poker.phase;

/** Тёрн: открывается четвёртая общая карта. */
public final class TurnPhase implements HandPhase {

    @Override
    public String name() {
        return "Тёрн";
    }

    @Override
    public int boardCardsToDeal() {
        return 1;
    }

    @Override
    public int firstToActIndex(int playersCount) {
        return 1 % playersCount;
    }

    @Override
    public HandPhase next() {
        return new RiverPhase();
    }
}

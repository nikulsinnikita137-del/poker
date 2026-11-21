package ru.vsu.poker.phase;

/** Ривер: открывается пятая общая карта, последний круг торговли. */
public final class RiverPhase implements HandPhase {

    @Override
    public String name() {
        return "Ривер";
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
        return new ShowdownPhase();
    }
}

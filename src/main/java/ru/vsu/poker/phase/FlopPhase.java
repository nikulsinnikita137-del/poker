package ru.vsu.poker.phase;

/** Флоп: открываются три общие карты. */
public final class FlopPhase implements HandPhase {

    @Override
    public String name() {
        return "Флоп";
    }

    @Override
    public int boardCardsToDeal() {
        return 3;
    }

    /** После флопа первым ходит первый игрок слева от кнопки. */
    @Override
    public int firstToActIndex(int playersCount) {
        return 1 % playersCount;
    }

    @Override
    public HandPhase next() {
        return new TurnPhase();
    }
}

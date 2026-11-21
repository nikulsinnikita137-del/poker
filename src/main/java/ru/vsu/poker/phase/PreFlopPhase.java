package ru.vsu.poker.phase;

/** Префлоп: карманные карты розданы, общих карт нет. */
public final class PreFlopPhase implements HandPhase {

    @Override
    public String name() {
        return "Префлоп";
    }

    @Override
    public int boardCardsToDeal() {
        return 0;
    }

    /** Первым ходит игрок после большого блайнда; вдвоём — кнопка (она же малый блайнд). */
    @Override
    public int firstToActIndex(int playersCount) {
        return playersCount == 2 ? 0 : 3 % playersCount;
    }

    @Override
    public HandPhase next() {
        return new FlopPhase();
    }
}

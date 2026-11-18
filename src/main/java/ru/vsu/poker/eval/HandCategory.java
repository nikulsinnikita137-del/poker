package ru.vsu.poker.eval;

/** Комбинации покера. Порядок констант — порядок старшинства: чем ниже в списке, тем сильнее. */
public enum HandCategory {
    HIGH_CARD("Старшая карта"),
    PAIR("Пара"),
    TWO_PAIR("Две пары"),
    THREE_OF_A_KIND("Тройка"),
    STRAIGHT("Стрит"),
    FLUSH("Флеш"),
    FULL_HOUSE("Фулл-хаус"),
    FOUR_OF_A_KIND("Каре"),
    STRAIGHT_FLUSH("Стрит-флеш"),
    ROYAL_FLUSH("Роял-флеш");

    private final String title;

    HandCategory(String title) {
        this.title = title;
    }

    /** @return название комбинации по-русски */
    public String title() {
        return title;
    }
}

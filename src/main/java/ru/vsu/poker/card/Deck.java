package ru.vsu.poker.card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

/**
 * Колода из 52 карт. Тасуется переданным генератором, поэтому при одном и том же зерне
 * порядок карт всегда одинаков.
 */
public final class Deck {
    private final List<Card> cards = new ArrayList<>(52);

    /**
     * Создаёт новую перетасованную колоду.
     *
     * @param random единственный источник случайности в игре
     */
    public Deck(Random random) {
        for (Suit s : Suit.values()) {
            for (Rank r : Rank.values()) {
                cards.add(new Card(r, s));
            }
        }
        Collections.shuffle(cards, random);
    }

    /**
     * Создаёт колоду с заданным порядком карт (первая в списке сдаётся первой). Нужна для тестов.
     *
     * @param order карты в порядке сдачи
     */
    public Deck(List<Card> order) {
        cards.addAll(order);
    }

    /**
     * Снимает верхнюю карту.
     *
     * @return карта
     * @throws NoSuchElementException если колода пуста
     */
    public Card draw() {
        if (cards.isEmpty()) {
            throw new NoSuchElementException("Колода пуста");
        }
        return cards.remove(0);
    }

    /** @return сколько карт осталось */
    public int size() {
        return cards.size();
    }
}

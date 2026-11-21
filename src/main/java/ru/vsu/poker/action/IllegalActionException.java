package ru.vsu.poker.action;

/** Недопустимое действие: нарушает правила торговли в текущей ситуации. */
public class IllegalActionException extends RuntimeException {

    /**
     * @param message что именно нарушено
     */
    public IllegalActionException(String message) {
        super(message);
    }
}

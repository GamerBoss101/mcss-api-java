package com.mcssapi;

public enum ServerAction {
    INVALID(0),
    STOP(1),
    START(2),
    KILL(3),
    RESTART(4);

    private final int value;

    ServerAction(final int newValue) {
        value = newValue;
    }

    public int getValue() { return value; }
}

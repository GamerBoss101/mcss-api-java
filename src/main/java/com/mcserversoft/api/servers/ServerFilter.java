package com.mcserversoft.api.servers;

public enum ServerFilter {
    NONE(0),
    MINIMAL(1),
    STATUS(2);

    private final int value;
    
    private ServerFilter(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

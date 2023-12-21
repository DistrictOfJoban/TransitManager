package com.lx862.mtrtm.data;

public enum TrainState {
    SKIP_COLLISION("Bypass collision", 0),
    HALT_DWELL("Dwell stopped", 1),
    HALT_SPEED("Speed stopped", 2);

    private final int pos;
    private final String name;

    TrainState(String name, int pos) {
        this.pos = pos;
        this.name = name;
    }
    public String getName() {
        return name;
    }

    public int getPos() {
        return pos;
    }
}

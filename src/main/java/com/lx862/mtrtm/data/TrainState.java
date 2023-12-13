package com.lx862.mtrtm.data;

public enum TrainState {
    SKIP_COLLISION(0),
    HALT_DWELL(1),
    HALT_SPEED(2);

    private final int pos;

    TrainState(int pos) {
        this.pos = pos;
    }

    public int getPos() {
        return pos;
    }
}

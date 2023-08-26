package com.lx862.mtrtm.data;

import mtr.data.TrainServer;
import net.minecraft.world.phys.Vec3;
import java.util.Set;
import java.util.UUID;

public class ExposedTrainData {
    public TrainServer train;
    public long routeId;
    public Vec3[] positions;
    public boolean isManual;
    public boolean isCurrentlyManual;
    public int accelerationSign;
    public int manualCooldown;
    public int manualToAutomaticTime;

    public ExposedTrainData(TrainServer server, long routeId, Vec3[] positions, boolean isManual) {
        this.train = server;
        this.routeId = routeId;
        this.positions = positions;
        this.isManual = isManual;
    }
}

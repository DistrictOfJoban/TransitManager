package com.lx.mtrtm.data;

import mtr.data.TrainServer;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.util.math.Vec3d;

import java.util.Set;
import java.util.UUID;

public class ExposedTrainData {
    public TrainServer train;
    public long routeId;
    public Vec3d[] positions;
    public boolean isManual;
    public boolean isCurrentlyManual;
    public int accelerationSign;
    public int manualCooldown;
    public int manualToAutomaticTime;
    public Set<UUID> ridingEntities;
    public SimpleInventory inventory;

    public ExposedTrainData(TrainServer server, long routeId, Vec3d[] positions, boolean isManual, SimpleInventory inventory, Set<UUID> ridingEntities) {
        this.train = server;
        this.routeId = routeId;
        this.positions = positions;
        this.isManual = isManual;
        this.inventory = inventory;
        this.ridingEntities = ridingEntities;
    }
}

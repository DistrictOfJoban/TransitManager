package com.lx862.mtrtm.data;

import net.minecraft.world.phys.Vec3;
import org.mtr.core.data.Vehicle;

public class VehicleDataWrapper {
    public Vehicle vehicle;
    public long routeId;
    public Vec3[] positions;
    public boolean isManual;
    public boolean isCurrentlyManual;
    public int accelerationSign;
    public int manualCooldown;
    public int manualToAutomaticTime;

    public VehicleDataWrapper(Vehicle vehicle, long routeId, Vec3[] positions, boolean isManual) {
        this.vehicle = vehicle;
        this.routeId = routeId;
        this.positions = positions;
        this.isManual = isManual;
        this.isCurrentlyManual = vehicle.vehicleExtraData.getIsCurrentlyManual();
        this.accelerationSign = vehicle.vehicleExtraData.getPowerLevel();
    }
}

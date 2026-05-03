package com.lx862.mtrtm.data;

import com.lx862.mtrtm.mixin.VehicleAccessorMixin;
import com.lx862.mtrtm.mixin.VehicleSchemaAccessorMixin;
import org.mtr.core.data.PathData;
import org.mtr.core.data.Vehicle;
import org.mtr.core.data.VehicleCar;
import org.mtr.core.data.VehicleRidingEntity;
import org.mtr.core.tool.Utilities;
import org.mtr.core.tool.Vector;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;

import java.util.ArrayList;
import java.util.List;

public class TargetVehicle {
    public final Vehicle vehicle;
    public final double speedKmh;
    public final Vector[] positions;
    public final boolean isManual;
    public final boolean isCurrentlyManual;
    public final int accelerationSign;
    public final long manualCooldownMs;
    public final long manualToAutomaticTime;
    public final long totalDwellTime;
    public final long elapsedDwellTime;
    public int closestCar = 0;
    public final List<VehicleRidingEntity> ridingEntities;
    public final ObjectArrayList<ObjectObjectImmutablePair<VehicleCar, ObjectArrayList<ObjectObjectImmutablePair<Vector, Vector>>>> carsAndPos;

    public TargetVehicle(Vehicle vehicle, Vector[] positions) {
        this.vehicle = vehicle;
        this.carsAndPos = this.vehicle.getVehicleCarsAndPositions();
        this.positions = positions;
        this.manualCooldownMs = ((VehicleAccessorMixin)vehicle).getManualCooldown();
        this.manualToAutomaticTime = -1;
        this.isManual = vehicle.vehicleExtraData.getIsManualAllowed();
        this.isCurrentlyManual = vehicle.vehicleExtraData.getIsCurrentlyManual();
        this.accelerationSign = vehicle.vehicleExtraData.getPowerLevel();
        this.speedKmh = ((VehicleSchemaAccessorMixin)vehicle).getSpeed() * 1000 * 3.6;
        this.totalDwellTime = getTotalDwellTime();
        this.elapsedDwellTime = ((VehicleSchemaAccessorMixin)vehicle).getElapsedDwellTime();
        this.ridingEntities = new ArrayList<>();
        vehicle.vehicleExtraData.iterateRidingEntities(vehicleRidingEntity -> ridingEntities.add(vehicleRidingEntity));
    }

    private long getTotalDwellTime() {
        int railIndex = Utilities.getIndexFromConditionalList(vehicle.vehicleExtraData.immutablePath, ((VehicleSchemaAccessorMixin)vehicle).getRailProgress() - 1F);
        if(railIndex < vehicle.vehicleExtraData.immutablePath.size()) {
            PathData path = vehicle.vehicleExtraData.immutablePath.get(railIndex);
            if(path != null) {
                return path.getDwellTime();
            }
        }
        return -1;
    }
}

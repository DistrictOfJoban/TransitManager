package com.lx862.mtrtm.util;

import com.lx862.mtrtm.data.TargetVehicle;
import com.lx862.mtrtm.mixin.SidingAccessorMixin;
import com.lx862.mtrtm.mixin.VehicleAccessorMixin;
import com.lx862.mtrtm.mixin.VehicleSchemaAccessorMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.mtr.core.data.*;
import org.mtr.core.simulation.Simulator;
import org.mtr.core.tool.Vector;
import org.mtr.libraries.it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.mtr.mapping.holder.Identifier;

import java.util.*;
import java.util.stream.Collectors;

public class MtrUtil {
    public static Simulator getSimulator(ObjectImmutableList<Simulator> simulators, Level level) {
        Identifier dimId = new Identifier(level.dimension().location());
        for(Simulator simulator : simulators) {
            if(dimensionToId(simulator.dimension).equals(dimId)) return simulator;
        }
        return null;
    }

    private static Identifier dimensionToId(String dim) {
        // MTR uses / as the separator
        return new Identifier(dim.replace("/", ":"));
    }

//    public static void syncTrainToPlayers(TrainServer trainServer, List<ServerPlayer> players) {
//        FriendlyByteBuf trainPacket = PacketByteBufs.create();
//        trainServer.writePacket(trainPacket);
//
//        FriendlyByteBuf packet = PacketByteBufs.create();
//        packet.writeBytes(trainPacket);
//
//        for(ServerPlayer player : players) {
//            ServerPlayNetworking.send(player, PACKET_UPDATE_TRAINS, packet);
//        }
//    }

    public static Station findStation(String targetSta, Simulator simulator) {
        Map<Long, Station> staMap = simulator.stationIdMap;
        for (Map.Entry<Long, Station> entry : staMap.entrySet()) {
            if(entry.getValue().getName().trim().equals(targetSta)) return entry.getValue();

            for(String lang : entry.getValue().getName().split("\\|")) {
                if(lang.trim().equalsIgnoreCase(targetSta)) {
                    return entry.getValue();
                }
            }
        }

        return null;
    }

    public static <T extends AreaBase> Set<T> findArea(String targetName, Set<T> areaBases) {
        String trimmedTargetDepot = targetName == null ? null : targetName.toLowerCase(Locale.ENGLISH).trim();

        if(trimmedTargetDepot == null) {
            return areaBases;
        } else {
            return areaBases.stream().filter(e -> e.getName().toLowerCase(Locale.ENGLISH).trim().contains(trimmedTargetDepot)).collect(Collectors.toSet());
        }
    }

    public static Set<Depot> findDepots(String targetDepot, Simulator simulator) {
        String trimmedTargetDepot = targetDepot == null ? null : targetDepot.toLowerCase(Locale.ENGLISH).trim();

        if(trimmedTargetDepot == null) {
            return simulator.depots;
        } else {
            return simulator.depots.stream().filter(e -> e.getName().toLowerCase(Locale.ENGLISH).trim().contains(trimmedTargetDepot)).collect(Collectors.toSet());
        }
    }

    public static String getRouteName(String str) {
        return str.replace("|", " ");
    }

    public static BlockPos getNonOccupiedPos(Level world, BlockPos targetPos, AreaBase<?, ? extends SavedRailBase> area) {
        final int initialY = targetPos.getY();

        long maxRailY = Integer.MIN_VALUE;
        long minRailY = Integer.MAX_VALUE;

        if(area.savedRails.isEmpty()) {
            maxRailY = initialY;
            minRailY = initialY;
        } else {
            for(SavedRailBase savedRailBase : area.savedRails) {
                long thisRailY = savedRailBase.getMidPosition().getY();
                if(thisRailY > maxRailY) {
                    maxRailY = thisRailY;
                }
                if(thisRailY < minRailY) {
                    minRailY = thisRailY;
                }
            }
        }

        final long targetY = Math.min(maxRailY+5, Math.max(minRailY, initialY));

        BlockPos pos = new BlockPos(targetPos.getX(), (int)targetY, targetPos.getZ());

        BlockState state = world.getBlockState(pos);
        if(!state.isAir()) { // Occupied
            BlockPos.MutableBlockPos mPos = pos.mutable();
            int offset = 0;

            while(offset < world.getHeight()) {
                offset++;

                BlockState offsetState = world.getBlockState(mPos.setY((int)targetY + offset));
                if(offsetState.isAir()) {
                    break;
                }
            }
            return new BlockPos(pos.getX(), mPos.getY(), pos.getZ());
        }

        return pos;
    }

    public static TargetVehicle getNearestTrain(ServerPlayer player, Vector playerPos, Simulator simulator) {
        TargetVehicle closestVehicle = null;
        List<TargetVehicle> vehicles = new ArrayList<>();

        for(Siding siding : simulator.sidings) {
            for(Vehicle train : ((SidingAccessorMixin)(Object)siding).getVehicles()) {
                var vehicleCarsAndPos = train.getVehicleCarsAndPositions();
                final Vector[] positions = new Vector[vehicleCarsAndPos.size()];

                double railProgress = ((VehicleSchemaAccessorMixin)train).getRailProgress();
                for(int i = 0; i < positions.length; i++) {
                    double trainLength = vehicleCarsAndPos.get(i).left().getLength();
                    double carMidRailProgress = railProgress - (trainLength / 2);
                    positions[i] = ((VehicleAccessorMixin)train).getPositionAt(carMidRailProgress, new DoubleArrayList());
                    railProgress -= trainLength;
                }

                vehicles.add(new TargetVehicle(train, positions));
            }
        }

        Vector closestPos = null;
        for(TargetVehicle vehicle : vehicles) {
            if(player != null) {
                VehicleRidingEntity playerRidingEntity = vehicle.ridingEntities.stream().filter(e -> e.uuid.equals(player.getUUID())).findFirst().orElse(null);
                // Player is riding, so it is most definitely the train player wants
                if(playerRidingEntity != null) {
                    vehicle.closestCar = (int)playerRidingEntity.getRidingCar();
                    closestVehicle = vehicle;
                    break;
                }
            }

            for(int i = 0; i < vehicle.positions.length; i++) {
                if(closestVehicle == null) {
                    vehicle.closestCar = i;
                    closestVehicle = vehicle;
                    closestPos = vehicle.positions[i];
                } else {
                    double lastTrainDistance = Util.getManhattenDistance(closestPos, playerPos);
                    double thisTrainDistance = Util.getManhattenDistance(vehicle.positions[i], playerPos);
                    boolean isCloser = thisTrainDistance < lastTrainDistance;

                    if(isCloser) {
                        vehicle.closestCar = i;
                        closestVehicle = vehicle;
                        closestPos = vehicle.positions[i];
                    }
                }
            }
        }

        if(closestVehicle == null) {
            return null;
        }

        return closestVehicle;
    }

}
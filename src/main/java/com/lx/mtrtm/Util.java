package com.lx.mtrtm;

import com.lx.mtrtm.data.ExposedTrainData;
import com.lx.mtrtm.mixin.SidingAccessorMixin;
import com.lx.mtrtm.mixin.TrainAccessorMixin;
import com.lx.mtrtm.mixin.TrainServerAccessorMixin;
import mtr.data.*;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static mtr.packet.IPacket.PACKET_UPDATE_TRAINS;

public class Util {
    public static ExposedTrainData getNearestTrain(World world, Vec3d playerPos) {
        List<ExposedTrainData> trainDataList = new ArrayList<>();
        RailwayData data = RailwayData.getInstance(world);

        /* Loop through each siding */
        for(Siding siding : data.sidings) {
            /* Loop through each train in each siding */
            for(TrainServer train : ((SidingAccessorMixin)siding).getTrains()) {
                final Vec3d[] positions = new Vec3d[train.trainCars + 1];

                /* Loop through each car in each train */
                for (int i = 0; i <= train.trainCars; i++) {
                    positions[i] = ((TrainAccessorMixin)train).getTheRoutePosition(((TrainAccessorMixin) train).getReversed() ? train.trainCars - i : i, train.spacing);
                }

                trainDataList.add(new ExposedTrainData(train, ((TrainServerAccessorMixin)train).getRouteId(), positions, ((TrainAccessorMixin)train).getIsManualAllowed()));
            }
        }

        ExposedTrainData closestTrainCar = null;
        Vec3d closestPos = null;
        for(ExposedTrainData train : trainDataList) {
            /* Loop through every car */
            for(int i = 0; i < train.positions.length; i++) {
                /* First train found */
                if(closestTrainCar == null) {
                    closestTrainCar = train;
                    closestPos = train.positions[i];
                } else {
                    /* Compare if the train looped is closer to the player */
                    double lastTrainDistance = getManhattenDistance(closestPos, playerPos);
                    double thisTrainDistance = getManhattenDistance(train.positions[i], playerPos);
                    boolean isCloser = thisTrainDistance < lastTrainDistance;

                    if(isCloser) {
                        closestTrainCar = train;
                        closestPos = train.positions[i];
                    }
                }
            }
        }

        ExposedTrainData trainData = closestTrainCar;

        if(trainData == null) {
            return null;
        }

        if(trainData.isManual) {
            trainData.isCurrentlyManual = ((TrainAccessorMixin)trainData.train).getIsCurrentlyManual();
            if(trainData.isCurrentlyManual) {
                trainData.accelerationSign = ((TrainAccessorMixin) trainData.train).getManualNotch();
                trainData.manualCooldown = ((TrainServerAccessorMixin)trainData.train).getManualCoolDown();
                trainData.manualToAutomaticTime = ((TrainAccessorMixin) trainData.train).getManualToAutomaticTime();
            }
        }

        return trainData;
    }

    public static double getManhattenDistance(Vec3d pos1, Vec3d pos2) {
        return Math.abs(pos2.getX() - pos1.getX()) + Math.abs(pos2.getY() - pos1.getY()) + Math.abs(pos2.getZ() - pos1.getZ());
    }

    public static double getManhattenDistance(BlockPos pos1, BlockPos pos2) {
        return Math.abs(pos2.getX() - pos1.getX()) + Math.abs(pos2.getY() - pos1.getY()) + Math.abs(pos2.getZ() - pos1.getZ());
    }

    public static String getRouteName(String str) {
        return str.split("\\|\\|")[0].replace("|", " ");
    }

    public static String getChin(String str) {
        return str.split("\\|")[0];
    }

    public static String getEng(String str) {
        String[] splitted = str.split("\\|");
        return splitted.length > 1 ? splitted[1] : "";
    }

    public static Depot findDepot(String targetDepot, World world) {
        String trimmedTargetDepot = targetDepot.trim();
        RailwayData data = RailwayData.getInstance(world);
        if(data == null) return null;

        Map<Long, Depot> depotMap = data.dataCache.depotIdMap;
        for (Depot depot : depotMap.values()) {
            if(targetDepot.equals(depot.name)) {
                return depot;
            }

            for(String lang : depot.name.split("\\|")) {
                if(lang.trim().equalsIgnoreCase(trimmedTargetDepot)) {
                    return depot;
                }
            }
        }

        return null;
    }

    public static List<String> formulateMatchingString(String target, List<String> list) {
        if(target.trim().length() == 0) {
            return list;
        }
        List<String> newList = new ArrayList<>(list.stream().filter(e -> e.toLowerCase().contains(target.toLowerCase())).toList());
        return newList;
    }

    public static Station findStation(String targetStn, World world) {
        RailwayData data = RailwayData.getInstance(world);
        if(data == null) return null;

        Map<Long, Station> stnMap = data.dataCache.stationIdMap;
        for (Map.Entry<Long, Station> entry : stnMap.entrySet()) {
            if(entry.getValue().name.trim().equals(targetStn)) return entry.getValue();

            for(String lang : entry.getValue().name.split("\\|")) {
                if(lang.trim().equalsIgnoreCase(targetStn)) {
                    return entry.getValue();
                }
            }
        }

        return null;
    }

    public static void syncTrainToPlayers(TrainServer trainServer, List<ServerPlayerEntity> players) {
        PacketByteBuf trainPacket = PacketByteBufs.create();
        trainServer.writePacket(trainPacket);

        PacketByteBuf packet = PacketByteBufs.create();
        packet.writeBytes(trainPacket);

        for(ServerPlayerEntity player : players) {
            ServerPlayNetworking.send(player, PACKET_UPDATE_TRAINS, packet);
        }
    }

    public static Pair<MutableText, MutableText> getInterchangeRouteNames(Station station, Route thisRoute, RailwayData data, Formatting forceColor) {
        MutableText chinTexts = Mappings.literalText("");
        MutableText engTexts = Mappings.literalText("");
        MutableText comma = Mappings.literalText(", ");
        final HashMap<String, Route> routeInStn = new HashMap<>();
        for(Route rt : data.routes) {
            if(!rt.isHidden && !getRouteName(rt.name).equals(getRouteName(thisRoute.name)) && !rt.name.startsWith("[WIP]") && rt.routeType != RouteType.HIGH_SPEED) {
                for(Route.RoutePlatform pid : rt.platformIds) {
                    Station stn = data.dataCache.platformIdToStation.get(pid.platformId);
                    if(stn != null && stn.id == station.id) {
                        routeInStn.put(getRouteName(rt.name), rt);
                    }
                }
            }
        }
        if(routeInStn.size() == 0) return null;

        boolean first = true;
        for(Route route : routeInStn.values()) {
            MutableText chinText = Mappings.literalText(getRouteName(getChin(route.name)));
            MutableText engText = Mappings.literalText(getRouteName(getEng(route.name)));
            if(forceColor == null) {
                chinText.setStyle(Style.EMPTY.withColor(route.color));
                engText.setStyle(Style.EMPTY.withColor(route.color));
            } else {
                chinTexts.setStyle(Style.EMPTY.withColor(forceColor));
                engText.setStyle(Style.EMPTY.withColor(forceColor));
            }
            if(first) {
                chinTexts.append(chinText);
                engTexts.append(engText);
            } else {
                chinTexts.append(comma).append(chinText);
                engTexts.append(comma).append(engText);
            }
            first = false;
        }
        return new Pair<>(chinTexts, engTexts);
    }
}

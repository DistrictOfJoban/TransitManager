package com.lx862.mtrtm;

import com.lx862.mtrtm.data.ExposedTrainData;
import com.lx862.mtrtm.mixin.SidingAccessorMixin;
import com.lx862.mtrtm.mixin.TrainAccessorMixin;
import com.lx862.mtrtm.mixin.TrainServerAccessorMixin;
import mtr.data.*;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static mtr.packet.IPacket.PACKET_UPDATE_TRAINS;

public class Util {
    public static ExposedTrainData getNearestTrain(Level world, ServerPlayer player, Vec3 playerPos) {
        RailwayData railwayData = RailwayData.getInstance(world);
        List<ExposedTrainData> trainDataList = new ArrayList<>();
        ExposedTrainData closestTrainCar = null;

        /* Loop through each siding */
        for(Siding siding : railwayData.sidings) {
            /* Loop through each train in each siding */
            for(TrainServer train : ((SidingAccessorMixin)siding).getTrains()) {
                final Vec3[] positions = new Vec3[train.trainCars + 1];

                /* Loop through each car in each train */
                for (int i = 0; i <= train.trainCars; i++) {
                    positions[i] = ((TrainAccessorMixin)train).getTheRoutePosition(((TrainAccessorMixin) train).getReversed() ? train.trainCars - i : i, train.spacing);
                }

                trainDataList.add(new ExposedTrainData(train, ((TrainServerAccessorMixin)train).getRouteId(), positions, ((TrainAccessorMixin)train).getIsManualAllowed()));
            }
        }

        Vec3 closestPos = null;
        for(ExposedTrainData train : trainDataList) {
            // Player is riding, so it is most definitely the train player wants
            if(player != null && train.train.isPlayerRiding(player)) {
                closestTrainCar = train;
                break;
            }

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

    public static double getManhattenDistance(Vec3 pos1, Vec3 pos2) {
        return Math.abs(pos2.x() - pos1.x()) + Math.abs(pos2.y() - pos1.y()) + Math.abs(pos2.z() - pos1.z());
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

    public static Depot findDepot(String targetDepot, Level world) {
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

    public static Station findStation(String targetStn, Level world) {
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

    public static void syncTrainToPlayers(TrainServer trainServer, List<ServerPlayer> players) {
        FriendlyByteBuf trainPacket = PacketByteBufs.create();
        trainServer.writePacket(trainPacket);

        FriendlyByteBuf packet = PacketByteBufs.create();
        packet.writeBytes(trainPacket);

        for(ServerPlayer player : players) {
            ServerPlayNetworking.send(player, PACKET_UPDATE_TRAINS, packet);
        }
    }

    public static Tuple<MutableComponent, MutableComponent> getInterchangeRouteNames(Station station, Route thisRoute, RailwayData data, ChatFormatting forceColor) {
        MutableComponent chinTexts = Mappings.literalText("");
        MutableComponent engTexts = Mappings.literalText("");
        MutableComponent comma = Mappings.literalText(", ");
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
            MutableComponent chinText = Mappings.literalText(getRouteName(getChin(route.name)));
            MutableComponent engText = Mappings.literalText(getRouteName(getEng(route.name)));
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
        return new Tuple<>(chinTexts, engTexts);
    }
}

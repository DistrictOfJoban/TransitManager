package com.lx862.mtrtm;

import com.lx862.mtrtm.data.ExposedTrainData;
import com.lx862.mtrtm.mixin.SidingAccessorMixin;
import com.lx862.mtrtm.mixin.TrainAccessorMixin;
import com.lx862.mtrtm.mixin.TrainServerAccessorMixin;
import mtr.data.*;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.stream.Collectors;

import static mtr.packet.IPacket.PACKET_UPDATE_TRAINS;

public class MtrUtil {
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
        final HashMap<String, Route> routeInSta = new HashMap<>();
        for(Route rt : data.routes) {
            if(!rt.isHidden && !getRouteName(rt.name).equals(getRouteName(thisRoute.name)) && !rt.name.startsWith("[WIP]") && rt.routeType != RouteType.HIGH_SPEED) {
                for(Route.RoutePlatform pid : rt.platformIds) {
                    Station sta = data.dataCache.platformIdToStation.get(pid.platformId);
                    if(sta != null && sta.id == station.id) {
                        routeInSta.put(getRouteName(rt.name), rt);
                    }
                }
            }
        }
        if(routeInSta.isEmpty()) return null;

        boolean first = true;
        for(Route route : routeInSta.values()) {
            MutableComponent chinText = Mappings.literalText(getRouteName(getNameChinese(route.name)));
            MutableComponent engText = Mappings.literalText(getRouteName(getNameEnglish(route.name)));
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

    public static Station findStation(String targetSta, Level world) {
        RailwayData data = RailwayData.getInstance(world);
        if(data == null) return null;

        Map<Long, Station> staMap = data.dataCache.stationIdMap;
        for (Map.Entry<Long, Station> entry : staMap.entrySet()) {
            if(entry.getValue().name.trim().equals(targetSta)) return entry.getValue();

            for(String lang : entry.getValue().name.split("\\|")) {
                if(lang.trim().equalsIgnoreCase(targetSta)) {
                    return entry.getValue();
                }
            }
        }

        return null;
    }

    public static Set<Depot> findDepots(String targetDepot, Level world) {
        String trimmedTargetDepot = targetDepot == null ? null : targetDepot.toLowerCase(Locale.ENGLISH).trim();
        RailwayData data = RailwayData.getInstance(world);
        if(data == null) return Set.of();

        if(trimmedTargetDepot == null) {
            return data.depots;
        } else {
            return data.depots.stream().filter(e -> e.name.toLowerCase(Locale.ENGLISH).trim().contains(trimmedTargetDepot)).collect(Collectors.toSet());
        }
    }

    public static String getRouteName(String str) {
        return str.split("\\|\\|")[0].replace("|", " ");
    }

    public static String getNameChinese(String str) {
        return str.split("\\|")[0];
    }

    public static String getNameEnglish(String str) {
        String[] splitted = str.split("\\|");
        return splitted.length > 1 ? splitted[1] : "";
    }

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
                    double lastTrainDistance = Util.getManhattenDistance(closestPos, playerPos);
                    double thisTrainDistance = Util.getManhattenDistance(train.positions[i], playerPos);
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
}
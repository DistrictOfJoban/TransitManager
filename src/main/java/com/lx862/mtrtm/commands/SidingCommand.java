package com.lx862.mtrtm.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lx862.mtrtm.TransitManager;
import com.lx862.mtrtm.mixin.InitAccessorMixin;
import com.lx862.mtrtm.mixin.MainAccessorMixin;
import com.lx862.mtrtm.mixin.SidingAccessorMixin;
import com.lx862.mtrtm.util.MtrUtil;
import com.mojang.brigadier.CommandDispatcher;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.mtr.core.Main;
import org.mtr.core.data.*;
import org.mtr.core.data.Position;
import org.mtr.core.simulation.Simulator;
import org.mtr.core.tool.Utilities;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongArrayList;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.TextHelper;

import java.util.ArrayList;
import java.util.List;

public class SidingCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("siding")
                .requires(ctx -> ctx.hasPermission(2))
                        .then(Commands.literal("scheduleRaw")
                                .executes(context -> {
                                    try {
                                        ServerPlayer rawPlayer = context.getSource().getPlayerOrException();
                                        ServerPlayerEntity player = new ServerPlayerEntity(rawPlayer);

                                        Main tsc = InitAccessorMixin.mtrtm$getMain();
                                        Simulator data = MtrUtil.getSimulator(((MainAccessorMixin)tsc).mtrtm$getSimulators(), context.getSource().getLevel());

                                        BlockPos playerBlockPos = player.getBlockPos();
                                        Position playerPos = new Position(playerBlockPos.getX(), playerBlockPos.getY(), playerBlockPos.getZ());

                                        // TODO: PICK UP - User friendly dump / siding info
                                        // TODO: Sort it by distance in case we have multiple matches
                                        Siding siding = data.sidings.stream()
                                                .filter(rail -> rail.closeTo(playerPos, 5))
                                                .findFirst()
                                                .orElse(null);

                                        if(siding == null) {
                                            context.getSource().sendFailure(TextHelper.literal("No nearby siding found.").data);
                                            return 1;
                                        }
                                        Depot depot = siding.area;

                                        LongArrayList sidingDepartures = ((SidingAccessorMixin)(Object)siding).mtrtm$getDepartures();

                                        List<RoutePlatformSet> routePlatforms = new ArrayList<>();
                                        for(int i = 0; i < depot.routes.size(); i++) {
                                            Route route = depot.routes.get(i);
                                            for(RoutePlatformData routePlatformData : route.getRoutePlatforms()) {
                                                Route lastRoute = i == 0 ? depot.getRepeatInfinitely() ? Utilities.getElement(depot.routes, depot.routes.size() - 1) : null : Utilities.getElement(depot.routes, i - 1);
                                                RoutePlatformData lastRoutePlatformData = lastRoute == null ? null : Utilities.getElement(lastRoute.getRoutePlatforms(), lastRoute.getRoutePlatforms().size()-1);

                                                if (lastRoutePlatformData != null && routePlatformData.platform.getId() == lastRoutePlatformData.platform.getId()) {
                                                    routePlatforms.remove(routePlatforms.size()-1);
                                                    routePlatforms.add(new RoutePlatformSet(route, routePlatformData));
                                                } else {
                                                    routePlatforms.add(new RoutePlatformSet(route, routePlatformData));
                                                }
                                            }
                                        }

                                        ObjectArrayList<PathData> sidingPath = new ObjectArrayList<>();
                                        sidingPath.addAll(((SidingAccessorMixin)(Object)siding).mtrtm$getPathSidingToMainRoute());
                                        sidingPath.addAll(((SidingAccessorMixin)(Object)siding).mtrtm$getPathMainRoute());
                                        sidingPath.addAll(((SidingAccessorMixin)(Object)siding).mtrtm$getPathMainRouteToSiding());

                                        List<JourneyCheckpoint> checkpoints = new ArrayList<>();

                                        if(!routePlatforms.isEmpty()) {
                                            long runningRouteId = -1;
                                            int routePlatformIndex = 0;

                                            for(int i = 0; i < sidingPath.size(); i++) {
                                                PathData path = sidingPath.get(i);

                                                boolean isPlatform = path.getRail().isPlatform();
                                                boolean isSiding = path.getRail().isSiding();

                                                if (isSiding) {
                                                    if(i == sidingPath.size() - 1) {
                                                        double arrivalTime = siding.getTimeAlongRoute(path.getEndDistance());
                                                        double departureTime = -1;
                                                        JourneyCheckpoint journeyCheckpoint = new JourneyCheckpoint(null, runningRouteId, siding.getName(), arrivalTime, departureTime, false, true);
                                                        checkpoints.add(journeyCheckpoint);
                                                    }
                                                } else if(isPlatform) {
                                                    RoutePlatformSet routePlatformSet = Utilities.getElement(routePlatforms, routePlatformIndex);
                                                    RoutePlatformData routePlatformData = routePlatformSet == null ? null : routePlatformSet.stopData();
                                                    if(routePlatformData != null) {
                                                        if (routePlatformData.getPlatform().getId() == path.getSavedRailBaseId()) {
                                                            runningRouteId = routePlatformSet.route.getId();

                                                            Station area = routePlatformData.getPlatform().area;
                                                            double arrivalTime = siding.getTimeAlongRoute(path.getEndDistance()-1);
                                                            double departureTime = arrivalTime + path.getDwellTime();
                                                            JourneyCheckpoint journeyCheckpoint = new JourneyCheckpoint(area, runningRouteId, routePlatformData.getPlatform().getName(), arrivalTime, departureTime, false, false);
                                                            checkpoints.add(journeyCheckpoint);

                                                            routePlatformIndex++;
                                                        }
                                                    }
                                                } else { // TBS
                                                    double arrivalTime = siding.getTimeAlongRoute(path.getEndDistance());
                                                    double departureTime = arrivalTime + path.getDwellTime();
                                                    JourneyCheckpoint journeyCheckpoint = new JourneyCheckpoint(null, runningRouteId, "TBS", arrivalTime, departureTime, true, false);
                                                    checkpoints.add(journeyCheckpoint);
                                                }
                                            }
                                        }

                                        JsonObject jsonObject = new JsonObject();
                                        jsonObject.addProperty("sidingId", siding.getId());
                                        jsonObject.addProperty("sidingName", siding.getName());

                                        JsonArray departureArray = new JsonArray();
                                        JsonArray journeyTimeArray = new JsonArray();
                                        for(Long departureTime : sidingDepartures) {
                                            departureArray.add(departureTime);
                                        }

                                        for(JourneyCheckpoint checkpoint : checkpoints) {
                                            journeyTimeArray.add(checkpoint.serialize());
                                        }

                                        jsonObject.add("departures", departureArray);
                                        jsonObject.add("journeyTime", journeyTimeArray);


                                        player.sendMessage(Text.cast(TextHelper.setStyle(TextHelper.literal(">>>>><<<<<"), Style.getEmptyMapped().withColor(TextColor.fromRgb(0x862F2B)))), false);
                                        player.sendMessage(Text.cast(TextHelper.setStyle(TextHelper.literal(jsonObject.toString()), Style.getEmptyMapped().withColor(TextFormatting.GOLD))), false);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                    return 1;
                                })
                        )
        );
    }

    public record RoutePlatformSet(Route route, RoutePlatformData stopData) {}

    public record JourneyCheckpoint(Station area, long runningRouteId, String trackName, double arrival, double departure, boolean isTbs, boolean isSiding) {
        public JsonObject serialize() {
            JsonObject entry = new JsonObject();
            entry.addProperty("areaId", area == null ? 0 : area.getId());
            entry.addProperty("routeId", runningRouteId);
            entry.addProperty("trackName", trackName);
            entry.addProperty("arrival", arrival);
            entry.addProperty("departure", departure);
            entry.addProperty("isTbs", isTbs);
            entry.addProperty("isSdg", isSiding);
            return entry;
        }
    };
}

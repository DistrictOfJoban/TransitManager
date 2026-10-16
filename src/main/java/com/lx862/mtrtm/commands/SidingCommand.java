package com.lx862.mtrtm.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lx862.mtrtm.mixin.InitAccessorMixin;
import com.lx862.mtrtm.mixin.MainAccessorMixin;
import com.lx862.mtrtm.mixin.SidingAccessorMixin;
import com.lx862.mtrtm.util.MtrUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.world.phys.Vec3;
import org.mtr.core.Main;
import org.mtr.core.data.*;
import org.mtr.core.data.Position;
import org.mtr.core.simulation.Simulator;
import org.mtr.core.tool.Utilities;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongArrayList;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.client.MinecraftClientData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SidingCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandNode<CommandSourceStack> node = dispatcher.register(Commands.literal("siding")
                .requires(ctx -> ctx.hasPermission(2))
                        .then(Commands.literal("scheduleRaw")
                                .executes(context -> executeSchedule(context))
                        )
        );

        dispatcher.register(
                Commands.literal("siding_at")
                        .requires(ctx -> ctx.hasPermission(2))
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                                .redirect(node,
                                        context -> context.getSource().withPosition(Vec3Argument.getVec3(context, "pos")))
                        )
        );
    }

    private static int executeSchedule(CommandContext<CommandSourceStack> ctx) {
        try {
            Main tsc = InitAccessorMixin.mtrtm$getMain();
            Simulator data = MtrUtil.getSimulator(((MainAccessorMixin)tsc).mtrtm$getSimulators(), ctx.getSource().getLevel());

            Vec3 vec3 = ctx.getSource().getPosition();
            Position tscPos = new Position((long)vec3.x(), (long)vec3.y(), (long)vec3.z());

            // TODO: PICK UP - User friendly dump / siding info
            Siding siding = data.sidings.stream()
                    .filter(rail -> rail.closeTo(tscPos, 5))
                    .min(Comparator.comparingDouble((rail) -> rail.getApproximateClosestDistance(tscPos, MinecraftClientData.getInstance())))
                    .orElse(null);

            if(siding == null) {
                ctx.getSource().sendFailure(TextHelper.literal("No nearby siding found.").data);
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
                    } else if(path.getDwellTime() == 1) { // TBS
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


            ctx.getSource().sendSuccess(() -> Text.cast(TextHelper.setStyle(TextHelper.literal(">>>>><<<<<"), Style.getEmptyMapped().withColor(TextColor.fromRgb(0x862F2B)))).data, false);
            ctx.getSource().sendSuccess(() -> Text.cast(TextHelper.setStyle(TextHelper.literal(jsonObject.toString()), Style.getEmptyMapped().withColor(TextFormatting.GOLD))).data, false);
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
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

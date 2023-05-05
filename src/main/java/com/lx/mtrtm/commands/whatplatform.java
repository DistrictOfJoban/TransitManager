package com.lx.mtrtm.commands;

import com.lx.mtrtm.Mappings;
import com.mojang.brigadier.CommandDispatcher;
import mtr.data.Platform;
import mtr.data.RailwayData;
import mtr.data.Route;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.util.Formatting;

import java.util.ArrayList;

public class whatplatform {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("whatplatform")
                .requires(ctx -> ctx.hasPermissionLevel(2))
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    RailwayData data = RailwayData.getInstance(context.getSource().getWorld());
                    long platformId = RailwayData.getClosePlatformId(data.platforms, data.dataCache, context.getSource().getPlayer().getBlockPos());
                    Platform platform = data.dataCache.platformIdMap.get(platformId);

                    if(platform == null) {
                        player.sendMessage(Mappings.literalText("No Platform found.").formatted(Formatting.RED), false);
                        return 1;
                    }

                    ArrayList<String> routeList = new ArrayList<>();
                    for (Route route : data.routes) {
                        if (route.platformIds.stream().anyMatch(e -> e.platformId == platformId)) {
                            routeList.add(route.name.replace("|", " "));
                        }
                    }

                    if(routeList.isEmpty()) {
                        routeList.add("None");
                    }

                    HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Mappings.literalText(String.join("\n", routeList)).formatted(Formatting.GREEN));
                    player.sendMessage(Mappings.literalText("===== Platform " + platform.name + " ====="), false);
                    player.sendMessage(Mappings.literalText("Dwell: " + (platform.getDwellTime() / 2F) + "s").formatted(Formatting.GOLD), false);
                    player.sendMessage(Mappings.literalText("Transport Type: " + platform.transportMode.toString().toLowerCase()).formatted(Formatting.GOLD), false);
                    player.sendMessage(Mappings.literalText("Route List: (Hover)").formatted(Formatting.GOLD).formatted(Formatting.UNDERLINE).styled(style -> style.withHoverEvent(hoverEvent)), false);
                    return 1;
                }));
    }
}

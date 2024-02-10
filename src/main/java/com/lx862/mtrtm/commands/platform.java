package com.lx862.mtrtm.commands;

import com.lx862.mtrtm.Mappings;
import com.lx862.mtrtm.Util;
import com.mojang.brigadier.CommandDispatcher;
import mtr.data.Platform;
import mtr.data.RailwayData;
import mtr.data.Route;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;

public class platform {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("platform")
                .requires(ctx -> ctx.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    RailwayData data = RailwayData.getInstance(context.getSource().getLevel());
                    long platformId = RailwayData.getClosePlatformId(data.platforms, data.dataCache, context.getSource().getPlayerOrException().blockPosition());
                    Platform platform = data.dataCache.platformIdMap.get(platformId);

                    if(platform == null) {
                        context.getSource().sendFailure(Mappings.literalText("No nearby platform found."));
                        return 1;
                    }

                    ArrayList<String> routeList = new ArrayList<>();
                    for (Route route : data.routes) {
                        if (route.platformIds.stream().anyMatch(e -> e.platformId == platformId)) {
                            String routeStr = route.name.replace("|", " ");
                            if(route.isHidden) routeStr += " (Hidden)";
                            routeList.add(routeStr);
                        }
                    }

                    if(routeList.isEmpty()) {
                        routeList.add("None");
                    }

                    HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Mappings.literalText(String.join("\n", routeList)).withStyle(ChatFormatting.GREEN));
                    player.displayClientMessage(Mappings.literalText("===== Platform " + platform.name + " ====="), false);
                    player.displayClientMessage(Mappings.literalText("Dwell: " + Util.getReadableTimeMs((long)(platform.getDwellTime() / 2F) * 1000L)).withStyle(ChatFormatting.GOLD), false);
                    player.displayClientMessage(Mappings.literalText("Transport Type: " + platform.transportMode.toString().toLowerCase()).withStyle(ChatFormatting.GOLD), false);
                    player.displayClientMessage(Mappings.literalText("Route List: (Hover)").withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.UNDERLINE).withStyle(style -> style.withHoverEvent(hoverEvent)), false);
                    return 1;
                }));
    }
}

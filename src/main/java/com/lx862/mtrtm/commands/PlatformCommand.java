package com.lx862.mtrtm.commands;

import com.lx862.mtrtm.mixin.InitAccessorMixin;
import com.lx862.mtrtm.mixin.MainAccessorMixin;
import com.lx862.mtrtm.util.MtrUtil;
import com.lx862.mtrtm.util.Util;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import org.mtr.core.Main;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Position;
import org.mtr.core.data.Route;
import org.mtr.core.simulation.Simulator;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.TextHelper;

import java.util.ArrayList;

public class PlatformCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("platform")
                .requires(ctx -> ctx.hasPermission(2))
                .executes(context -> {
                    ServerPlayer rawPlayer = context.getSource().getPlayerOrException();
                    ServerPlayerEntity player = new ServerPlayerEntity(rawPlayer);

                    Main tsc = InitAccessorMixin.mtrtm$getMain();
                    Simulator data = MtrUtil.getSimulator(((MainAccessorMixin)tsc).mtrtm$getSimulators(), context.getSource().getLevel());

                    BlockPos playerBlockPos = player.getBlockPos();
                    Position playerPos = new Position(playerBlockPos.getX(), playerBlockPos.getY(), playerBlockPos.getZ());

                    Platform platform = data.platforms.stream().filter(plat -> plat.closeTo(playerPos, 5)).findFirst().orElse(null);

                    if(platform == null) {
                        context.getSource().sendFailure(TextHelper.literal("No nearby platform found.").data);
                        return 1;
                    }

                    ArrayList<String> routeList = new ArrayList<>();
                    for (Route route : data.routes) {
                        if (route.getRoutePlatforms().stream().anyMatch(e -> e.getPlatform().getId() == platform.getId())) {
                            String routeStr = route.getName().replace("|", " ");
                            if(route.getHidden()) routeStr += " (Hidden)";
                            routeList.add(routeStr);
                        }
                    }

                    if(routeList.isEmpty()) {
                        routeList.add("None");
                    }

                    HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextHelper.setStyle(TextHelper.literal(String.join("\n", routeList)), Style.getEmptyMapped().withColor(TextFormatting.GREEN)).data);
                    player.sendMessage(Text.cast(TextHelper.setStyle(TextHelper.literal("===== Platform " + platform.getName() + " ====="), Style.getEmptyMapped().withColor(TextColor.fromRgb(0x862F2B)))), false);
                    player.sendMessage(Text.cast(TextHelper.setStyle(TextHelper.literal("Dwell: " + Util.getReadableTimeMs(platform.getDwellTime())), Style.getEmptyMapped().withColor(TextFormatting.GOLD))), false);
                    player.sendMessage(Text.cast(TextHelper.setStyle(TextHelper.literal("Transport Type: " + platform.getTransportMode().toString().toLowerCase()), Style.getEmptyMapped().withColor(TextFormatting.GOLD))), false);
                    player.sendMessage(Text.cast(TextHelper.setStyle(TextHelper.literal("Route List: (Hover)"), new Style(Style.getEmptyMapped().data.withColor(ChatFormatting.GOLD).withUnderlined(true).withHoverEvent(hoverEvent)))), false);
                    return 1;
                })
        );
    }
}

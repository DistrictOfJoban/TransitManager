package com.lx862.mtrtm.commands;

import com.lx862.mtrtm.mixin.InitAccessorMixin;
import com.lx862.mtrtm.mixin.MainAccessorMixin;
import com.lx862.mtrtm.util.MtrUtil;
import com.lx862.mtrtm.util.Util;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.mtr.core.Main;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Position;
import org.mtr.core.data.Route;
import org.mtr.core.simulation.Simulator;
import org.mtr.mapping.holder.*;
import org.mtr.mapping.mapper.TextHelper;
import org.mtr.mod.client.MinecraftClientData;

import java.util.ArrayList;
import java.util.Comparator;

public class PlatformCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandNode<CommandSourceStack> node = dispatcher.register(Commands.literal("platform")
                .requires(ctx -> ctx.hasPermission(2))
                .executes(context -> {
                    Main tsc = InitAccessorMixin.mtrtm$getMain();
                    Simulator data = MtrUtil.getSimulator(((MainAccessorMixin)tsc).mtrtm$getSimulators(), context.getSource().getLevel());

                    Vec3 pos = context.getSource().getPosition();
                    Position tscPos = new Position((long)pos.x(), (long)pos.y(), (long)pos.z());

                    Platform platform = data.platforms.stream()
                            .filter(rail -> rail.closeTo(tscPos, 5))
                            .min(Comparator.comparingDouble((rail) -> rail.getApproximateClosestDistance(tscPos, MinecraftClientData.getInstance())))
                            .orElse(null);

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
                    context.getSource().sendSuccess(() -> Text.cast(TextHelper.setStyle(TextHelper.literal("===== Platform " + platform.getName() + " ====="), Style.getEmptyMapped().withColor(TextColor.fromRgb(0x862F2B)))).data, false);
                    context.getSource().sendSuccess(() -> Text.cast(TextHelper.setStyle(TextHelper.literal("Dwell: " + Util.getReadableTimeMs(platform.getDwellTime())), Style.getEmptyMapped().withColor(TextFormatting.GOLD))).data, false);
                    context.getSource().sendSuccess(() -> Text.cast(TextHelper.setStyle(TextHelper.literal("Transport Type: " + platform.getTransportMode().toString().toLowerCase()), Style.getEmptyMapped().withColor(TextFormatting.GOLD))).data, false);
                    context.getSource().sendSuccess(() -> Text.cast(TextHelper.setStyle(TextHelper.literal("Route List: (Hover)"), new Style(Style.getEmptyMapped().data.withColor(ChatFormatting.GOLD).withUnderlined(true).withHoverEvent(hoverEvent)))).data, false);
                    return 1;
                })
        );

        dispatcher.register(Commands.literal("platform_at")
                .requires(ctx -> ctx.hasPermission(2))
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                            .redirect(node, ctx -> ctx.getSource().withPosition(Vec3Argument.getVec3(ctx, "pos")))
                            .executes((ctx) -> {
                                return node.getCommand().run(ctx.copyFor(ctx.getSource().withPosition(Vec3Argument.getVec3(ctx, "pos"))));
                            })
                        )
        );
    }
}

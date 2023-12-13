package com.lx862.mtrtm.mixin;

import com.lx862.mtrtm.Mappings;
import com.lx862.mtrtm.MtrUtil;
import mtr.data.RailwayData;
import mtr.data.Route;
import mtr.data.Station;
import mtr.packet.PacketTrainDataGuiServer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PacketTrainDataGuiServer.class, remap = false)
public class PacketTrainDataGuiServerMixin {
    /* Format: "TheCakeIsALie;;Route|name;LastStationName;NextStationName;NextNextStationName[TL]" */
    /* If the corresponding text is inputted in train announcer, it will display a fancier next station message instead. */
    /* Deliberately undocumented as there's no customizability at the moment, will hopefully come in a future version */

    @Inject(method = "announceS2C", at = @At("HEAD"), cancellable = true)
    private static void announceS2C(ServerPlayer player, String message, ResourceLocation soundId, CallbackInfo ci) {
        if(message.startsWith("TheCakeIsALie")) {
            RailwayData data = RailwayData.getInstance(player.level);
            if(data == null) return;

            String[] splitted = message.split(";");
            String rtName = splitted[2];
            Route route = data.routes.stream().filter(rt -> rt.name.equals(rtName)).findFirst().orElse(null);
            if(route == null) return;

            final MutableComponent title = Mappings.literalText("===== " + MtrUtil.getRouteName(route.name) + " =====").setStyle(Style.EMPTY.withColor(route.color));
            player.displayClientMessage(title, false);

            for(int i = 3; i < splitted.length; i++) {
                boolean isPreviousStation = i == 3;
                boolean isNextStation = i == 4;
                boolean terminus = false;

                if(splitted[i].isEmpty()) continue;

                ChatFormatting color = isPreviousStation ? ChatFormatting.DARK_GRAY : isNextStation ? ChatFormatting.YELLOW : ChatFormatting.GRAY;
                if(splitted[i].contains("[TL]")) {
                    terminus = true;
                }

                final String queryStnName = splitted[i].replace("[TL]", "");

                Station stn = data.stations.stream().filter(st -> {
                    for(String lang : st.name.split("\\|")) {
                        if(lang.equals(queryStnName)) return true;
                    }
                    return false;
                }).findFirst().orElse(null);

                if(stn == null) continue;
                ChatFormatting forceColor = isNextStation ? null : color;
                Tuple<MutableComponent, MutableComponent> interchanges = MtrUtil.getInterchangeRouteNames(stn, route, data, forceColor);
                MutableComponent symbol = terminus ? Mappings.literalText("Ⓣ ") : Mappings.literalText("↓ ");
                // Send stn name
                player.displayClientMessage(symbol.append(Mappings.literalText(MtrUtil.getRouteName(stn.name))).withStyle(color), false);
                if(!isPreviousStation && interchanges != null) {
                    final MutableComponent interchangeChin = Mappings.literalText("可轉乘: ");
                    final MutableComponent interchangeEng = Mappings.literalText("Interchange for: ");
                    final MutableComponent chinText = Mappings.literalText("  ").append(interchangeChin.withStyle(color)).append(interchanges.getA());
                    final MutableComponent engText = Mappings.literalText("  ").append(interchangeEng.withStyle(color)).append(interchanges.getB());
                    player.displayClientMessage(chinText, false);
                    player.displayClientMessage(engText, false);
                }
            }
            ci.cancel();
        }
    }
}

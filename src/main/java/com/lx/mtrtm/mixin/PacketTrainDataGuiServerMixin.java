package com.lx.mtrtm.mixin;

import com.lx.mtrtm.Mappings;
import com.lx.mtrtm.Util;
import mtr.data.RailwayData;
import mtr.data.Route;
import mtr.data.Station;
import mtr.packet.PacketTrainDataGuiServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
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
    private static void announceS2C(ServerPlayerEntity player, String message, Identifier soundId, CallbackInfo ci) {
        if(message.startsWith("TheCakeIsALie")) {
            RailwayData data = RailwayData.getInstance(player.world);
            if(data == null) return;

            String[] splitted = message.split(";");
            String rtName = splitted[2];
            Route route = data.routes.stream().filter(rt -> rt.name.equals(rtName)).findFirst().orElse(null);
            if(route == null) return;

            final MutableText title = Mappings.literalText("===== " + Util.getRouteName(route.name) + " =====").setStyle(Style.EMPTY.withColor(route.color));
            player.sendMessage(title, false);

            for(int i = 3; i < splitted.length; i++) {
                boolean isPreviousStation = i == 3;
                boolean isNextStation = i == 4;
                boolean terminus = false;

                if(splitted[i].isEmpty()) continue;

                Formatting color = isPreviousStation ? Formatting.DARK_GRAY : isNextStation ? Formatting.YELLOW : Formatting.GRAY;
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
                Formatting forceColor = isNextStation ? null : color;
                Pair<MutableText, MutableText> interchanges = Util.getInterchangeRouteNames(stn, route, data, forceColor);
                MutableText symbol = terminus ? Mappings.literalText("Ⓣ ") : Mappings.literalText("↓ ");
                // Send stn name
                player.sendMessage(symbol.append(Mappings.literalText(Util.getRouteName(stn.name))).formatted(color), false);
                if(!isPreviousStation && interchanges != null) {
                    final MutableText interchangeChin = Mappings.literalText("可轉乘: ");
                    final MutableText interchangeEng = Mappings.literalText("Interchange for: ");
                    final MutableText chinText = Mappings.literalText("  ").append(interchangeChin.formatted(color)).append(interchanges.getLeft());
                    final MutableText engText = Mappings.literalText("  ").append(interchangeEng.formatted(color)).append(interchanges.getRight());
                    player.sendMessage(chinText, false);
                    player.sendMessage(engText, false);
                }
            }
            ci.cancel();
        }
    }
}

package com.lx862.mtrtm.util;

import com.lx862.mtrtm.commands.TrainCommand;
import com.lx862.mtrtm.data.TargetVehicle;
import com.lx862.mtrtm.mixin.InitAccessorMixin;
import com.lx862.mtrtm.mixin.MainAccessorMixin;
import com.lx862.mtrtm.mixin.SidingAccessorMixin;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import org.mtr.core.Main;
import org.mtr.core.data.Siding;
import org.mtr.core.simulation.Simulator;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Calendar;
import java.util.concurrent.CompletableFuture;

public class DepartureIndexHelper {
    public static CompletableFuture<Suggestions> suggestDepartureIndex(CommandContext<CommandSourceStack> context, SuggestionsBuilder suggestionsBuilder) {
        Main tsc = InitAccessorMixin.getMain();
        Simulator simulator = MtrUtil.getSimulator(((MainAccessorMixin)tsc).getSimulators(), context.getSource().getLevel());
        String target = suggestionsBuilder.getRemainingLowerCase();
        TargetVehicle targetVehicle;
        try {
            targetVehicle = TrainCommand.requireNearestVehicle(context);
        } catch (Exception e) {
            return suggestionsBuilder.buildFuture();
        }
        long sidingId = targetVehicle.vehicle.vehicleExtraData.getSidingId();
        Siding siding = simulator.sidingIdMap.get(sidingId);
        if(siding == null) return suggestionsBuilder.buildFuture();

        LongArrayList sidingDepartures = ((SidingAccessorMixin)(Object)siding).getDepartures();
        ObjectArrayList<String> suggestedDepartures = new ObjectArrayList<>();
        LongArrayList usedDepartureIndex = new LongArrayList();
        long tzOffset = System.currentTimeMillis() / 86400000L * 86400000L;

        ((SidingAccessorMixin)(Object)siding).getVehicles().forEach(e -> {
            usedDepartureIndex.add(e.getDepartureIndex());
        });

        if(targetVehicle.isManual) {
            suggestedDepartures.add("- -1 (MANUAL)");
        }

        int digits = String.valueOf(sidingDepartures.size()).length();

        for(int i = 0; i < sidingDepartures.size(); i++) {
            if(siding.getIsUnlimited() || !usedDepartureIndex.contains(i)) {
                long departureTime = sidingDepartures.getLong(i);
                String sign = departureTime >= (System.currentTimeMillis() % 86400000L) ? "+" : "-";
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(departureTime + tzOffset);
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);
                int second = calendar.get(Calendar.SECOND);

                suggestedDepartures.add(String.format("%s %0"+digits+"d (%02d:%02d:%02d)", sign, i, hour, minute, second));
            }
        }

        Util.formulateMatchingString(target, suggestedDepartures).forEach(suggestionsBuilder::suggest);
        return suggestionsBuilder.buildFuture();
    }
}

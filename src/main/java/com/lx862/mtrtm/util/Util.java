package com.lx862.mtrtm.util;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.phys.Vec3;
import org.mtr.core.tool.Vector;

import java.util.ArrayList;
import java.util.List;

public class Util {

    public static double getManhattenDistance(Vector pos1, Vector pos2) {
        return Math.abs(pos2.x() - pos1.x()) + Math.abs(pos2.y() - pos1.y()) + Math.abs(pos2.z() - pos1.z());
    }

    public static double getManhattenDistance(BlockPos pos1, BlockPos pos2) {
        return Math.abs(pos2.getX() - pos1.getX()) + Math.abs(pos2.getY() - pos1.getY()) + Math.abs(pos2.getZ() - pos1.getZ());
    }

    public static List<String> formulateMatchingString(String target, List<String> list) {
        if(target.trim().isEmpty()) {
            return list;
        }
        return new ArrayList<>(list.stream().filter(e -> e.toLowerCase().contains(target.toLowerCase())).toList());
    }

    public static String getReadableTimeMs(long ms) {
        double seconds = ms / 1000.0;
        double sec = seconds % 60;
        double min = (seconds / 60.0) % 60;
        double hr = (seconds / 60.0 / 60.0) % 24;
        double day = seconds / 60.0 / 60.0 / 24.0;

        if(seconds < 1) {
            return ms + "ms";
        } else if(seconds < 60) {
            return (int)Math.round(seconds) + "s";
        } else if(seconds < (60 * 60)) {
            return String.format("%dm %ds", (int)min, (int)sec);
        } else if(seconds < (60 * 60 * 24)) {
            return String.format("%dh %dm %ds", (int)hr, (int)min, (int)sec);
        } else {
            return String.format("%dd %dh %dm %ds", (int)day, (int)hr, (int)min, (int)sec);
        }
    }

    public static Vector toVector(Vec3 vec3) {
        return new Vector(vec3.x(), vec3.y(), vec3.z());
    }

    public static void sendKeyValueFeedback(CommandContext<CommandSourceStack> context, MutableComponent key, MutableComponent value) {
        context.getSource().sendSuccess(() -> key.withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)).append(value), false);
    }
}

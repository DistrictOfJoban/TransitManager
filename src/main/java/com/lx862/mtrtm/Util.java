package com.lx862.mtrtm;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public class Util {

    public static double getManhattenDistance(Vec3 pos1, Vec3 pos2) {
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
        double min = seconds / 60.0;
        double sec = seconds % 60;
        double hr = seconds / 60.0 / 60.0;
        double day = seconds / 60.0 / 60.0 / 24.0;

        if(seconds < 60) {
            return (int)Math.round(seconds) + "s";
        } else if(seconds < (60 * 60)) {
            return String.format("%dm %ds", (int)min, (int)sec);
        } else if(seconds < (60 * 60 * 24)) {
            return String.format("%dh %dm %ds", (int)hr, (int)min, (int)sec);
        } else {
            return String.format("%dd %dh %dm %ds", (int)day, (int)hr, (int)min, (int)sec);
        }
    }

    public static BlockPos getNonOccupiedPos(Level world, BlockPos targetPos) {
        BlockState state = world.getBlockState(targetPos);
        if(!state.isAir()) {
            BlockPos.MutableBlockPos mPos = targetPos.mutable();
            int offset = 0;

            while(offset < world.getHeight()) {
                offset++;

                BlockState offsetState = world.getBlockState(mPos.setY(targetPos.getY() + offset));
                if(offsetState.isAir()) {
                    break;
                }
            }
            return new BlockPos(targetPos.getX(), mPos.getY(), targetPos.getZ());
        } else {
            return targetPos;
        }
    }
}

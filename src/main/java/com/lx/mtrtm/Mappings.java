package com.lx.mtrtm;

import com.mojang.brigadier.CommandDispatcher;
#if MC_VERSION >= "11903"
    import net.minecraft.registry.entry.RegistryEntry;
    import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
    import net.minecraft.sound.SoundEvent;
#else
    import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket;
#endif

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/* Provide cross MC version methods */
public class Mappings {
    public static MutableText literalText(String content) {
        MutableText text = null;
    #if MC_VERSION >= "11900"
        text = Text.literal(content);
    #elif MC_VERSION < "11900"
        text = new net.minecraft.text.LiteralText(content);
    #endif
        return text;
    }

    public static void registerCommands(Consumer<CommandDispatcher<ServerCommandSource>> callback) {
        #if MC_VERSION >= "11900"
            net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, third) -> {
                callback.accept(dispatcher);
            });
        #elif MC_VERSION < "11900"
            net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
                callback.accept(dispatcher);
            });
        #endif
    }

    public static void sendPlaySoundIdS2CPacket(World world, ServerPlayerEntity player, Identifier sound, SoundCategory soundCategory, Vec3d pos, float volume, float pitch) {
        #if MC_VERSION >= "11903"
            player.networkHandler.sendPacket(new PlaySoundS2CPacket(RegistryEntry.of(SoundEvent.of(sound)), soundCategory, pos.x, pos.y, pos.z, volume, 1, world.getRandom().nextLong()));
        #else
            player.networkHandler.sendPacket(new PlaySoundIdS2CPacket(sound, soundCategory, pos, volume, 1, world.getRandom().nextLong()));
        #endif
    }
}

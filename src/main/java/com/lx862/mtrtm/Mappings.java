package com.lx862.mtrtm;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.function.Consumer;

/* Provide cross MC version methods */
public class Mappings {

    public static MutableComponent literalText(String content) {
        #if MC_VERSION <= "11802"
            return new net.minecraft.network.chat.TextComponent(content);
        #else
            return Component.literal(content);
        #endif
    }

    public static void registerCommands(Consumer<CommandDispatcher<CommandSourceStack>> callback) {
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
}

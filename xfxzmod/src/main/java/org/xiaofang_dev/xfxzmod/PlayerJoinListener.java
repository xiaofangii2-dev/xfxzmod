package org.xiaofang_dev.xfxzmod;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class PlayerJoinListener {
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            OpConfig.autoRegister(handler.getPlayer().getName().getString());
        });
    }
}

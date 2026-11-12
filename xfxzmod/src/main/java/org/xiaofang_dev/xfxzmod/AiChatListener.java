package org.xiaofang_dev.xfxzmod;

import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;

public class AiChatListener {
    public static void register() {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            if (AiChatManager.isEnabled(sender.getUUID())) {
                AiChatManager.ask(sender.level().getServer(), sender, message.signedContent());
            }
        });
    }
}

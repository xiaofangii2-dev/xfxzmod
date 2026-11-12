package org.xiaofang_dev.xfxzmod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class AiChatCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ai")
                .then(Commands.literal("chat")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(AiChatCommand::run))));
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        var source = ctx.getSource();
        if (!(source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)) {
            source.sendFailure(Component.literal("该命令只能由玩家执行"));
            return 0;
        }
        boolean on = BoolArgumentType.getBool(ctx, "enabled");
        AiChatManager.toggle(player.getUUID(), on);
        source.sendSuccess(() -> Component.literal("AI 聊天模式: " + (on ? "开启" : "关闭")), false);
        return 1;
    }
}

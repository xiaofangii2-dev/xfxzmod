package org.xiaofang_dev.xfxzmod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Map;

public class XfxzCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("xfxz")
                .then(Commands.literal("version").executes(XfxzCommand::version))
                .then(Commands.literal("op")
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .executes(ctx -> opAdd(ctx, true))))
                        .then(Commands.literal("rm")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .executes(ctx -> opAdd(ctx, false))))
                        .then(Commands.literal("ls").executes(XfxzCommand::opList)))
                .then(Commands.literal("plugins")
                        .then(Commands.literal("list").executes(ctx -> PluginsCommand.list(ctx)))
                        .then(Commands.literal("rm")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> PluginsCommand.rm(ctx))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> PluginsCommand.add(ctx))))));
    }

    private static int version(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal(
                "xfxzmod: " + XfxzMod.MOD_VERSION + "\nxfxzlib: " + XfxzLibVersion.VERSION), false);
        return 1;
    }

    private static int opAdd(CommandContext<CommandSourceStack> ctx, boolean op) {
        String player = StringArgumentType.getString(ctx, "player");
        OpConfig.setOp(player, op);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "玩家 " + player + " 的 OP 状态已设为: " + op), false);
        return 1;
    }

    private static int opList(CommandContext<CommandSourceStack> ctx) {
        Map<String, Boolean> all = OpConfig.all();
        if (all.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("没有 OP 记录"), false);
            return 1;
        }
        StringBuilder sb = new StringBuilder("OP 列表:\n");
        for (Map.Entry<String, Boolean> e : all.entrySet()) {
            sb.append(e.getKey()).append(": ").append(e.getValue()).append("\n");
        }
        ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }
}

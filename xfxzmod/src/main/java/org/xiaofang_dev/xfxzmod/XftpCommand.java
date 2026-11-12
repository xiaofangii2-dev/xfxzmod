package org.xiaofang_dev.xfxzmod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Collections;

public class XftpCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("xftp")
                .then(Commands.argument("target", EntityArgument.players())
                        .then(Commands.argument("dimension", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (WorldConfig.Entry e : WorldConfig.all()) builder.suggest(e.fullId());
                                    return builder.buildFuture();
                                })
                                .executes(XftpCommand::run))));
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        try {
            Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "target");
            String dimId = StringArgumentType.getString(ctx, "dimension");
            WorldConfig.Entry entry = WorldConfig.get(dimId);
            if (entry == null) {
                ctx.getSource().sendFailure(Component.literal("未知维度: " + dimId));
                return 0;
            }
            Identifier id = Identifier.tryParse(entry.fullId());
            if (id == null) {
                ctx.getSource().sendFailure(Component.literal("维度 ID 格式错误: " + entry.fullId()));
                return 0;
            }
            ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id);
            ServerLevel level = ctx.getSource().getServer().getLevel(key);
            if (level == null) {
                ctx.getSource().sendFailure(Component.literal("维度不存在或未加载: " + entry.fullId()));
                return 0;
            }
            BlockPos pos = BlockPos.containing(entry.x, entry.y, entry.z);
            for (ServerPlayer p : targets) {
                p.teleportTo(level, pos.getX() + 0.5, (double) pos.getY(), pos.getZ() + 0.5,
                        Collections.emptySet(), p.getYRot(), p.getXRot(), false);
            }
            ctx.getSource().sendSuccess(() -> Component.literal("已传送 " + targets.size() + " 名玩家到 " + entry.fullId()), true);
            return targets.size();
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("传送失败: " + e.getMessage()));
            return 0;
        }
    }
}

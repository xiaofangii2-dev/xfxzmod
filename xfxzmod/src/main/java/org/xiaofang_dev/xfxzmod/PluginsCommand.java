package org.xiaofang_dev.xfxzmod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PluginsCommand {
    private static final Path PLUGINS_DIR = Paths.get(System.getProperty("user.dir")).resolve("config/xfxz/plugins");
    private static final String BASE_URL = "https://github.com/xiaofangii2-dev/xfxzmod/releases/download/plugins/";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("plugins")
                .then(Commands.literal("list").executes(PluginsCommand::list))
                .then(Commands.literal("rm")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(PluginsCommand::rm)))
                .then(Commands.literal("add")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(PluginsCommand::add))));
    }

    public static int list(CommandContext<CommandSourceStack> ctx) {
        if (!isOp(ctx)) return 0;
        if (!Files.exists(PLUGINS_DIR)) {
            ctx.getSource().sendSuccess(() -> Component.literal("插件目录不存在"), false);
            return 1;
        }
        StringBuilder sb = new StringBuilder("插件列表:\n");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(PLUGINS_DIR, "*.xfxzpe")) {
            for (Path p : stream) {
                String name = readName(p);
                sb.append(name != null ? name : p.getFileName().toString()).append("\n");
            }
        } catch (IOException e) {
            ctx.getSource().sendFailure(Component.literal("读取失败: " + e.getMessage()));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    public static int rm(CommandContext<CommandSourceStack> ctx) {
        if (!isOp(ctx)) return 0;
        String id = StringArgumentType.getString(ctx, "id");
        Path target = PLUGINS_DIR.resolve(id + ".xfxzpe");
        try {
            if (Files.deleteIfExists(target)) {
                ctx.getSource().sendSuccess(() -> Component.literal("已删除: " + id), false);
                return 1;
            } else {
                ctx.getSource().sendFailure(Component.literal("未找到: " + id));
                return 0;
            }
        } catch (IOException e) {
            ctx.getSource().sendFailure(Component.literal("删除失败: " + e.getMessage()));
            return 0;
        }
    }

    public static int add(CommandContext<CommandSourceStack> ctx) {
        if (!isOp(ctx)) return 0;
        String id = StringArgumentType.getString(ctx, "id");
        String url = BASE_URL + id + ".xfxzpe";
        try {
            Files.createDirectories(PLUGINS_DIR);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() != 200) {
                ctx.getSource().sendFailure(Component.literal("下载失败 HTTP " + resp.statusCode()));
                return 0;
            }
            Files.write(PLUGINS_DIR.resolve(id + ".xfxzpe"), resp.body());
            ctx.getSource().sendSuccess(() -> Component.literal("已下载: " + id + "，重启后生效"), false);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("下载异常: " + e.getMessage()));
            return 0;
        }
    }

    private static boolean isOp(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer p)) {
            ctx.getSource().sendFailure(Component.literal("只能由玩家执行"));
            return false;
        }
        if (!OpConfig.isOp(p.getName().getString())) {
            ctx.getSource().sendFailure(Component.literal("你没有权限"));
            return false;
        }
        return true;
    }

    private static String readName(Path xfxzpe) {
        try (ZipFile zip = new ZipFile(xfxzpe.toFile())) {
            ZipEntry entry = zip.getEntry("META-INF/plugin.xfxzjson");
            if (entry == null) return null;
            try (InputStream in = zip.getInputStream(entry)) {
                String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                int idx = content.indexOf("\"name\"");
                if (idx < 0) return null;
                int colon = content.indexOf(':', idx);
                int q1 = content.indexOf('"', colon);
                int q2 = content.indexOf('"', q1 + 1);
                return content.substring(q1 + 1, q2);
            }
        } catch (IOException e) {
            return null;
        }
    }
}

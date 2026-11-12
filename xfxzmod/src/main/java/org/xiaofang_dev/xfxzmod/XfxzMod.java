package org.xiaofang_dev.xfxzmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

public class XfxzMod implements ModInitializer {
    public static final String MOD_ID = "xfxzmod";
    public static final String MOD_VERSION = "1.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        copyDefaultConfig();
        WorldConfig.reload();
        AiConfig.init();
        OpConfig.load();
        PlayerJoinListener.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            XftpCommand.register(dispatcher);
            AiChatCommand.register(dispatcher);
            XfxzCommand.register(dispatcher);
        });
        AiChatListener.register();
        LOGGER.info("XFXZ Mod 初始化完成，/xftp、/ai、/xfxz 已注册。");
    }

    private void copyDefaultConfig() {
        Path configRoot = FabricLoader.getInstance().getConfigDir().resolve("xfxz");
        if (Files.exists(configRoot)) return;
        try {
            Files.createDirectories(configRoot.resolve("world_config/xfxz"));
            Files.createDirectories(configRoot.resolve("ai"));
            Files.createDirectories(configRoot.resolve("user"));
            Files.createDirectories(configRoot.resolve("plugins"));
            String tpl = "# xfxz可传送维度列表\nnamespace:xfxz\nid:%s\ncoordinates: 0 64 0\n";
            for (String id : List.of("bw2", "rural", "main")) {
                Files.writeString(configRoot.resolve("world_config/xfxz/" + id + ".cfg"), String.format(tpl, id));
            }
            Files.writeString(configRoot.resolve("ai/ai.cfg"),
                    "# xfxz AI 聊天配置（仅服务器端使用）\napi_url: \napi_key: \nmodel: \n");
            Files.writeString(configRoot.resolve("user/op.ini"), "");
            LOGGER.info("已创建默认配置文件: {}", configRoot);
        } catch (IOException e) {
            LOGGER.error("创建默认配置文件失败", e);
        }
    }
}

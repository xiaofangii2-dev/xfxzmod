package org.xiaofang_dev.xfxzmod;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.*;

public class AiConfig {
    public static String apiUrl = "";
    public static String apiKey = "";
    public static String model = "";

    public static void init() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("xfxz/ai");
        Path file = dir.resolve("ai.cfg");
        try {
            Files.createDirectories(dir);
            if (!Files.exists(file)) {
                Files.writeString(file,
                        "# xfxz AI 聊天配置（仅服务器端使用）\n" +
                        "api_url: \n" +
                        "api_key: \n" +
                        "model: \n");
            }
            for (String line : Files.readAllLines(file)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int idx = line.indexOf(':');
                if (idx < 0) continue;
                String key = line.substring(0, idx).trim();
                String val = line.substring(idx + 1).trim();
                switch (key) {
                    case "api_url" -> apiUrl = val;
                    case "api_key" -> apiKey = val;
                    case "model" -> model = val;
                }
            }
        } catch (IOException e) {
            XfxzMod.LOGGER.error("读取 ai.cfg 失败", e);
        }
    }
}

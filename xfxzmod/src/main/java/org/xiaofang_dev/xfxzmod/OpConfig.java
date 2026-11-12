package org.xiaofang_dev.xfxzmod;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class OpConfig {
    private static final Map<String, Boolean> OPS = new LinkedHashMap<>();
    private static final Map<String, String> UUIDS = new LinkedHashMap<>();

    public static void load() {
        OPS.clear();
        UUIDS.clear();
        Path file = FabricLoader.getInstance().getConfigDir().resolve("xfxz/user/op.ini");
        if (!Files.exists(file)) return;
        try {
            String section = null;
            for (String line : Files.readAllLines(file)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.startsWith("[") && line.endsWith("]")) {
                    section = line.substring(1, line.length() - 1);
                    continue;
                }
                int idx = line.indexOf(':');
                if (idx < 0 || section == null) continue;
                String key = line.substring(0, idx).trim();
                String val = line.substring(idx + 1).trim();
                switch (key) {
                    case "op" -> OPS.put(section, Boolean.parseBoolean(val));
                    case "uuid" -> UUIDS.put(section, val);
                }
            }
        } catch (IOException e) {
            XfxzMod.LOGGER.error("读取 op.ini 失败", e);
        }
    }

    public static void save() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("xfxz/user/op.ini");
        try {
            Files.createDirectories(file.getParent());
            StringBuilder sb = new StringBuilder();
            for (String name : OPS.keySet()) {
                sb.append("[").append(name).append("]\n");
                sb.append("op:").append(OPS.get(name)).append("\n");
                sb.append("id:").append(name).append("\n");
                sb.append("uuid:").append(UUIDS.getOrDefault(name, genUuid())).append("\n\n");
            }
            Files.writeString(file, sb.toString());
        } catch (IOException e) {
            XfxzMod.LOGGER.error("写入 op.ini 失败", e);
        }
    }

    public static void setOp(String player, boolean op) {
        if (!OPS.containsKey(player)) {
            OPS.put(player, op);
            UUIDS.put(player, genUuid());
        } else {
            OPS.put(player, op);
        }
        save();
    }

    public static void autoRegister(String player) {
        if (OPS.containsKey(player)) return;
        OPS.put(player, false);
        UUIDS.put(player, genUuid());
        save();
    }

    public static Map<String, Boolean> all() { return OPS; }
    public static boolean isOp(String player) { return OPS.getOrDefault(player, false); }

    private static String genUuid() {
        String chars = "0123456789abcdef";
        StringBuilder sb = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < 24; i++) {
            if (i > 0 && i % 4 == 0) sb.append('-');
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }
}

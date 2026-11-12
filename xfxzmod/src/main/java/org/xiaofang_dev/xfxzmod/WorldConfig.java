package org.xiaofang_dev.xfxzmod;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class WorldConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("xfxzmod-config");

    public static class Entry {
        public final String namespace;
        public final String id;
        public final double x, y, z;
        public Entry(String namespace, String id, double x, double y, double z) {
            this.namespace = namespace; this.id = id;
            this.x = x; this.y = y; this.z = z;
        }
        public String fullId() { return namespace + ":" + id; }
    }

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    public static void reload() {
        ENTRIES.clear();
        Path root = FabricLoader.getInstance().getConfigDir().resolve("xfxz/world_config");
        if (!Files.exists(root)) return;
        try (DirectoryStream<Path> nsDirs = Files.newDirectoryStream(root, Files::isDirectory)) {
            for (Path nsDir : nsDirs) {
                String ns = nsDir.getFileName().toString();
                try (DirectoryStream<Path> cfgFiles = Files.newDirectoryStream(nsDir, "*.cfg")) {
                    for (Path cfg : cfgFiles) {
                        Entry e = parse(ns, cfg);
                        if (e != null) ENTRIES.put(e.fullId(), e);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("读取配置目录失败", e);
        }
    }

    private static Entry parse(String namespace, Path file) {
        String id = file.getFileName().toString().replace(".cfg", "");
        double x = 0, y = 64, z = 0;
        try {
            for (String line : Files.readAllLines(file)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int idx = line.indexOf(':');
                if (idx < 0) continue;
                String key = line.substring(0, idx).trim();
                String val = line.substring(idx + 1).trim();
                switch (key) {
                    case "namespace" -> namespace = val;
                    case "id" -> id = val;
                    case "coordinates" -> {
                        String[] p = val.split("\\s+");
                        if (p.length >= 3) {
                            x = Double.parseDouble(p[0]);
                            y = Double.parseDouble(p[1]);
                            z = Double.parseDouble(p[2]);
                        }
                    }
                    default -> {}
                }
            }
        } catch (Exception e) {
            LOGGER.warn("解析配置失败: " + file, e);
            return null;
        }
        return new Entry(namespace, id, x, y, z);
    }

    public static Entry get(String fullId) { return ENTRIES.get(fullId); }
    public static Collection<Entry> all() { return ENTRIES.values(); }
}

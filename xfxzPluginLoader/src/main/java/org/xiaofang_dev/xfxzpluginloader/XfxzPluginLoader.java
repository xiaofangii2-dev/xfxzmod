package org.xiaofang_dev.xfxzpluginloader;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class XfxzPluginLoader implements PreLaunchEntrypoint {
    public static final Logger LOGGER = LoggerFactory.getLogger("xfxzpluginloader");

    @Override
    public void onPreLaunch() {
        Path pluginsDir = Paths.get(System.getProperty("user.dir")).resolve("config/xfxz/plugins");
        if (!Files.exists(pluginsDir)) {
            LOGGER.info("插件目录不存在，跳过: {}", pluginsDir);
            return;
        }

        Map<String, Path> latest = new LinkedHashMap<>();
        Map<String, String> latestVersion = new LinkedHashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginsDir, "*.xfxzpe")) {
            for (Path p : stream) {
                String fileName = p.getFileName().toString();
                String id = fileName.substring(0, fileName.length() - ".xfxzpe".length());
                String version = readVersion(p);
                if (version == null) {
                    LOGGER.warn("无法读取版本，跳过: {}", fileName);
                    continue;
                }
                if (!latest.containsKey(id) || compareVersion(version, latestVersion.get(id)) > 0) {
                    latest.put(id, p);
                    latestVersion.put(id, version);
                }
            }
        } catch (IOException e) {
            LOGGER.error("扫描插件目录失败", e);
            return;
        }

        if (latest.isEmpty()) {
            LOGGER.info("没有找到 .xfxzpe 插件");
            return;
        }

        String existing = System.getProperty("fabric.addMods", "");
        StringBuilder addMods = new StringBuilder(existing);
        for (Path p : latest.values()) {
            if (addMods.length() > 0) addMods.append(java.io.File.pathSeparator);
            addMods.append(p.toAbsolutePath().toString());
        }
        System.setProperty("fabric.addMods", addMods.toString());
        LOGGER.info("已注册 {} 个插件到 fabric.addMods", latest.size());
    }

    private static String readVersion(Path xfxzpe) {
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(xfxzpe.toFile())) {
            java.util.zip.ZipEntry entry = zip.getEntry("META-INF/plugin.xfxzjson");
            if (entry == null) return null;
            try (java.io.InputStream in = zip.getInputStream(entry)) {
                String content = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                int idx = content.indexOf("\"version\"");
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

    private static int compareVersion(String a, String b) {
        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        int len = Math.max(pa.length, pb.length);
        for (int i = 0; i < len; i++) {
            int na = i < pa.length ? parseIntSafe(pa[i]) : 0;
            int nb = i < pb.length ? parseIntSafe(pb[i]) : 0;
            if (na != nb) return Integer.compare(na, nb);
        }
        return 0;
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }
}

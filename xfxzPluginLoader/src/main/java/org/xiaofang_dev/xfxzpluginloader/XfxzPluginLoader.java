package org.xiaofang_dev.xfxzpluginloader;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class XfxzPluginLoader implements PreLaunchEntrypoint {
    public static final Logger LOGGER = LoggerFactory.getLogger("xfxzpluginloader");

    @Override
    public void onPreLaunch() {
        reload();
    }

    public static boolean reload() {
        Path root = Paths.get(System.getProperty("user.dir"));
        Path pluginsDir = root.resolve("config/xfxz/plugins");
        Path modsDir = root.resolve("mods");
        if (!Files.exists(pluginsDir)) return false;

        Map<String, Path> latest = new LinkedHashMap<>();
        Map<String, String> versions = new LinkedHashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginsDir, "*.xfxzpe")) {
            for (Path p : stream) {
                String fileName = p.getFileName().toString();
                String id = fileName.substring(0, fileName.length() - ".xfxzpe".length());
                String version = readMetaField(p, "version");
                if (version == null) continue;
                if (!latest.containsKey(id) || compare(version, versions.get(id)) > 0) {
                    latest.put(id, p);
                    versions.put(id, version);
                }
            }
        } catch (IOException e) {
            LOGGER.error("扫描插件目录失败", e);
            return false;
        }
        if (latest.isEmpty()) return false;

        try {
            Files.createDirectories(modsDir);
        } catch (IOException e) {
            LOGGER.error("创建 mods 目录失败", e);
            return false;
        }

        Path out = modsDir.resolve("xfxzplugins.jar");
        try {
            buildBundle(latest, out);
            LOGGER.info("已生成插件包: {}", out.getFileName());
            return true;
        } catch (IOException ex) {
            LOGGER.error("生成插件包失败", ex);
            return false;
        }
    }

    private static void buildBundle(Map<String, Path> plugins, Path out) throws IOException {
        InputStream shell = XfxzPluginLoader.class.getResourceAsStream("/plugin.jar");
        if (shell == null) {
            LOGGER.error("找不到 plugin.jar 资源");
            return;
        }

        try (ZipInputStream zin = new ZipInputStream(shell);
             ZipOutputStream zout = new ZipOutputStream(Files.newOutputStream(out))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                if (entry.getName().equals("fabric.mod.json")) {
                    String json = new String(zin.readAllBytes(), StandardCharsets.UTF_8);
                    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                    JsonArray jars = new JsonArray();
                    for (String id : plugins.keySet()) {
                        JsonObject jarObj = new JsonObject();
                        jarObj.addProperty("file", "META-INF/jars/" + id + ".jar");
                        jars.add(jarObj);
                    }
                    obj.add("jars", jars);
                    zout.putNextEntry(new ZipEntry("fabric.mod.json"));
                    zout.write(obj.toString().getBytes(StandardCharsets.UTF_8));
                    zout.closeEntry();
                    continue;
                }
                zout.putNextEntry(new ZipEntry(entry.getName()));
                if (!entry.isDirectory()) zin.transferTo(zout);
                zout.closeEntry();
            }

            for (Map.Entry<String, Path> e : plugins.entrySet()) {
                ZipEntry nested = new ZipEntry("META-INF/jars/" + e.getKey() + ".jar");
                zout.putNextEntry(nested);
                zout.write(Files.readAllBytes(e.getValue()));
                zout.closeEntry();
            }
        }
    }

    private static JsonObject readMeta(Path xfxzpe) {
        try (ZipFile zip = new ZipFile(xfxzpe.toFile())) {
            ZipEntry entry = zip.getEntry("assets/plugin.xfxzjson");
            if (entry == null) entry = zip.getEntry("META-INF/plugin.xfxzjson");
            if (entry == null) return null;
            try (InputStream in = zip.getInputStream(entry)) {
                return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static String readMetaField(Path xfxzpe, String field) {
        JsonObject meta = readMeta(xfxzpe);
        return meta != null && meta.has(field) ? meta.get(field).getAsString() : null;
    }

    private static int compare(String a, String b) {
        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        int len = Math.max(pa.length, pb.length);
        for (int i = 0; i < len; i++) {
            int na = i < pa.length ? parseInt(pa[i]) : 0;
            int nb = i < pb.length ? parseInt(pb[i]) : 0;
            if (na != nb) return Integer.compare(na, nb);
        }
        return 0;
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
}

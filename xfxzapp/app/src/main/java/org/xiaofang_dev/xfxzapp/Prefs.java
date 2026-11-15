package org.xiaofang_dev.xfxzapp;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class Prefs {
    private static final String FILE_NAME = "xfxz.properties";
    private static final String KEY_IP = "ip";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_DEV = "dev_mode";
    private static final String KEY_WATCHDOG_TPS = "watchdog_tps_enabled";
    private static final String KEY_WATCHDOG_TPS_THRESHOLD = "watchdog_tps_threshold";
    private static final String KEY_WATCHDOG_MSPT = "watchdog_mspt_enabled";
    private static final String KEY_WATCHDOG_MSPT_THRESHOLD = "watchdog_mspt_threshold";
    private static final String KEY_UPDATE_INTERVAL = "update_interval";

    private static Properties load(Context ctx) {
        Properties p = new Properties();
        File f = new File(ctx.getExternalFilesDir(null), FILE_NAME);
        if (f.exists()) {
            try (FileInputStream in = new FileInputStream(f)) {
                p.load(in);
            } catch (Exception ignored) {}
        }
        return p;
    }

    private static void save(Context ctx, Properties p) {
        File f = new File(ctx.getExternalFilesDir(null), FILE_NAME);
        try (FileOutputStream out = new FileOutputStream(f)) {
            p.store(out, null);
        } catch (Exception ignored) {}
    }

    public static void saveConnection(Context ctx, String ip, String token) {
        Properties p = load(ctx);
        p.setProperty(KEY_IP, ip);
        p.setProperty(KEY_TOKEN, token);
        save(ctx, p);
    }

    public static String getIp(Context ctx) { return load(ctx).getProperty(KEY_IP, ""); }
    public static String getToken(Context ctx) { return load(ctx).getProperty(KEY_TOKEN, ""); }

    public static boolean isDevMode(Context ctx) {
        return Boolean.parseBoolean(load(ctx).getProperty(KEY_DEV, "false"));
    }
    public static void setDevMode(Context ctx, boolean v) {
        Properties p = load(ctx);
        p.setProperty(KEY_DEV, String.valueOf(v));
        save(ctx, p);
    }

    public static boolean isTpsWatchdogEnabled(Context ctx) {
        return Boolean.parseBoolean(load(ctx).getProperty(KEY_WATCHDOG_TPS, "false"));
    }
    public static void setTpsWatchdogEnabled(Context ctx, boolean v) {
        Properties p = load(ctx);
        p.setProperty(KEY_WATCHDOG_TPS, String.valueOf(v));
        save(ctx, p);
    }
    public static int getTpsThreshold(Context ctx) {
        try { return Integer.parseInt(load(ctx).getProperty(KEY_WATCHDOG_TPS_THRESHOLD, "15")); }
        catch (Exception e) { return 15; }
    }
    public static void setTpsThreshold(Context ctx, int v) {
        Properties p = load(ctx);
        p.setProperty(KEY_WATCHDOG_TPS_THRESHOLD, String.valueOf(v));
        save(ctx, p);
    }

    public static boolean isMsptWatchdogEnabled(Context ctx) {
        return Boolean.parseBoolean(load(ctx).getProperty(KEY_WATCHDOG_MSPT, "false"));
    }
    public static void setMsptWatchdogEnabled(Context ctx, boolean v) {
        Properties p = load(ctx);
        p.setProperty(KEY_WATCHDOG_MSPT, String.valueOf(v));
        save(ctx, p);
    }
    public static int getMsptThreshold(Context ctx) {
        try { return Integer.parseInt(load(ctx).getProperty(KEY_WATCHDOG_MSPT_THRESHOLD, "50")); }
        catch (Exception e) { return 50; }
    }
    public static void setMsptThreshold(Context ctx, int v) {
        Properties p = load(ctx);
        p.setProperty(KEY_WATCHDOG_MSPT_THRESHOLD, String.valueOf(v));
        save(ctx, p);
    }

    public static int getUpdateInterval(Context ctx) {
        try { return Integer.parseInt(load(ctx).getProperty(KEY_UPDATE_INTERVAL, "5")); }
        catch (Exception e) { return 5; }
    }
    public static void setUpdateInterval(Context ctx, int v) {
        Properties p = load(ctx);
        p.setProperty(KEY_UPDATE_INTERVAL, String.valueOf(v));
        save(ctx, p);
    }
}

package org.xiaofang_dev.xfxzapp;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class LangManager {
    private static final String FILE_NAME = "lang.properties";
    
    public static boolean isEn(Context ctx) {
        Properties p = new Properties();
        File f = new File(ctx.getExternalFilesDir(null), FILE_NAME);
        if (f.exists()) {
            try (FileInputStream in = new FileInputStream(f)) { p.load(in); } catch (Exception ignored) {}
        }
        return "en".equals(p.getProperty("lang", "zh"));
    }

    public static void setLang(Context ctx, boolean en) {
        Properties p = new Properties();
        p.setProperty("lang", en ? "en" : "zh");
        File f = new File(ctx.getExternalFilesDir(null), FILE_NAME);
        try (FileOutputStream out = new FileOutputStream(f)) { p.store(out, null); } catch (Exception ignored) {}
    }
}

package org.xiaofang_dev.xfxzapp;

import android.app.Application;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class XfxzApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                File dir = getExternalFilesDir(null);
                if (dir != null) {
                    File log = new File(dir, "crash.log");
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    pw.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
                    pw.println("Thread: " + thread.getName());
                    throwable.printStackTrace(pw);
                    pw.flush();
                    FileWriter fw = new FileWriter(log, true);
                    fw.write(sw.toString());
                    fw.write("\n\n");
                    fw.close();
                }
            } catch (Exception ignored) {}
            System.exit(1);
        });
    }
}

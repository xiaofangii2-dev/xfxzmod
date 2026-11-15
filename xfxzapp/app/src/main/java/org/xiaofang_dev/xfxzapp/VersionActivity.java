package org.xiaofang_dev.xfxzapp;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class VersionActivity extends AppCompatActivity {
    private int tapCount = 0;
    private long lastTap = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_version);

        TextView tvAppVersion = findViewById(R.id.tvAppVersion);
        tvAppVersion.setText("APP 版本: " + getAppVersion());

        tvAppVersion.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            if (now - lastTap > 1500) tapCount = 0;
            lastTap = now;
            tapCount++;
            if (tapCount >= 6) {
                tapCount = 0;
                Prefs.setDevMode(this, true);
                Toast.makeText(this, "开发者模式已开启", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getAppVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0.0";
        }
    }
}

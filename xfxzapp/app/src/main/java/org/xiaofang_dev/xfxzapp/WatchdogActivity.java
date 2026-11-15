package org.xiaofang_dev.xfxzapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class WatchdogActivity extends AppCompatActivity {
    private TextView tvTpsStatus, tvMsptStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watchdog);

        tvTpsStatus = findViewById(R.id.tvTpsStatus);
        tvMsptStatus = findViewById(R.id.tvMsptStatus);

        refresh();

        findViewById(R.id.btnTpsToggle).setOnClickListener(v -> {
            Prefs.setTpsWatchdogEnabled(this, !Prefs.isTpsWatchdogEnabled(this));
            refresh();
        });
        findViewById(R.id.btnTpsThreshold).setOnClickListener(v -> {
            EditText input = new EditText(this);
            input.setText(String.valueOf(Prefs.getTpsThreshold(this)));
            new AlertDialog.Builder(this)
                    .setTitle("TPS 阈值")
                    .setView(input)
                    .setPositiveButton("保存", (d, w) -> {
                        try {
                            Prefs.setTpsThreshold(this, Integer.parseInt(input.getText().toString()));
                            refresh();
                        } catch (Exception ignored) {}
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        findViewById(R.id.btnMsptToggle).setOnClickListener(v -> {
            Prefs.setMsptWatchdogEnabled(this, !Prefs.isMsptWatchdogEnabled(this));
            refresh();
        });
        findViewById(R.id.btnMsptThreshold).setOnClickListener(v -> {
            EditText input = new EditText(this);
            input.setText(String.valueOf(Prefs.getMsptThreshold(this)));
            new AlertDialog.Builder(this)
                    .setTitle("MSPT 阈值（毫秒）")
                    .setView(input)
                    .setPositiveButton("保存", (d, w) -> {
                        try {
                            Prefs.setMsptThreshold(this, Integer.parseInt(input.getText().toString()));
                            refresh();
                        } catch (Exception ignored) {}
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    private void refresh() {
        tvTpsStatus.setText("TPS 看门狗: " + (Prefs.isTpsWatchdogEnabled(this) ? "已开启" : "已关闭")
                + "\n阈值: " + Prefs.getTpsThreshold(this));
        tvMsptStatus.setText("MSPT 看门狗: " + (Prefs.isMsptWatchdogEnabled(this) ? "已开启" : "已关闭")
                + "\n阈值: " + Prefs.getMsptThreshold(this) + " ms");
    }
}

package org.xiaofang_dev.xfxzapp;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.JsonObject;

public class ServerActivity extends AppCompatActivity {
    private static final String CHANNEL_ID = "watchdog";
    private static final int NOTIFY_TPS = 1001;
    private static final int NOTIFY_MSPT = 1002;

    private ApiClient api;
    private boolean authorized = false;
    private TextView tvVersion, tvPlayers, tvUptime, tvTps, tvMspt, tvMemory;
    private EditText etRcon;
    private TextView tvRconResult;
    private LinearLayout pageHome, pageRcon;
    private ScrollView pageSettings;
    private long lastTpsNotify = 0;
    private long lastMsptNotify = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        String ip = getIntent().getStringExtra("ip");
        String token = getIntent().getStringExtra("token");
        if (ip == null || token == null) { finish(); return; }

        createChannel();

        tvVersion = findViewById(R.id.tvVersion);
        tvPlayers = findViewById(R.id.tvPlayers);
        tvUptime = findViewById(R.id.tvUptime);
        tvTps = findViewById(R.id.tvTps);
        tvMspt = findViewById(R.id.tvMspt);
        tvMemory = findViewById(R.id.tvMemory);
        etRcon = findViewById(R.id.etRcon);
        tvRconResult = findViewById(R.id.tvRconResult);
        pageHome = findViewById(R.id.pageHome);
        pageRcon = findViewById(R.id.pageRcon);
        pageSettings = findViewById(R.id.pageSettings);

        findViewById(R.id.btnRconSend).setOnClickListener(v -> sendRcon());
        findViewById(R.id.btnGithub).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/xiaofangii2-dev/xfxzmod"))));
        findViewById(R.id.btnVersion).setOnClickListener(v ->
                startActivity(new Intent(this, VersionActivity.class)));
        findViewById(R.id.btnWatchdogEntry).setOnClickListener(v ->
                startActivity(new Intent(this, WatchdogActivity.class)));
        findViewById(R.id.btnDevMode).setOnClickListener(v -> openDevDialog());

        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setOnItemSelectedListener(item -> {
            pageHome.setVisibility(item.getItemId() == R.id.navHome ? View.VISIBLE : View.GONE);
            pageRcon.setVisibility(item.getItemId() == R.id.navRcon ? View.VISIBLE : View.GONE);
            pageSettings.setVisibility(item.getItemId() == R.id.navSettings ? View.VISIBLE : View.GONE);
            return true;
        });

        refreshDevVisibility();

        api = new ApiClient(ip, token);
        api.fetchStatus(d -> { authorized = true; update(d); api.connectWebSocket(this::update, msg -> {}); },
                msg -> Toast.makeText(this, "连接失败: " + msg, Toast.LENGTH_SHORT).show());
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "看门狗", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(ch);
        }
    }

    private void notifyWatchdog(int id, String title, String text) {
        Intent i = new Intent(this, ServerActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(id, b.build());
    }

    private void refreshDevVisibility() {
        boolean dev = Prefs.isDevMode(this);
        findViewById(R.id.btnDevMode).setVisibility(dev ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnWatchdogEntry).setVisibility(dev ? View.VISIBLE : View.GONE);
    }

    private void openDevDialog() {
        new AlertDialog.Builder(this)
                .setTitle("开发者模式")
                .setMessage("关闭后隐藏开发者选项。")
                .setPositiveButton("关闭", (d, w) -> {
                    Prefs.setDevMode(this, false);
                    refreshDevVisibility();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void update(JsonObject d) {
        double tps = d.get("tps").getAsDouble();
        double mspt = d.get("mspt").getAsDouble();

        tvVersion.setText("版本: " + d.get("version").getAsString());
        tvPlayers.setText("玩家: " + d.get("players").getAsInt() + "/" + d.get("maxPlayers").getAsInt());
        tvUptime.setText("运行: " + (d.get("uptimeMs").getAsLong() / 1000) + " 秒");
        tvTps.setText("TPS: " + tps);
        tvMspt.setText("MSPT: " + mspt + " ms");
        long used = d.get("memoryUsed").getAsLong() / 1048576;
        long max = d.get("memoryMax").getAsLong() / 1048576;
        tvMemory.setText("内存: " + used + " / " + max + " MB");

        long now = System.currentTimeMillis();
        if (Prefs.isTpsWatchdogEnabled(this)) {
            int threshold = Prefs.getTpsThreshold(this);
            if (tps < threshold && now - lastTpsNotify > 60000) {
                lastTpsNotify = now;
                notifyWatchdog(NOTIFY_TPS, "TPS 看门狗警告", "TPS 低于阈值: " + tps + " < " + threshold);
                new AlertDialog.Builder(this)
                        .setTitle("TPS 看门狗警告")
                        .setMessage("TPS 低于阈值: " + tps + " < " + threshold)
                        .setPositiveButton("知道了", null)
                        .show();
            }
        }
        if (Prefs.isMsptWatchdogEnabled(this)) {
            int threshold = Prefs.getMsptThreshold(this);
            if (mspt > threshold && now - lastMsptNotify > 60000) {
                lastMsptNotify = now;
                notifyWatchdog(NOTIFY_MSPT, "MSPT 看门狗警告", "MSPT 超过阈值: " + mspt + " > " + threshold);
                new AlertDialog.Builder(this)
                        .setTitle("MSPT 看门狗警告")
                        .setMessage("MSPT 超过阈值: " + mspt + " > " + threshold)
                        .setPositiveButton("知道了", null)
                        .show();
            }
        }
    }

    private void sendRcon() {
        if (!authorized || api == null) {
            Toast.makeText(this, "token 未配对，无法执行", Toast.LENGTH_SHORT).show();
            return;
        }
        String cmd = etRcon.getText().toString().trim();
        if (cmd.isEmpty()) return;
        api.sendRcon(cmd, new ApiClient.RconCallback() {
            @Override public void onResult(JsonObject json) {
                if (json.has("ok") && json.get("ok").getAsBoolean()) {
                    String out = json.has("output") ? json.get("output").getAsString() : "";
                    tvRconResult.setText(out.isEmpty() ? "(无输出)" : out);
                } else {
                    String err = json.has("error") ? json.get("error").getAsString() : "未知错误";
                    tvRconResult.setText("执行失败: " + err);
                }
            }
            @Override public void onError(String msg) {
                tvRconResult.setText("请求错误: " + msg);
            }
        });
        etRcon.setText("");
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDevVisibility();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (api != null) api.close();
    }
}

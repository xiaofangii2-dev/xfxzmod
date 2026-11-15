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
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class ServerActivity extends AppCompatActivity {
    private static final String CHANNEL_ID = "watchdog";
    private static final int NOTIFY_TPS = 1001;
    private static final int NOTIFY_MSPT = 1002;

    private ApiClient api;
    private boolean authorized = false;
    private TextView tvVersion, tvPlayers, tvUptime, tvTps, tvMspt, tvMemory;
    private EditText etRcon;
    private TextView tvRconResult;
    private LinearLayout pageHome, pageRcon, pagePlugin;
    private FrameLayout pagePlayers;
    private ScrollView pageSettings;
    private long lastTpsNotify = 0;
    private long lastMsptNotify = 0;
    private String ip, token;

    private ListView lvPlugins;
    private List<Plugin> pluginList = new ArrayList<>();
    private PluginAdapter pluginAdapter;

    private ListView lvPlayers;
    private List<String> playerList = new ArrayList<>();
    private PlayerAdapter playerAdapter;
    private TextView tvNoPlayers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        ip = getIntent().getStringExtra("ip");
        token = getIntent().getStringExtra("token");
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
        pagePlugin = findViewById(R.id.pagePlugin);
        pagePlayers = findViewById(R.id.pagePlayers);
        pageSettings = findViewById(R.id.pageSettings);
        
        lvPlugins = findViewById(R.id.lvPlugins);
        pluginAdapter = new PluginAdapter();
        lvPlugins.setAdapter(pluginAdapter);

        lvPlayers = findViewById(R.id.lvPlayers);
        playerAdapter = new PlayerAdapter();
        lvPlayers.setAdapter(playerAdapter);
        tvNoPlayers = findViewById(R.id.tvNoPlayers);

        findViewById(R.id.btnRconSend).setOnClickListener(v -> sendRcon());
        
        findViewById(R.id.btnPluginAdd).setOnClickListener(v -> {
            Intent i = new Intent(ServerActivity.this, StoreActivity.class);
            i.putExtra("ip", ip);
            i.putExtra("token", token);
            startActivity(i);
        });
        
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
            pagePlayers.setVisibility(item.getItemId() == R.id.navPlayers ? View.VISIBLE : View.GONE);
            pagePlugin.setVisibility(item.getItemId() == R.id.navPlugin ? View.VISIBLE : View.GONE);
            pageRcon.setVisibility(item.getItemId() == R.id.navRcon ? View.VISIBLE : View.GONE);
            pageSettings.setVisibility(item.getItemId() == R.id.navSettings ? View.VISIBLE : View.GONE);
            if (item.getItemId() == R.id.navPlugin) loadPlugins();
            return true;
        });

        refreshDevVisibility();

        api = new ApiClient(ip, token);
        api.fetchStatus(d -> { authorized = true; update(d); api.connectWebSocket(this::update, msg -> {}); },
                msg -> Toast.makeText(this, "连接失败: " + msg, Toast.LENGTH_SHORT).show());
    }

    private void loadPlugins() {
        if (!authorized || api == null) return;
        api.fetchPlugins(new ApiClient.PluginsCallback() {
            @Override public void onList(List<Plugin> plugins) {
                pluginList.clear();
                pluginList.addAll(plugins);
                pluginAdapter.notifyDataSetChanged();
            }
            @Override public void onError(String msg) {
                Toast.makeText(ServerActivity.this, "获取插件失败: " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class PlayerAdapter extends BaseAdapter {
        @Override public int getCount() { return playerList.size(); }
        @Override public Object getItem(int position) { return playerList.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) convertView = getLayoutInflater().inflate(R.layout.item_player, parent, false);
            String p = playerList.get(position);
            TextView tvName = convertView.findViewById(R.id.tvPlayerName);
            TextView btnMenu = convertView.findViewById(R.id.btnPlayerMenu);

            tvName.setText(p);

            btnMenu.setOnClickListener(v -> {
                String[] options = {"踢出", "给OP", "清除OP"};
                new AlertDialog.Builder(ServerActivity.this)
                        .setTitle(p)
                        .setItems(options, (d, which) -> {
                            String cmd = "";
                            if (which == 0) cmd = "kick " + p;
                            else if (which == 1) cmd = "op " + p;
                            else if (which == 2) cmd = "deop " + p;
                            
                            api.sendRcon(cmd, new ApiClient.RconCallback() {
                                @Override public void onResult(JsonObject json) {
                                    Toast.makeText(ServerActivity.this, "执行成功: " + options[which], Toast.LENGTH_SHORT).show();
                                }
                                @Override public void onError(String msg) {
                                    Toast.makeText(ServerActivity.this, "执行失败", Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .show();
            });
            return convertView;
        }
    }

    private class PluginAdapter extends BaseAdapter {
        @Override public int getCount() { return pluginList.size(); }
        @Override public Object getItem(int position) { return pluginList.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) convertView = getLayoutInflater().inflate(R.layout.item_plugin, parent, false);
            Plugin p = pluginList.get(position);
            TextView tvName = convertView.findViewById(R.id.tvName);
            TextView tvVersion = convertView.findViewById(R.id.tvVersion);
            TextView tvStatus = convertView.findViewById(R.id.tvStatus);
            TextView btnMenu = convertView.findViewById(R.id.btnMenu);

            tvName.setText(p.name);
            tvVersion.setText("版本: " + p.version);
            tvStatus.setText("状态: " + p.status);

            btnMenu.setOnClickListener(v -> {
                new AlertDialog.Builder(ServerActivity.this)
                        .setTitle(p.name)
                        .setItems(new String[]{"卸载"}, (d, which) -> deletePlugin(p.id))
                        .show();
            });
            return convertView;
        }
    }

    private void deletePlugin(String id) {
        api.deletePlugin(id, msg -> {
            if ("OK".equals(msg)) {
                Toast.makeText(this, "已删除，重启后生效", Toast.LENGTH_SHORT).show();
                loadPlugins();
            } else {
                Toast.makeText(this, "删除失败: " + msg, Toast.LENGTH_SHORT).show();
            }
        });
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

        if (d.has("playerList")) {
            String names = d.get("playerList").getAsString();
            playerList.clear();
            if (!names.isEmpty()) {
                for (String n : names.split(",")) {
                    playerList.add(n);
                }
            }
            playerAdapter.notifyDataSetChanged();
            
            if (playerList.isEmpty()) {
                lvPlayers.setVisibility(View.GONE);
                tvNoPlayers.setVisibility(View.VISIBLE);
            } else {
                lvPlayers.setVisibility(View.VISIBLE);
                tvNoPlayers.setVisibility(View.GONE);
            }
        }

        long now = System.currentTimeMillis();
        if (Prefs.isTpsWatchdogEnabled(this)) {
            int threshold = Prefs.getTpsThreshold(this);
            if (tps < threshold && now - lastTpsNotify > 60000) {
                lastTpsNotify = now;
                notifyWatchdog(NOTIFY_TPS, "TPS 看门狗警告", "TPS 低于阈值: " + tps + " < " + threshold);
            }
        }
        if (Prefs.isMsptWatchdogEnabled(this)) {
            int threshold = Prefs.getMsptThreshold(this);
            if (mspt > threshold && now - lastMsptNotify > 60000) {
                lastMsptNotify = now;
                notifyWatchdog(NOTIFY_MSPT, "MSPT 看门狗警告", "MSPT 超过阈值: " + mspt + " > " + threshold);
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

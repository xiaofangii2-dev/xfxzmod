package org.xiaofang_dev.xfxzapp;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ServerActivity extends AppCompatActivity {
    private static final String CHANNEL_ID = "watchdog";
    private static final int NOTIFY_TPS = 1001;
    private static final int NOTIFY_MSPT = 1002;

    private ApiClient api;
    private boolean authorized = false;
    private boolean isEn = false;
    
    private TextView tvVersion, tvPlayers, tvUptime, tvTps, tvMspt, tvMemory;
    private TextView tvTitleTps, tvTitleMspt, tvTitlePlayers, tvTitleVersion, tvTitleUptime, tvTitleMemory;
    private EditText etRcon;
    private TextView tvRconResult;
    private LinearLayout pageRcon, pagePlugin;
    private FrameLayout pagePlayers;
    private ScrollView pageHome, pageSettings;
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
        isEn = LangManager.isEn(this);
        setContentView(R.layout.activity_server);

        ip = getIntent().getStringExtra("ip");
        if (ip == null) ip = Prefs.getIp(this);
        token = getIntent().getStringExtra("token");
        if (token == null) token = Prefs.getToken(this);
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
        
        tvTitleTps = findViewById(R.id.tvTitleTps);
        tvTitleMspt = findViewById(R.id.tvTitleMspt);
        tvTitlePlayers = findViewById(R.id.tvTitlePlayers);
        tvTitleVersion = findViewById(R.id.tvTitleVersion);
        tvTitleUptime = findViewById(R.id.tvTitleUptime);
        tvTitleMemory = findViewById(R.id.tvTitleMemory);

        lvPlugins = findViewById(R.id.lvPlugins);
        pluginAdapter = new PluginAdapter();
        lvPlugins.setAdapter(pluginAdapter);

        lvPlayers = findViewById(R.id.lvPlayers);
        playerAdapter = new PlayerAdapter();
        lvPlayers.setAdapter(playerAdapter);
        tvNoPlayers = findViewById(R.id.tvNoPlayers);

        if (isEn) {
            tvTitleTps.setText("TPS");
            tvTitleMspt.setText("MSPT");
            tvTitlePlayers.setText("Players");
            tvTitleVersion.setText("Version");
            tvTitleUptime.setText("Uptime");
            tvTitleMemory.setText("Memory");
            tvNoPlayers.setText("No Players");
            etRcon.setHint("Enter command");
            ((Button)findViewById(R.id.btnRconSend)).setText("Send");
            ((Button)findViewById(R.id.btnPluginAdd)).setText("Download");
            ((TextView)findViewById(R.id.btnGithub)).setText("About");
            ((TextView)findViewById(R.id.btnVersion)).setText("Version");
            ((TextView)findViewById(R.id.btnOther)).setText("Other");
            ((TextView)findViewById(R.id.btnDevMode)).setText("Close Developer Mode");
            ((TextView)findViewById(R.id.btnWatchdogEntry)).setText("Watchdog");
            
            BottomNavigationView nav = findViewById(R.id.bottomNav);
            nav.getMenu().findItem(R.id.navHome).setTitle("Home");
            nav.getMenu().findItem(R.id.navPlayers).setTitle("Players");
            nav.getMenu().findItem(R.id.navPlugin).setTitle("Plugins");
            nav.getMenu().findItem(R.id.navRcon).setTitle("RCON");
            nav.getMenu().findItem(R.id.navSettings).setTitle("Settings");
        } else {
            tvTitleTps.setText("TPS");
            tvTitleMspt.setText("MSPT");
            tvTitlePlayers.setText("在线玩家");
            tvTitleVersion.setText("服务器版本");
            tvTitleUptime.setText("运行时长");
            tvTitleMemory.setText("内存占用");
            tvNoPlayers.setText("没有玩家");
            etRcon.setHint("输入命令");
            ((Button)findViewById(R.id.btnRconSend)).setText("发送");
            ((Button)findViewById(R.id.btnPluginAdd)).setText("下载插件");
            ((TextView)findViewById(R.id.btnGithub)).setText("关于");
            ((TextView)findViewById(R.id.btnVersion)).setText("版本");
            ((TextView)findViewById(R.id.btnOther)).setText("其他");
            ((TextView)findViewById(R.id.btnDevMode)).setText("关闭开发者模式");
            ((TextView)findViewById(R.id.btnWatchdogEntry)).setText("看门狗");
            
            BottomNavigationView nav = findViewById(R.id.bottomNav);
            nav.getMenu().findItem(R.id.navHome).setTitle("主页");
            nav.getMenu().findItem(R.id.navPlayers).setTitle("玩家");
            nav.getMenu().findItem(R.id.navPlugin).setTitle("插件");
            nav.getMenu().findItem(R.id.navRcon).setTitle("RCON");
            nav.getMenu().findItem(R.id.navSettings).setTitle("设置");
        }

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
        findViewById(R.id.btnOther).setOnClickListener(v -> 
                startActivity(new Intent(this, OtherActivity.class)));
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
        resetStats();

        api = new ApiClient(ip, token, Prefs.getUpdateInterval(this));
        api.fetchStatus(d -> {
            authorized = true;
            update(d);
            api.startPolling(this::update, msg -> {});
        }, msg -> {
            resetStats();
            api.startPolling(this::update, err -> {});
        });
    }

    private void resetStats() {
        tvVersion.setText("0");
        tvPlayers.setText("0/0");
        tvUptime.setText("00:00:00");
        tvTps.setText("0.0");
        tvMspt.setText("0.0");
        tvMemory.setText("0 / 0 MB");
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
                Toast.makeText(ServerActivity.this, "Error", Toast.LENGTH_SHORT).show();
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
                String[] options = isEn ? new String[]{"Kick", "Give OP", "Remove OP"} : new String[]{"踢出", "给OP", "清除OP"};
                new AlertDialog.Builder(ServerActivity.this)
                        .setTitle(p)
                        .setItems(options, (d, which) -> {
                            String cmd = "";
                            if (which == 0) cmd = "kick " + p;
                            else if (which == 1) cmd = "op " + p;
                            else if (which == 2) cmd = "deop " + p;
                            
                            api.sendRcon(cmd, new ApiClient.RconCallback() {
                                @Override public void onResult(JsonObject json) {
                                    Toast.makeText(ServerActivity.this, "Success", Toast.LENGTH_SHORT).show();
                                }
                                @Override public void onError(String msg) {
                                    Toast.makeText(ServerActivity.this, "Failed", Toast.LENGTH_SHORT).show();
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
            tvVersion.setText((isEn ? "Version: " : "版本: ") + p.version);
            tvStatus.setText((isEn ? "Status: Enabled" : "状态: 已启用"));

            btnMenu.setOnClickListener(v -> {
                new AlertDialog.Builder(ServerActivity.this)
                        .setTitle(p.name)
                        .setItems(isEn ? new String[]{"Uninstall"} : new String[]{"卸载"}, (d, which) -> deletePlugin(p.id))
                        .show();
            });
            return convertView;
        }
    }

    private void deletePlugin(String id) {
        api.deletePlugin(id, msg -> {
            if ("OK".equals(msg)) {
                Toast.makeText(this, isEn ? "Success, wait for reboot" : "已删除，重启后生效", Toast.LENGTH_SHORT).show();
                loadPlugins();
            } else {
                Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void update(JsonObject d) {
        double tps = d.get("tps").getAsDouble();
        double mspt = d.get("mspt").getAsDouble();

        tvVersion.setText(d.get("version").getAsString());
        tvPlayers.setText(d.get("players").getAsInt() + "/" + d.get("maxPlayers").getAsInt());
        
        long upSec = d.get("uptimeMs").getAsLong() / 1000;
        long days = upSec / 86400;
        long hours = (upSec % 86400) / 3600;
        long mins = (upSec % 3600) / 60;
        long secs = upSec % 60;
        
        String timeStr;
        if (days > 0) {
            timeStr = isEn ? String.format(Locale.US, "%d d %02d:%02d:%02d", days, hours, mins, secs) 
                           : String.format(Locale.US, "%d 天 %02d:%02d:%02d", days, hours, mins, secs);
        } else {
            timeStr = String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs);
        }
        tvUptime.setText(timeStr);
        
        tvTps.setText(String.format("%.2f", tps));
        tvMspt.setText(String.format("%.2f", mspt));
        
        long used = d.get("memoryUsed").getAsLong() / 1048576;
        long max = d.get("memoryMax").getAsLong() / 1048576;
        tvMemory.setText(used + " / " + max + " MB");

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
                notifyWatchdog(NOTIFY_TPS, isEn ? "TPS Warning" : "TPS 看门狗警告", "TPS low: " + tps + " < " + threshold);
            }
        }
        if (Prefs.isMsptWatchdogEnabled(this)) {
            int threshold = Prefs.getMsptThreshold(this);
            if (mspt > threshold && now - lastMsptNotify > 60000) {
                lastMsptNotify = now;
                notifyWatchdog(NOTIFY_MSPT, isEn ? "MSPT Warning" : "MSPT 看门狗警告", "MSPT high: " + mspt + " > " + threshold);
            }
        }
    }

    private void sendRcon() {
        if (!authorized || api == null) {
            Toast.makeText(this, isEn ? "No connection" : "未连接", Toast.LENGTH_SHORT).show();
            return;
        }
        String cmd = etRcon.getText().toString().trim();
        if (cmd.isEmpty()) return;
        api.sendRcon(cmd, new ApiClient.RconCallback() {
            @Override public void onResult(JsonObject json) {
                if (json.has("ok") && json.get("ok").getAsBoolean()) {
                    String out = json.has("output") ? json.get("output").getAsString() : "";
                    tvRconResult.setText(out.isEmpty() ? "(No output)" : out);
                } else {
                    String err = json.has("error") ? json.get("error").getAsString() : "Unknown error";
                    tvRconResult.setText("Error: " + err);
                }
            }
            @Override public void onError(String msg) {
                tvRconResult.setText("Request error: " + msg);
            }
        });
        etRcon.setText("");
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Watchdog", NotificationManager.IMPORTANCE_HIGH);
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
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_alert, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvAlertTitle);
        TextView tvMsg = dialogView.findViewById(R.id.tvAlertMsg);
        Button btnCancel = dialogView.findViewById(R.id.btnAlertCancel);
        Button btnOk = dialogView.findViewById(R.id.btnAlertOk);

        tvTitle.setText(isEn ? "Developer Mode" : "开发者模式");
        tvMsg.setText(isEn ? "Close to hide dev options." : "关闭后隐藏开发者选项。");
        btnCancel.setText(isEn ? "Cancel" : "取消");
        btnOk.setText(isEn ? "Close" : "关闭");

        AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.80), ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnOk.setOnClickListener(v -> {
            Prefs.setDevMode(this, false);
            refreshDevVisibility();
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isEn != LangManager.isEn(this)) {
            recreate();
        } else {
            refreshDevVisibility();
            if (authorized && api != null) {
                api.fetchStatus(this::update, msg -> {});
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (api != null) api.close();
    }
}

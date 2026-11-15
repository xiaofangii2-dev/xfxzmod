package org.xiaofang_dev.xfxzapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import rikka.shizuku.Shizuku;

public class SetupActivity extends AppCompatActivity {
    private static final int REQ_NOTIF = 1;
    private static final int REQ_SHIZUKU = 2;
    private TextView status;
    private Button next;
    private boolean jumped = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        status = findViewById(R.id.tvStatus);
        next = findViewById(R.id.btnNext);
        next.setEnabled(false);
        next.setOnClickListener(v -> {
            startActivity(new Intent(this, ConnectActivity.class));
            finish();
        });
        
        requestNotifications();
        requestShizuku();
    }

    private void requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
        } else {
            updateStatus();
        }
    }

    private void requestShizuku() {
        try {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                updateStatus();
                return;
            }
            Shizuku.requestPermission(REQ_SHIZUKU);
        } catch (Exception e) {
            updateStatus();
        }
    }

    private boolean hasFileAccess() {
        if (Build.VERSION.SDK_INT >= 30) {
            return Environment.isExternalStorageManager();
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestFileAccess() {
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            i.setData(Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } else if (Build.VERSION.SDK_INT < 30 && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 3);
        }
    }

    private void updateStatus() {
        boolean notif = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED;
        boolean shizuku = false;
        try {
            shizuku = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Exception ignored) {}
        boolean files = hasFileAccess();
        
        if (status != null) {
            status.setText("通知权限: " + (notif ? "已授权" : "未授权") +
                    "\nShizuku 权限: " + (shizuku ? "已授权" : "未授权") +
                    "\n文件访问权限: " + (files ? "已授权" : "未授权"));
        }
        
        if (!files) requestFileAccess();
        
        if (next != null) {
            next.setEnabled(notif && shizuku && files);
        }

        String ip = Prefs.getIp(this);
        String token = Prefs.getToken(this);
        if (!ip.isEmpty() && !token.isEmpty() && notif && files && !jumped) {
            jumped = true;
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent i = new Intent(this, ServerActivity.class);
                i.putExtra("ip", ip);
                i.putExtra("token", token);
                startActivity(i);
                finish();
            }, 500);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        updateStatus();
    }
}

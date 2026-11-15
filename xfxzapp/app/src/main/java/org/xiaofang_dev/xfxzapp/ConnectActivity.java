package org.xiaofang_dev.xfxzapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ConnectActivity extends AppCompatActivity {
    private EditText etIp, etToken;
    private Button btnConnect;
    private boolean isEn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isEn = LangManager.isEn(this);
        setContentView(R.layout.activity_connect);
        etIp = findViewById(R.id.etIp);
        etToken = findViewById(R.id.etToken);
        btnConnect = findViewById(R.id.btnConnect);

        etIp.setText(Prefs.getIp(this));
        etToken.setText(Prefs.getToken(this));
        
        if (isEn) {
            etIp.setHint("Server IP");
            etToken.setHint("Token");
            btnConnect.setText("Connect");
        } else {
            etIp.setHint("服务器 IP 或地址");
            etToken.setHint("Token");
            btnConnect.setText("连接");
        }

        btnConnect.setOnClickListener(v -> connect());
    }

    private void connect() {
        String ip = etIp.getText().toString().trim();
        String token = etToken.getText().toString().trim();
        if (ip.isEmpty() || token.isEmpty()) {
            Toast.makeText(this, isEn ? "IP and Token required" : "IP 和 token 都要填", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!ip.startsWith("http")) ip = "http://" + ip;
        ApiClient candidate = new ApiClient(ip, token, Prefs.getUpdateInterval(this));
        String finalIp = ip;
        
        btnConnect.setEnabled(false);
        btnConnect.setText(isEn ? "Connecting..." : "连接中...");

        candidate.fetchStatus(d -> {
            Prefs.saveConnection(this, finalIp, token);
            Intent i = new Intent(this, ServerActivity.class);
            i.putExtra("ip", finalIp);
            i.putExtra("token", token);
            startActivity(i);
            finish();
        }, msg -> {
            btnConnect.setEnabled(true);
            btnConnect.setText(isEn ? "Connect" : "连接");
            Toast.makeText(this, (isEn ? "Failed: " : "连接失败: ") + msg, Toast.LENGTH_SHORT).show();
        });
    }
}

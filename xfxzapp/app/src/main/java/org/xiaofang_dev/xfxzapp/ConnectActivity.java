package org.xiaofang_dev.xfxzapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ConnectActivity extends AppCompatActivity {
    private EditText etIp, etToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        etIp = findViewById(R.id.etIp);
        etToken = findViewById(R.id.etToken);
        etIp.setText(Prefs.getIp(this));
        etToken.setText(Prefs.getToken(this));
        Button btn = findViewById(R.id.btnConnect);
        btn.setOnClickListener(v -> connect());
    }

    private void connect() {
        String ip = etIp.getText().toString().trim();
        String token = etToken.getText().toString().trim();
        if (ip.isEmpty() || token.isEmpty()) {
            Toast.makeText(this, "IP 和 token 都要填", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!ip.startsWith("http")) ip = "http://" + ip;
        ApiClient candidate = new ApiClient(ip, token);
        String finalIp = ip;
        candidate.fetchStatus(d -> {
            Prefs.saveConnection(this, finalIp, token);
            Intent i = new Intent(this, ServerActivity.class);
            i.putExtra("ip", finalIp);
            i.putExtra("token", token);
            startActivity(i);
            finish();
        }, msg -> Toast.makeText(this, "连接失败: " + msg, Toast.LENGTH_SHORT).show());
    }
}

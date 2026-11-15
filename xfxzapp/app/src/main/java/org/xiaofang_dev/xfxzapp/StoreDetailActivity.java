package org.xiaofang_dev.xfxzapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class StoreDetailActivity extends AppCompatActivity {
    private String pluginId;
    private String baseUrl;
    private String token;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_detail);

        pluginId = getIntent().getStringExtra("id");
        String ip = getIntent().getStringExtra("ip");
        token = getIntent().getStringExtra("token");
        baseUrl = ip.endsWith("/") ? ip.substring(0, ip.length() - 1) : ip;

        TextView tvName = findViewById(R.id.tvDetailName);
        Button btnDownload = findViewById(R.id.btnDownload);

        tvName.setText(pluginId);

        btnDownload.setOnClickListener(v -> downloadPlugin());
    }

    private void downloadPlugin() {
        showToast("服务端开始下载 " + pluginId);
        
        RequestBody body = new FormBody.Builder().add("id", pluginId).build();
        Request req = new Request.Builder()
                .url(baseUrl + "/api/plugins/download")
                .header("Authorization", "Bearer " + token)
                .post(body)
                .build();

        OkHttpClient client = StoreActivity.getUnsafeOkHttpClient();
        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> showToast("请求失败: " + e.getMessage()));
            }
            @Override public void onResponse(Call call, Response response) {
                mainHandler.post(() -> {
                    if (response.isSuccessful()) {
                        showToast("服务端下载成功！\n请使用 /xfxz reload 重载");
                    } else {
                        showToast("服务端下载失败 HTTP: " + response.code());
                    }
                });
            }
        });
    }

    private void showToast(String msg) {
        Toast t = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        t.setGravity(Gravity.BOTTOM, 0, 200);
        t.show();
    }
}

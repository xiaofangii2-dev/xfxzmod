package org.xiaofang_dev.xfxzapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class StoreActivity extends AppCompatActivity {
    private ListView lvStore;
    private List<StorePlugin> list = new ArrayList<>();
    private StoreAdapter adapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String ip, token;

    public static class StorePlugin {
        public String id;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);
        lvStore = findViewById(R.id.lvStore);
        
        ip = getIntent().getStringExtra("ip");
        token = getIntent().getStringExtra("token");

        adapter = new StoreAdapter();
        lvStore.setAdapter(adapter);

        lvStore.setOnItemClickListener((parent, view, position, id) -> {
            StorePlugin p = list.get(position);
            Intent intent = new Intent(StoreActivity.this, StoreDetailActivity.class);
            intent.putExtra("id", p.id);
            intent.putExtra("ip", ip);
            intent.putExtra("token", token);
            startActivity(intent);
        });

        String storeUrl = "https://github.com/xiaofangii2-dev/xfxzmod/releases/download/plugins/store.json";
        
        OkHttpClient client = getUnsafeOkHttpClient();

        Request req = new Request.Builder().url(storeUrl).build();
        
        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> showToast("获取失败: " + e.getMessage()));
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (response.isSuccessful() && body != null) {
                        String json = body.string();
                        Type listType = new TypeToken<List<StorePlugin>>(){}.getType();
                        List<StorePlugin> result = new Gson().fromJson(json, listType);
                        mainHandler.post(() -> {
                            list.clear();
                            if (result != null) list.addAll(result);
                            adapter.notifyDataSetChanged();
                        });
                    } else {
                        mainHandler.post(() -> showToast("HTTP 错误: " + response.code()));
                    }
                }
            }
        });
    }

    private void showToast(String msg) {
        Toast t = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        t.setGravity(Gravity.BOTTOM, 0, 200);
        t.show();
    }

    public static OkHttpClient getUnsafeOkHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {}
                    @Override public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {}
                    @Override public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[]{}; }
                }
            };
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private class StoreAdapter extends BaseAdapter {
        @Override public int getCount() { return list.size(); }
        @Override public Object getItem(int position) { return list.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) convertView = getLayoutInflater().inflate(R.layout.item_store, parent, false);
            StorePlugin p = list.get(position);
            TextView tvName = convertView.findViewById(R.id.tvStoreName);
            tvName.setText(p.id);
            return convertView;
        }
    }
}

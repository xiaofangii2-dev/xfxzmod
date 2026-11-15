package org.xiaofang_dev.xfxzapp;

import android.os.Handler;
import android.os.Looper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    private final OkHttpClient client;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final String baseUrl;
    private final String token;
    private final int intervalSec;
    private boolean polling = false;
    private Runnable pollTask;

    public interface StatusCallback { void onStatus(JsonObject json); }
    public interface ErrorCallback { void onError(String msg); }
    public interface RconCallback { void onResult(JsonObject json); void onError(String msg); }
    public interface PluginsCallback { void onList(List<Plugin> plugins); void onError(String msg); }

    public ApiClient(String baseUrl, String token, int intervalSec) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.token = token;
        this.intervalSec = intervalSec;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    public void fetchStatus(StatusCallback ok, ErrorCallback err) {
        Request req = new Request.Builder().url(baseUrl + "/api/status").header("Authorization", "Bearer " + token).build();
        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { main.post(() -> err.onError(e.getMessage())); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) { main.post(() -> err.onError("HTTP " + response.code())); return; }
                    JsonObject json = JsonParser.parseString(body.string()).getAsJsonObject();
                    main.post(() -> ok.onStatus(json));
                }
            }
        });
    }

    // 使用轮询代替 WebSocket，这样可以动态修改刷新时间
    public void startPolling(StatusCallback ok, ErrorCallback err) {
        polling = true;
        doPoll(ok, err);
    }

    private void doPoll(StatusCallback ok, ErrorCallback err) {
        if (!polling) return;
        fetchStatus(json -> {
            ok.onStatus(json);
            if (polling) {
                pollTask = () -> doPoll(ok, err);
                main.postDelayed(pollTask, intervalSec * 1000L);
            }
        }, msg -> {
            err.onError(msg);
            if (polling) {
                pollTask = () -> doPoll(ok, err);
                main.postDelayed(pollTask, intervalSec * 1000L);
            }
        });
    }

    public void fetchPlugins(PluginsCallback cb) {
        Request req = new Request.Builder().url(baseUrl + "/api/plugins").header("Authorization", "Bearer " + token).build();
        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { main.post(() -> cb.onError(e.getMessage())); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) { main.post(() -> cb.onError("HTTP " + response.code())); return; }
                    Type listType = new TypeToken<List<Plugin>>(){}.getType();
                    List<Plugin> list = new Gson().fromJson(body.string(), listType);
                    main.post(() -> cb.onList(list));
                } catch (Exception e) { main.post(() -> cb.onError(e.getMessage())); }
            }
        });
    }

    public void deletePlugin(String id, ErrorCallback cb) {
        Request req = new Request.Builder().url(baseUrl + "/api/plugins/delete?id=" + id).header("Authorization", "Bearer " + token).post(RequestBody.create("", null)).build();
        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { main.post(() -> cb.onError(e.getMessage())); }
            @Override public void onResponse(Call call, Response response) { main.post(() -> cb.onError("OK")); }
        });
    }

    public void sendRcon(String cmd, RconCallback cb) {
        Request req = new Request.Builder().url(baseUrl + "/api/rcon").header("Authorization", "Bearer " + token)
                .post(RequestBody.create(cmd, MediaType.parse("text/plain"))).build();
        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { main.post(() -> cb.onError(e.getMessage())); }
            @Override public void onResponse(Call call, Response response) {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) { main.post(() -> cb.onError("HTTP " + response.code())); return; }
                    JsonObject json = JsonParser.parseString(body.string()).getAsJsonObject();
                    main.post(() -> cb.onResult(json));
                } catch (Exception e) { main.post(() -> cb.onError(e.getMessage())); }
            }
        });
    }

    public void close() {
        polling = false;
        if (pollTask != null) main.removeCallbacks(pollTask);
    }
}

package org.xiaofang_dev.xfxzapp;

import android.os.Handler;
import android.os.Looper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final String baseUrl;
    private final String token;
    private WebSocket ws;

    public interface StatusCallback { void onStatus(JsonObject json); }
    public interface ErrorCallback { void onError(String msg); }
    public interface RconCallback {
        void onResult(JsonObject json);
        void onError(String msg);
    }

    public ApiClient(String baseUrl, String token) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.token = token;
    }

    public void fetchStatus(StatusCallback ok, ErrorCallback err) {
        Request req = new Request.Builder()
                .url(baseUrl + "/api/status")
                .header("Authorization", "Bearer " + token)
                .build();
        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, java.io.IOException e) { main.post(() -> err.onError(e.getMessage())); }
            @Override public void onResponse(Call call, Response response) throws java.io.IOException {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) { main.post(() -> err.onError("HTTP " + response.code())); return; }
                    JsonObject json = JsonParser.parseString(body.string()).getAsJsonObject();
                    main.post(() -> ok.onStatus(json));
                }
            }
        });
    }

    public void connectWebSocket(StatusCallback ok, ErrorCallback err) {
        String wsUrl = baseUrl.replaceFirst("^http", "ws") + "/ws";
        Request req = new Request.Builder().url(wsUrl).build();
        ws = client.newWebSocket(req, new WebSocketListener() {
            @Override public void onMessage(WebSocket webSocket, String text) {
                try {
                    JsonObject json = JsonParser.parseString(text).getAsJsonObject();
                    main.post(() -> ok.onStatus(json));
                } catch (Exception ignored) {}
            }
            @Override public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                main.post(() -> err.onError(t.getMessage()));
            }
        });
    }

    public void sendRcon(String cmd, RconCallback cb) {
        Request req = new Request.Builder()
                .url(baseUrl + "/api/rcon")
                .header("Authorization", "Bearer " + token)
                .post(RequestBody.create(cmd, MediaType.parse("text/plain")))
                .build();
        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, java.io.IOException e) { main.post(() -> cb.onError(e.getMessage())); }
            @Override public void onResponse(Call call, Response response) {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) { main.post(() -> cb.onError("HTTP " + response.code())); return; }
                    JsonObject json = JsonParser.parseString(body.string()).getAsJsonObject();
                    main.post(() -> cb.onResult(json));
                } catch (Exception e) {
                    main.post(() -> cb.onError(e.getMessage()));
                }
            }
        });
    }

    public void close() {
        if (ws != null) ws.close(1000, "bye");
    }
}

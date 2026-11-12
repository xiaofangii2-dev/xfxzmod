package org.xiaofang_dev.xfxzmod;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AiChatManager {
    private static final Set<UUID> ENABLED = ConcurrentHashMap.newKeySet();
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static boolean toggle(UUID uuid, boolean on) {
        if (on) return ENABLED.add(uuid);
        ENABLED.remove(uuid);
        return false;
    }

    public static boolean isEnabled(UUID uuid) { return ENABLED.contains(uuid); }

    public static void ask(MinecraftServer server, ServerPlayer player, String message) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("model", AiConfig.model);
            JsonArray messages = new JsonArray();
            JsonObject msg = new JsonObject();
            msg.addProperty("role", "user");
            msg.addProperty("content", message);
            messages.add(msg);
            body.add("messages", messages);

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(AiConfig.apiUrl))
                    .header("Content-Type", "application/json");
            if (!AiConfig.apiKey.isEmpty()) {
                builder.header("Authorization", "Bearer " + AiConfig.apiKey);
            }
            HttpRequest req = builder
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();

            CLIENT.sendAsync(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                    .thenAccept(resp -> {
                        String reply;
                        try {
                            JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
                            reply = json.getAsJsonArray("choices").get(0).getAsJsonObject()
                                    .getAsJsonObject("message").get("content").getAsString();
                        } catch (Exception e) {
                            reply = "解析回复失败: " + e.getMessage() + " | 原始响应: " + resp.body();
                        }
                        final String r = reply;
                        server.execute(() -> player.sendSystemMessage(Component.literal("AI: " + r)));
                    })
                    .exceptionally(ex -> {
                        server.execute(() -> player.sendSystemMessage(Component.literal("AI 请求失败: " + ex.getMessage())));
                        return null;
                    });
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("AI 请求异常: " + e.getMessage()));
        }
    }
}

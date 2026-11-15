package org.xiaofang_dev.xfpanel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PanelServer {
    private final MinecraftServer mcServer;
    private final int port;
    private Server jetty;
    private static long startTime = System.currentTimeMillis();
    private static final Set<Session> SESSIONS = ConcurrentHashMap.newKeySet();
    private static final ScheduledExecutorService PUSH = Executors.newSingleThreadScheduledExecutor();
    public static String token = "";

    public PanelServer(MinecraftServer mcServer, int port) {
        this.mcServer = mcServer;
        this.port = port;
    }

    public void start() {
        loadOrCreateToken();
        try {
            jetty = new Server(port);
            ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.SESSIONS);
            ctx.setContextPath("/");
            JettyWebSocketServletContainerInitializer.configure(ctx, null);
            ctx.addServlet(new ServletHolder(new StatusServlet()), "/api/status");
            ctx.addServlet(new ServletHolder(new RconServlet()), "/api/rcon");
            ctx.addServlet(new ServletHolder(new PluginsServlet()), "/api/plugins");
            ctx.addServlet(new ServletHolder(new PluginDeleteServlet()), "/api/plugins/delete");
            ctx.addServlet(new ServletHolder(new PluginDownloadServlet()), "/api/plugins/download");
            ctx.addServlet(new ServletHolder(new WsServlet()), "/ws");
            jetty.setHandler(ctx);
            jetty.start();
            PUSH.scheduleAtFixedRate(this::pushStatus, 5, 5, TimeUnit.SECONDS);
            XfPanel.LOGGER.info("XfPanel 已启动: http://0.0.0.0:{}", port);
        } catch (Exception e) {
            XfPanel.LOGGER.error("启动面板失败", e);
        }
    }

    public void stop() {
        try {
            PUSH.shutdownNow();
            if (jetty != null) jetty.stop();
        } catch (Exception ignored) {}
    }

    private void loadOrCreateToken() {
        Path file = Paths.get(System.getProperty("user.dir")).resolve("config/xfxz/panel.cfg");
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                String t = UUID.randomUUID().toString().replace("-", "");
                Files.writeString(file, "token: " + t + "\n");
                token = t;
            } else {
                for (String line : Files.readAllLines(file)) {
                    if (line.startsWith("token:")) token = line.substring(6).trim();
                }
            }
        } catch (IOException e) {
            token = UUID.randomUUID().toString().replace("-", "");
        }
    }

    private boolean checkToken(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        return auth != null && auth.equals("Bearer " + token);
    }

    private String statusJson() {
        float mspt = getMspt();
        double tps = mspt > 0 ? Math.min(20.0, 1000.0 / mspt) : 20.0;
        StringBuilder players = new StringBuilder();
        for (String name : mcServer.getPlayerNames()) {
            if (players.length() > 0) players.append(",");
            players.append(name);
        }
        return "{" +
                "\"version\":\"" + esc(mcServer.getServerVersion()) + "\"," +
                "\"players\":" + mcServer.getPlayerCount() + "," +
                "\"maxPlayers\":" + mcServer.getMaxPlayers() + "," +
                "\"uptimeMs\":" + (System.currentTimeMillis() - startTime) + "," +
                "\"tps\":" + String.format("%.2f", tps) + "," +
                "\"mspt\":" + String.format("%.2f", mspt) + "," +
                "\"memoryUsed\":" + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) + "," +
                "\"memoryMax\":" + Runtime.getRuntime().maxMemory() + "," +
                "\"playerList\":\"" + esc(players.toString()) + "\"" +
                "}";
    }

    private void pushStatus() {
        if (SESSIONS.isEmpty()) return;
        String msg = statusJson();
        for (Session s : SESSIONS) {
            if (s.isOpen()) {
                try { s.getRemote().sendString(msg); } catch (Exception ignored) { SESSIONS.remove(s); }
            } else SESSIONS.remove(s);
        }
    }

    private float getMspt() {
        try { return mcServer.getCurrentSmoothedTickTime(); } catch (Throwable e) { return 50.0f; }
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private class StatusServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            if (!checkToken(req)) { resp.sendError(401); return; }
            resp.setContentType("application/json; charset=utf-8");
            resp.getOutputStream().write(statusJson().getBytes(StandardCharsets.UTF_8));
        }
    }

    private class PluginsServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            if (!checkToken(req)) { resp.sendError(401); return; }
            Path pluginsDir = Paths.get(System.getProperty("user.dir")).resolve("config/xfxz/plugins");
            JsonArray arr = new JsonArray();
            if (Files.exists(pluginsDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginsDir, "*.xfxzpe")) {
                    for (Path p : stream) {
                        JsonObject meta = readMeta(p);
                        if (meta != null) {
                            JsonObject item = new JsonObject();
                            item.addProperty("id", meta.has("id") ? meta.get("id").getAsString() : p.getFileName().toString().replace(".xfxzpe", ""));
                            item.addProperty("name", meta.has("name") ? meta.get("name").getAsString() : "Unknown");
                            item.addProperty("version", meta.has("version") ? meta.get("version").getAsString() : "0.0.0");
                            item.addProperty("status", "已启用");
                            arr.add(item);
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            resp.setContentType("application/json; charset=utf-8");
            resp.getOutputStream().write(arr.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private class PluginDeleteServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            if (!checkToken(req)) { resp.sendError(401); return; }
            String id = req.getParameter("id");
            boolean deleted = false;
            if (id != null && !id.isEmpty()) {
                Path pluginsDir = Paths.get(System.getProperty("user.dir")).resolve("config/xfxz/plugins");
                if (Files.exists(pluginsDir)) {
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginsDir, "*.xfxzpe")) {
                        for (Path p : stream) {
                            JsonObject meta = readMeta(p);
                            String fileId = (meta != null && meta.has("id")) ? meta.get("id").getAsString() : p.getFileName().toString().replace(".xfxzpe", "");
                            if (fileId.equalsIgnoreCase(id)) {
                                try {
                                    Files.delete(p);
                                    deleted = true;
                                } catch (Exception e) { e.printStackTrace(); }
                                break;
                            }
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
            resp.setContentType("application/json; charset=utf-8");
            resp.getOutputStream().write(("{\"ok\":" + deleted + "}").getBytes(StandardCharsets.UTF_8));
        }
    }

    private class PluginDownloadServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            if (!checkToken(req)) { resp.sendError(401); return; }
            String id = req.getParameter("id");
            boolean ok = false;
            if (id != null && !id.isEmpty()) {
                try {
                    Path pluginsDir = Paths.get(System.getProperty("user.dir")).resolve("config/xfxz/plugins");
                    Files.createDirectories(pluginsDir);
                    Path target = pluginsDir.resolve(id + ".xfxzpe");

                    String url = "https://github.com/xiaofangii2-dev/xfxzmod/releases/download/plugins/" + id + ".xfxzpe";
                    
                    // 绕过证书校验的 HttpClient
                    TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() { return null; }
                            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                        }
                    };
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

                    HttpClient client = HttpClient.newBuilder()
                            .sslContext(sslContext)
                            .followRedirects(HttpClient.Redirect.ALWAYS)
                            .build();

                    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                    HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(target));

                    if (response.statusCode() == 200) {
                        ok = true;
                        XfPanel.LOGGER.info("插件下载成功: {}", id);
                    } else {
                        Files.deleteIfExists(target);
                        XfPanel.LOGGER.error("插件下载失败 HTTP: {}", response.statusCode());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            resp.setContentType("application/json; charset=utf-8");
            resp.getOutputStream().write(("{\"ok\":" + ok + "}").getBytes(StandardCharsets.UTF_8));
        }
    }

    private static JsonObject readMeta(Path file) {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            ZipEntry entry = zip.getEntry("assets/plugin.xfxzjson");
            if (entry == null) entry = zip.getEntry("META-INF/plugin.xfxzjson");
            if (entry == null) return null;
            try (InputStream in = zip.getInputStream(entry)) {
                return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            }
        } catch (Exception e) { return null; }
    }

    private class RconServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            if (!checkToken(req)) { resp.sendError(401); return; }
            String cmd = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            CompletableFuture<String> future = new CompletableFuture<>();
            mcServer.execute(() -> {
                try {
                    List<String> lines = new ArrayList<>();
                    CommandSource captureSource = new CommandSource() {
                        @Override public void sendSystemMessage(Component message) { lines.add(message.getString()); }
                        @Override public boolean acceptsSuccess() { return true; }
                        @Override public boolean acceptsFailure() { return true; }
                        @Override public boolean shouldInformAdmins() { return false; }
                    };
                    mcServer.getCommands().performPrefixedCommand(mcServer.createCommandSourceStack().withSource(captureSource), cmd);
                    future.complete("{\"ok\":true,\"output\":\"" + esc(String.join("\n", lines)) + "\"}");
                } catch (Exception e) {
                    future.complete("{\"ok\":false,\"error\":\"" + esc(e.getMessage()) + "\"}");
                }
            });
            try { resp.getOutputStream().write(future.get(10, TimeUnit.SECONDS).getBytes(StandardCharsets.UTF_8)); }
            catch (Exception e) { resp.getOutputStream().write("{\"ok\":false,\"error\":\"timeout\"}".getBytes(StandardCharsets.UTF_8)); }
        }
    }

    private class WsServlet extends JettyWebSocketServlet {
        @Override protected void configure(JettyWebSocketServletFactory factory) { factory.register(WsEndpoint.class); }
    }

    @WebSocket
    public static class WsEndpoint {
        @OnWebSocketConnect public void onConnect(Session session) { SESSIONS.add(session); }
        @OnWebSocketClose public void onClose(Session session, int statusCode, String reason) { SESSIONS.remove(session); }
        @OnWebSocketError public void onError(Session session, Throwable error) { SESSIONS.remove(session); }
    }
}

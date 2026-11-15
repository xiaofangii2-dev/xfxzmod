package org.xiaofang_dev.xfpanel;

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
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

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
        Path file = Paths.get("config/xfxz/panel.cfg");
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
        return "{" +
                "\"version\":\"" + esc(mcServer.getServerVersion()) + "\"," +
                "\"players\":" + mcServer.getPlayerCount() + "," +
                "\"maxPlayers\":" + mcServer.getMaxPlayers() + "," +
                "\"uptimeMs\":" + (System.currentTimeMillis() - startTime) + "," +
                "\"tps\":" + String.format("%.2f", tps) + "," +
                "\"mspt\":" + String.format("%.2f", mspt) + "," +
                "\"memoryUsed\":" + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) + "," +
                "\"memoryMax\":" + Runtime.getRuntime().maxMemory() +
                "}";
    }

    private void pushStatus() {
        if (SESSIONS.isEmpty()) return;
        String msg = statusJson();
        for (Session s : SESSIONS) {
            try { s.getRemote().sendString(msg); } catch (Exception ignored) {}
        }
    }

    private float getMspt() {
        try { return mcServer.getCurrentSmoothedTickTime(); }
        catch (Throwable e) { return 50.0f; }
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private class StatusServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            if (!checkToken(req)) { resp.sendError(401); return; }
            byte[] body = statusJson().getBytes(StandardCharsets.UTF_8);
            resp.setContentType("application/json; charset=utf-8");
            resp.getOutputStream().write(body);
        }
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
                        @Override
                        public void sendSystemMessage(Component message) {
                            System.out.println("CAPTURE: " + message.getString());
                            System.out.println("RCON capture: " + message.getString());
                            lines.add(message.getString());
                        }

                        @Override
                        public boolean acceptsSuccess() {
                            return true;
                        }

                        @Override
                        public boolean acceptsFailure() {
                            return true;
                        }

                        @Override
                        public boolean shouldInformAdmins() {
                            return false;
                        }
                    };
                    CommandSourceStack base = mcServer.createCommandSourceStack();
                    CommandSourceStack capture = base.withSource(captureSource);
                    mcServer.getCommands().performPrefixedCommand(capture, cmd);
                    future.complete("{\"ok\":true,\"output\":\"" + esc(String.join("\n", lines)) + "\"}");
                } catch (Exception e) {
                    future.complete("{\"ok\":false,\"error\":\"" + esc(e.getMessage()) + "\"}");
                }
            });
            String json;
            try {
                json = future.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                json = "{\"ok\":false,\"error\":\"timeout\"}";
            }
            resp.setContentType("application/json; charset=utf-8");
            resp.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    private class WsServlet extends JettyWebSocketServlet {
        @Override
        protected void configure(JettyWebSocketServletFactory factory) {
            factory.register(WsEndpoint.class);
        }
    }

    @WebSocket
    public static class WsEndpoint {
        @OnWebSocketConnect
        public void onConnect(Session session) {
            SESSIONS.add(session);
        }
        @OnWebSocketClose
        public void onClose(Session session, int statusCode, String reason) {
            SESSIONS.remove(session);
        }
    }
}

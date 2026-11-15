package org.xiaofang_dev.xfpanel;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XfPanel implements ModInitializer {
    public static final String MOD_ID = "xfpanel";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final int PORT = 18923;

    private static PanelServer server;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(mcServer -> {
            server = new PanelServer(mcServer, PORT);
            server.start();
            LOGGER.info("XfPanel 已启动: http://127.0.0.1:{}", PORT);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(mcServer -> {
            if (server != null) {
                server.stop();
                LOGGER.info("XfPanel 已停止");
            }
        });
    }
}

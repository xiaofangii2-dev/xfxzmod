package org.xiaofang_dev.xfxzmod;

import net.minecrell.terminalconsole.SimpleTerminalConsole;
import net.minecraft.server.MinecraftServer;

public class XfxzConsole extends SimpleTerminalConsole {
    private final MinecraftServer server;

    public XfxzConsole(MinecraftServer server) {
        this.server = server;
    }

    @Override
    protected boolean isRunning() {
        return server.isRunning();
    }

    @Override
    protected void runCommand(String command) {
        server.execute(() -> server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack(), command));
    }

    @Override
    protected void shutdown() {
        server.halt(false);
    }
}

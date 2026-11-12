package org.xiaofang_dev.xfxzmod.mixin;

import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DedicatedServer.class)
public class DedicatedServerMixin {

    @Redirect(method = "initServer", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;start()V"))
    private void xfxzmod$replaceConsoleThread(Thread thread) {
        if ("Server console handler".equals(thread.getName())) {
            org.xiaofang_dev.xfxzmod.XfxzMod.LOGGER.info("DedicatedServerMixin: 拦截原版控制台线程，改用 XfxzConsole");
            // 拿 DedicatedServer 实例：this 就是 mixin 注入的实例
            DedicatedServer server = (DedicatedServer) (Object) this;
            org.xiaofang_dev.xfxzmod.XfxzConsole console = new org.xiaofang_dev.xfxzmod.XfxzConsole(server);
            Thread t = new Thread(console::start, "XFXZ Console");
            t.setDaemon(true);
            t.start();
        } else {
            thread.start();
        }
    }
}

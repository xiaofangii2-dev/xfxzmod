package org.xiaofang_dev.xfxzlib;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XfxzLib implements ModInitializer {
    public static final String MOD_ID = "xfxzlib";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("XFXZ Lib 初始化完成（原生库占位，未实际加载）。");
    }
}

package com.furina.dontgethurt;

import com.furina.dontgethurt.event.PlayerDamageHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DontGetHurt implements ModInitializer {
    public static final String MOD_ID = "dont_get_hurt";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("不要受伤模组已加载！");
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(new PlayerDamageHandler());
    }
}

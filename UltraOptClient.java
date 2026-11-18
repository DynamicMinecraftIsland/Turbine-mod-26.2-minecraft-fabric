package com.ultraopt;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UltraOptClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("UltraOpt");

    private boolean applied = false;
    private int tick = 0;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        LOG.info("Ultra Optimizer loaded");
    }

    private void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            applied = false;
            Tuner.reset();
            CullLogic.clear();
            return;
        }
        if (!applied) {
            Tuner.applyBase(mc);
            applied = true;
        }
        if (++tick % 20 == 0) {
            Tuner.adapt(mc);
        }
    }
}

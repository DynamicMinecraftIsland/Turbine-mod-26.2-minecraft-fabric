package com.ultraopt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class Tuner {
    // ===== НАСТРОЙКИ (меняй под себя) =====
    public static final int MAX_RENDER = 10;
    public static final int MIN_RENDER = 5;
    public static final int SIM_DISTANCE = 6;
    public static final int FPS_LOW = 70;
    public static final int FPS_HIGH = 120;
    public static final long COOLDOWN_MS = 8000;

    private static int baseRender = -1;
    private static double ema = -1;
    private static long lastChange = 0;

    private Tuner() {}

    public static void reset() {
        baseRender = -1;
        ema = -1;
    }

    public static void applyBase(Minecraft mc) {
        Options o = mc.options;

        Integer rd = getInt(o, "renderDistance");
        baseRender = Math.min(rd == null ? MAX_RENDER : rd, MAX_RENDER);
        set(o, "renderDistance", String.valueOf(baseRender));
        set(o, "simulationDistance", String.valueOf(SIM_DISTANCE));

        set(o, "graphicsMode", "FAST");
        set(o, "improvedTransparency", "false");
        set(o, "cutoutLeaves", "false");
        set(o, "ambientOcclusion", "false");
        set(o, "cloudStatus", "OFF");
        set(o, "particles", "MINIMAL");
        set(o, "entityShadows", "false");
        set(o, "biomeBlendRadius", "0");
        set(o, "mipmapLevels", "0");
        set(o, "entityDistanceScaling", "0.6");
        set(o, "weatherRadius", "3");
        set(o, "bobView", "false");
        set(o, "enableVsync", "false");
        set(o, "framerateLimit", "260");
        set(o, "menuBackgroundBlurriness", "0");
        set(o, "chunkSectionFadeInTime", "0.0");

        UltraOptClient.LOG.info("Base settings applied, render distance = {}", baseRender);
    }

    public static void adapt(Minecraft mc) {
        int fps = fps(mc);
        if (fps <= 0) return;
        ema = ema < 0 ? fps : ema * 0.7 + fps * 0.3;

        long now = System.currentTimeMillis();
        if (now - lastChange < COOLDOWN_MS) return;

        Options o = mc.options;
        Integer cur = getInt(o, "renderDistance");
        if (cur == null || baseRender < 0) return;

        if (ema < FPS_LOW && cur > MIN_RENDER) {
            set(o, "renderDistance", String.valueOf(cur - 1));
            lastChange = now;
        } else if (ema > FPS_HIGH && cur < baseRender) {
            set(o, "renderDistance", String.valueOf(cur + 1));
            lastChange = now;
        }
    }

    private static int fps(Minecraft mc) {
        try {
            Method m = Minecraft.class.getMethod("getFps");
            Object r = m.invoke(Modifier.isStatic(m.getModifiers()) ? null : mc);
            return (Integer) r;
        } catch (Throwable ignored) {}
        try {
            Field f = Minecraft.class.getDeclaredField("fps");
            f.setAccessible(true);
            return f.getInt(Modifier.isStatic(f.getModifiers()) ? null : mc);
        } catch (Throwable ignored) {}
        return -1;
    }

    private static Object opt(Options o, String name) {
        try {
            return Options.class.getMethod(name).invoke(o);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Integer getInt(Options o, String name) {
        Object inst = opt(o, name);
        if (inst instanceof OptionInstance<?> oi) {
            Object v = oi.get();
            if (v instanceof Integer i) return i;
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void set(Options o, String name, String wanted) {
        Object inst = opt(o, name);
        if (!(inst instanceof OptionInstance oi)) return;
        try {
            Object cur = oi.get();
            Object val = null;
            if (cur instanceof Boolean) {
                val = Boolean.parseBoolean(wanted);
            } else if (cur instanceof Integer) {
                val = Integer.parseInt(wanted);
            } else if (cur instanceof Double) {
                val = Double.parseDouble(wanted);
            } else if (cur instanceof Enum<?> e) {
                for (Object c : e.getDeclaringClass().getEnumConstants()) {
                    if (((Enum<?>) c).name().equalsIgnoreCase(wanted)) {
                        val = c;
                        break;
                    }
                }
            }
            if (val != null) oi.set(val);
        } catch (Throwable t) {
            UltraOptClient.LOG.warn("Could not set option {}: {}", name, t.toString());
        }
    }
}

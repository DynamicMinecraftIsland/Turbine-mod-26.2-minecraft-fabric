package com.ultraopt;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;

public final class CullLogic {
    // ===== НАСТРОЙКИ =====
    public static final double MAX_PLAYER = 128;
    public static final double MAX_MOB = 56;
    public static final double MAX_ITEM = 28;
    public static final double MAX_OTHER = 40;
    public static final double NO_OCCLUSION_RADIUS = 6;
    public static final int RECHECK_TICKS = 3;

    private static final HashMap<Integer, Long> CACHE = new HashMap<>();
    private static final BlockPos.MutableBlockPos POS = new BlockPos.MutableBlockPos();

    private CullLogic() {}

    public static void clear() {
        CACHE.clear();
    }

    public static boolean visible(Entity e, double cx, double cy, double cz) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || e == mc.player || e == mc.getCameraEntity()) return true;
        if (e.isCurrentlyGlowing()) return true;

        double dx = e.getX() - cx;
        double dy = e.getY() - cy;
        double dz = e.getZ() - cz;
        double d2 = dx * dx + dy * dy + dz * dz;

        if (e.getBbWidth() > 2.5f || e.getBbHeight() > 3.5f) {
            return d2 < 160 * 160;
        }

        double max;
        if (e instanceof Player) max = MAX_PLAYER;
        else if (e instanceof ItemEntity || e instanceof ExperienceOrb) max = MAX_ITEM;
        else if (e instanceof LivingEntity) max = MAX_MOB;
        else max = MAX_OTHER;

        if (d2 > max * max) return false;
        if (d2 < NO_OCCLUSION_RADIUS * NO_OCCLUSION_RADIUS) return true;

        long now = level.getGameTime();
        Long c = CACHE.get(e.getId());
        if (c != null && now - (c >> 1) < RECHECK_TICKS) {
            return (c & 1L) == 1L;
        }

        double h = e.getBbHeight();
        boolean vis = rayClear(level, cx, cy, cz, e.getX(), e.getY() + h * 0.5, e.getZ())
                || rayClear(level, cx, cy, cz, e.getX(), e.getY() + h * 0.95, e.getZ())
                || rayClear(level, cx, cy, cz, e.getX(), e.getY() + 0.1, e.getZ());

        CACHE.put(e.getId(), (now << 1) | (vis ? 1L : 0L));
        if (CACHE.size() > 4096) CACHE.clear();
        return vis;
    }

    private static boolean rayClear(Level level, double cx, double cy, double cz,
                                    double tx, double ty, double tz) {
        double dx = tx - cx, dy = ty - cy, dz = tz - cz;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int steps = (int) (len / 0.8);
        if (steps < 2) return true;
        double sx = dx / steps, sy = dy / steps, sz = dz / steps;
        for (int i = 1; i < steps; i++) {
            POS.set(Mth.floor(cx + sx * i), Mth.floor(cy + sy * i), Mth.floor(cz + sz * i));
            if (level.getBlockState(POS).isSolidRender()) return false;
        }
        return true;
    }
}

package com.ultraopt.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true, require = 0)
    private void ultraopt$createParticle(ParticleOptions options, double x, double y, double z,
                                         double vx, double vy, double vz,
                                         CallbackInfoReturnable<Particle> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        double dx = x - mc.player.getX();
        double dy = y - mc.player.getY();
        double dz = z - mc.player.getZ();
        if (dx * dx + dy * dy + dz * dz > 24 * 24) {
            cir.setReturnValue(null);
        }
    }
}

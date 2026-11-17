package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitSpec;
import com.bslx.XueXiMod.shu.technique.release.parting.PartingOffers;
import com.bslx.XueXiMod.shu.technique.release.parting.PartingOperation;
import com.bslx.XueXiMod.shu.technique.release.parting.WaterPartingService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** 身前先铺水面，再由中间一道斩击把水向两边推开。水本身不命中。 */
public final class WaterYieldingEffect implements TechniqueEffect {
    public static final int SURFACE_TICKS = 10;
    static final int LIFETIME = 32;
    static final double REACH = 7.0D;
    static final double SPAN = 0.7D;
    static final double RIBBON_HEIGHT = 3.2D;

    public static void declareBalance() {
        SpecialBalance.publish(SpecialBalance.blocks(
                SpecialBalance.blocks(
                        SpecialBalance.blocks(SpecialBalance.begin("water_yielding_ribbon"),
                                BalanceKeys.BOX_DEPTH, REACH, "柔带纵深"),
                        BalanceKeys.BOX_WIDTH, SPAN, "柔带宽度"),
                BalanceKeys.BOX_HEIGHT, RIBBON_HEIGHT, "柔带高度"));
    }

    @Override
    public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 ahead = flatAhead(cast.getLook());
        Vec3 anchor = player.position().add(ahead.scale(1.15)).add(0.0, 1.15, 0.0);
        TechniqueHitSpec spec = new TechniqueHitSpec();
        spec.techniqueId = technique.getId();
        spec.path = TechniqueHitSpec.Path.STILL_AIM;
        spec.speed = 0.0;
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        spec.length = SpecialBalance.number("water_yielding_ribbon", BalanceKeys.BOX_DEPTH, REACH);
        spec.width = SpecialBalance.number("water_yielding_ribbon", BalanceKeys.BOX_WIDTH, SPAN);
        spec.height = SpecialBalance.number("water_yielding_ribbon", BalanceKeys.BOX_HEIGHT, RIBBON_HEIGHT);
        spec.lifetime = definition != null ? definition.lifetimeTicks() : LIFETIME;
        spec.visual = 6;
        spec.hitReadyAge = SURFACE_TICKS;
        spec.anchor = anchor;
        spec.aim = ahead;
        PartingOperation parting = WaterPartingService.prepare(player, PartingOffers.current());
        if (parting != null) {
            spec.publishedHook = hitEntity -> WaterPartingService.attach(parting, hitEntity);
        }
        TechniqueHitSpec.SpawnReport report = TechniqueHitSpec.spawnReported(serverLevel, player, spec);
        if (parting != null) {
            WaterPartingService.noteSpawn(parting, report.accepted());
        }
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.65F, 0.85F);
        int slashTick = serverLevel.getServer().getTickCount() + SURFACE_TICKS;
        ChainScheduler.schedule(serverLevel.getServer(), slashTick, () -> {
            if (!player.isAlive() || player.level() != serverLevel) {
                return;
            }
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.7F);
        });
    }

    private static Vec3 flatAhead(Vec3 look) {
        Vec3 ahead = look == null ? new Vec3(0.0, 0.0, 1.0) : new Vec3(look.x, 0.0, look.z);
        if (ahead.lengthSqr() < 1.0E-6) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return ahead.normalize();
    }
}

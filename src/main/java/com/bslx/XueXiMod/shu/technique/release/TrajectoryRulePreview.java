package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.player.MagicPool;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.compute.ComputeCodes;
import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
import com.bslx.XueXiMod.shu.technique.release.batch.MotionProfile;
import com.bslx.XueXiMod.skill.Skill;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 单独装配的弹道规则预览。只按已登记弹道采样并发送原版粒子。
 * 不生成命中体，不锁定目标，也不改方块或战斗账本。
 */
public final class TrajectoryRulePreview {
    /** 登记寿命过短时，预览至少走完这段，方便看见路径。 */
    private static final int MINIMUM_PREVIEW_TICKS = 20;
    /** 预览上限。配置更长时也不把一次占位拖成无限任务。 */
    private static final int MAXIMUM_PREVIEW_TICKS = 100;
    /** 登记速度不是正数时，沿用普通弹丸的默认速度。 */
    private static final double FALLBACK_SPEED = 1.0D;
    private static final double SCATTER_RADIUS = 1.5D;
    private static final double DROP_LIFT = 8.0D;

    private TrajectoryRulePreview() {
    }

    /**
     * 本次实际选择的完整组里，有效项恰好一个且会改后续弹道。
     * 空项、未知项和两份相同规则都保持组合，不在这里滤掉。
     */
    public static boolean isStandalone(Skill skill) {
        if (skill == null) {
            return false;
        }
        List<Technique> techniques = skill.getTechniques();
        if (techniques == null || techniques.size() != 1) {
            return false;
        }
        Technique technique = techniques.get(0);
        if (technique == null || retiredChainRule(technique.getId())) {
            return false;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        return definition != null && definition.role().modifiesFollowingTrajectory();
    }

    /**
     * 扣掉调用方传入的现有报价后播放一次有限预览。
     * 法力不足时什么都不播，调用方沿用原提示。
     */
    public static boolean play(ServerPlayer player, Technique technique, int quotedCost) {
        if (player == null || technique == null || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (definition == null || !definition.role().modifiesFollowingTrajectory()) {
            return false;
        }
        if (!MagicPool.trySpend(player, quotedCost)) {
            return false;
        }
        if (quotedCost > 0) {
            MagicPool.showBalance(player);
        }
        Vec3 look = finiteUnit(player.getLookAngle());
        Vec3 origin = player.getEyePosition();
        long seed = level.getRandom().nextLong();
        ChainRole role = definition.role();
        if (role == ChainRole.TRAJECTORY_UPWELL) {
            scheduleUpwell(player, level, origin, look);
        } else if (role == ChainRole.TRAJECTORY_SCATTER) {
            scheduleScatter(player, level, origin, look, speedOf(definition, technique), seed);
        } else if (definition.trajectory() == Trajectory.VORTEX) {
            scheduleVortex(player, level, origin, look);
        } else if (role == ChainRole.TRAJECTORY_DROP || definition.trajectory() == Trajectory.DROP) {
            scheduleDrop(player, level, origin, speedOf(definition, technique));
        } else if (role == ChainRole.TRAJECTORY_PARALLEL || definition.trajectory() == Trajectory.PARALLEL) {
            scheduleStraight(player, level, origin, horizontal(look), speedOf(definition, technique), lifeOf(definition));
        } else {
            scheduleStraight(player, level, origin, look, speedOf(definition, technique), lifeOf(definition));
        }
        return true;
    }

    private static void scheduleStraight(ServerPlayer player, ServerLevel level, Vec3 origin, Vec3 direction,
                                         double speed, int lifetimeTicks) {
        int life = boundedLife(lifetimeTicks);
        schedule(player, level, life, age -> {
            TrajectorySampler.Sample sample = TrajectorySampler.sample(
                    Trajectory.STRAIGHT, origin, direction, speed, life, age, null, false, direction, null);
            emit(level, ParticleTypes.END_ROD, sample.position());
        });
    }

    private static void scheduleDrop(ServerPlayer player, ServerLevel level, Vec3 origin, double speed) {
        Vec3 raised = origin.add(0.0D, DROP_LIFT, 0.0D);
        int life = boundedLife((int) Math.ceil(Math.sqrt((2.0D * DROP_LIFT) / (ComputeCodes.DROP_LEADING * ComputeCodes.DROP_FACTOR))));
        schedule(player, level, life, age -> {
            TrajectorySampler.Sample sample = TrajectorySampler.sample(
                    Trajectory.DROP, raised, new Vec3(0.0D, -1.0D, 0.0D), speed, life, age,
                    null, false, new Vec3(0.0D, -1.0D, 0.0D), null);
            emit(level, ParticleTypes.END_ROD, sample.position());
        });
    }

    private static void scheduleVortex(ServerPlayer player, ServerLevel level, Vec3 origin, Vec3 look) {
        int spiralFloor = Math.max(ComputeCodes.VORTEX_SPIRAL_TICK_FLOOR, (MINIMUM_PREVIEW_TICKS * 2) / 3);
        int life = boundedLife(spiralFloor + 4);
        Vec3 phase = new Vec3(Math.cos(look.x), 0.0D, Math.sin(look.z));
        schedule(player, level, life, age -> {
            TrajectorySampler.Sample sample = TrajectorySampler.sample(
                    Trajectory.VORTEX, origin, phase, 0.0D, life, age, null, false, phase, null);
            emit(level, ParticleTypes.CLOUD, sample.position());
        });
    }

    private static void scheduleUpwell(ServerPlayer player, ServerLevel level, Vec3 origin, Vec3 look) {
        MotionProfile profile = MotionProfile.approvedUpwell();
        int life = boundedLife(profile.motionTicks());
        Vec3 velocity = TrajectorySampler.upwellVelocity(look, profile.speed(), profile.coneDegrees(), 0, 1);
        schedule(player, level, life, age -> {
            TrajectorySampler.Sample sample = TrajectorySampler.sampleBallistic(
                    origin, velocity, profile.gravity(), age, life, profile.horizontalLimit());
            emit(level, ParticleTypes.FLAME, sample.position());
        });
    }

    private static void scheduleScatter(ServerPlayer player, ServerLevel level, Vec3 origin, Vec3 look,
                                        double speed, long seed) {
        int life = boundedLife(MINIMUM_PREVIEW_TICKS);
        RandomSource random = RandomSource.create(seed ^ 0x6C62272EL);
        double rise = random.nextDouble() * 2.0D - 1.0D;
        double spin = random.nextDouble() * Math.PI * 2.0D;
        double ring = Math.sqrt(Math.max(0.0D, 1.0D - rise * rise));
        Vec3 direction = new Vec3(ring * Math.cos(spin), rise, ring * Math.sin(spin));
        Vec3 flatLook = horizontal(look);
        Vec3 flatRight = new Vec3(-flatLook.z, 0.0D, flatLook.x);
        double radius = random.nextDouble() * SCATTER_RADIUS;
        double azimuth = random.nextDouble() * Math.PI * 2.0D;
        Vec3 scattered = origin
                .add(flatRight.scale(Math.cos(azimuth) * radius))
                .add(flatLook.scale(Math.sin(azimuth) * radius));
        schedule(player, level, life, age -> {
            TrajectorySampler.Sample sample = TrajectorySampler.sample(
                    Trajectory.STRAIGHT, scattered, direction, speed, life, age, null, false, direction, null);
            emit(level, ParticleTypes.CRIT, sample.position());
        });
    }

    private static void schedule(ServerPlayer player, ServerLevel level, int lifetimeTicks, SampleEmitter emitter) {
        int started = player.getServer().getTickCount();
        for (int age = 0; age <= lifetimeTicks; age++) {
            int sampleAge = age;
            ChainScheduler.schedule(player.getServer(), started + age, () -> {
                if (!player.isAlive() || player.level() != level) {
                    return;
                }
                emitter.emit(sampleAge);
            });
        }
    }

    private static void emit(ServerLevel level, ParticleOptions particle, Vec3 position) {
        if (position == null || !Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)) {
            return;
        }
        level.sendParticles(particle, position.x, position.y, position.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static double speedOf(ReleaseDefinition definition, Technique technique) {
        float registered = definition.speed();
        if (Float.isFinite(registered) && registered > 0.0F) {
            return registered;
        }
        float projectile = technique.getProjectileSpeed();
        if (Float.isFinite(projectile) && projectile > 0.0F) {
            return projectile;
        }
        return FALLBACK_SPEED;
    }

    private static int lifeOf(ReleaseDefinition definition) {
        int registered = definition.lifetimeTicks();
        if (registered > 1) {
            return registered;
        }
        return MINIMUM_PREVIEW_TICKS;
    }

    private static int boundedLife(int ticks) {
        return Math.max(1, Math.min(MAXIMUM_PREVIEW_TICKS, ticks));
    }

    /** 旧链仍识别、但已从可装配列表移除的两条规则。不算单独预览。 */
    private static boolean retiredChainRule(String techniqueId) {
        return "rule_interleave".equals(techniqueId) || "rule_damage_tally".equals(techniqueId);
    }

    private static Vec3 finiteUnit(Vec3 vector) {
        if (vector == null || !Double.isFinite(vector.x) || !Double.isFinite(vector.y) || !Double.isFinite(vector.z)) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        double length = vector.length();
        if (!Double.isFinite(length) || length < 1.0E-6D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return vector.scale(1.0D / length);
    }

    private static Vec3 horizontal(Vec3 vector) {
        Vec3 unit = finiteUnit(vector);
        double length = Math.sqrt(unit.x * unit.x + unit.z * unit.z);
        if (!Double.isFinite(length) || length < 1.0E-4D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return new Vec3(unit.x / length, 0.0D, unit.z / length);
    }

    @FunctionalInterface
    private interface SampleEmitter {
        void emit(int age);
    }
}

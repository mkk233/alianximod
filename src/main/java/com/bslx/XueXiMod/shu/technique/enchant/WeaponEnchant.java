package com.bslx.XueXiMod.shu.technique.enchant;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueManager;
import com.bslx.XueXiMod.shu.balance.BalanceService;
import com.bslx.XueXiMod.shu.balance.RawDamage;
import com.bslx.XueXiMod.shu.technique.enchant.resource.HitResourceResult;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantCatalog;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantSlice;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceHitCommit;
import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.skill.SkillType;
import com.bslx.XueXiMod.status.fire.seed.FireCastContext;
import com.bslx.XueXiMod.status.fire.seed.FireSeedGrant;
import com.bslx.XueXiMod.status.wood.vine.VineMarkRuntime;
import com.bslx.XueXiMod.shu.technique.release.relations.HarmPolicy;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * 没有攻击术式承载时，附魔记在主手武器上，直到下一次普通攻击消耗。再次写入会覆盖。
 * 含潮印的印记并进原版那一次主目标伤害，不再另打一刀。纯非水印记仍走原来的事件。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class WeaponEnchant {
    public static final String KEY = "xueximod_weapon_enchant";
    private static final String VERSION_KEY = "Version";
    private static final String TIDE_DAMAGE_KEY = "TideDamage";
    private static final String ADDED_SOURCE_KEY = "AddedSource";
    private static final int CONFIGURED_VERSION = 1;
    private static final WeakHashMap<ItemStack, Integer> LIVE_STAMP = new WeakHashMap<>();
    private static int nextStamp = 1;

    private WeaponEnchant() {}

    public static void imprint(ServerPlayer player, ItemStack weapon, EnchantBonus bonus) {
        if (player == null || weapon == null || weapon.isEmpty() || bonus == null) {
            return;
        }
        boolean resourceBearing = bonus.resourceLoad != null && !bonus.resourceLoad.empty();
        if (!resourceBearing && bonus.burnSeconds <= 0 && bonus.addedDamage <= 0f && bonus.tideMarks <= 0
                && bonus.seedPoints <= 0) {
            return;
        }
        CompoundTag tag = weapon.getOrCreateTagElement(KEY);
        putBonus(tag, bonus);
        if (resourceBearing) {
            ListTag storedIds = new ListTag();
            for (ResourceEnchantSlice slice : bonus.resourceLoad.slices()) {
                if (slice != null && slice.enchantmentId() != null) {
                    storedIds.add(StringTag.valueOf(slice.enchantmentId()));
                }
            }
            tag.put("ResourceIds", storedIds);
        }
        LIVE_STAMP.put(weapon, nextStamp++);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a附魔已记在武器上，下一次攻击消耗"));
    }

    public static CompoundTag toTag(EnchantBonus bonus) {
        CompoundTag tag = new CompoundTag();
        if (bonus == null) {
            return tag;
        }
        putBonus(tag, bonus);
        return tag;
    }

    /** 含水印记加到原版主攻击上的附加值。不含保存的武器攻击力。无潮印时为 0，留给旧流程。 */
    public static float meleeFlat(CompoundTag tag) {
        if (tag == null || tag.getInt("Tide") <= 0) {
            return 0f;
        }
        float addedDamage = tag.getFloat("Added");
        if (!tag.contains(VERSION_KEY)) {
            return tag.getInt("Tide") * 2f + addedDamage;
        }
        float tideDamage = tag.contains(TIDE_DAMAGE_KEY) ? tag.getFloat(TIDE_DAMAGE_KEY) : tag.getInt("Tide") * 2f;
        return tideDamage + addedDamage;
    }

    private static void putBonus(CompoundTag tag, EnchantBonus bonus) {
        tag.putInt(VERSION_KEY, CONFIGURED_VERSION);
        tag.putFloat("Attack", configuredAttack(bonus));
        tag.putFloat("Added", configuredAdded(bonus));
        tag.putFloat(TIDE_DAMAGE_KEY, configuredTideDamage(bonus));
        tag.putString(ADDED_SOURCE_KEY, bonus.addedSourceId());
        tag.putInt("Burn", bonus.burnSeconds);
        tag.putInt("SeedPoints", bonus.seedPoints);
        tag.putFloat("Radius", bonus.radiusMultiplier);
        tag.putInt("Control", bonus.controlTicks);
        tag.putBoolean("Extinguish", bonus.extinguish);
        tag.putFloat("Knockback", bonus.knockback);
        tag.putInt("Tide", bonus.tideMarks);
    }

    private static float configuredAttack(EnchantBonus bonus) {
        return BalanceService.globalDamageContribution(bonus.rawHostAttack()).amount();
    }

    private static float configuredAdded(EnchantBonus bonus) {
        if (bonus.rawAddedDamage() <= 0.0f || bonus.addedSourceId().isEmpty()) {
            return 0.0f;
        }
        return BalanceService.damageContribution(bonus.addedSourceId(), RawDamage.of(bonus.rawAddedDamage())).amount();
    }

    private static float configuredTideDamage(EnchantBonus bonus) {
        if (bonus.tideMarks() <= 0 || bonus.rawTideDamage() <= 0.0f) {
            return 0.0f;
        }
        return BalanceService.damageContribution(SpecialBalance.TIDE_MARK, RawDamage.of(bonus.rawTideDamage())).amount();
    }

    public static boolean consumeImprint(boolean damageAccepted, CompoundTag captured, CompoundTag current) {
        return damageAccepted && captured != null && current != null
                && captured.getInt("Tide") > 0 && captured.equals(current);
    }

    public static float includeTideFlat(Player player, float amount) {
        CompoundTag tag = tideTag(player);
        if (tag == null) {
            return amount;
        }
        return amount + meleeFlat(tag);
    }

    public static SwingTide captureSwing(Entity attacker, Entity target) {
        if (!(attacker instanceof ServerPlayer player) || target == null) {
            return SwingTide.NONE;
        }
        ItemStack stack = player.getMainHandItem();
        CompoundTag tag = imprintTag(stack);
        if (tag == null) {
            return SwingTide.NONE;
        }
        CompoundTag captured = tag.copy();
        int stamp = LIVE_STAMP.getOrDefault(stack, 0);
        TideMarkDamageScope.Token token = TideMarkDamageScope.open(
                target, player, TideMarkSuccess.of(captured.getInt("Tide"), player.getUUID()));
        return new SwingTide(player, stack, captured, stamp, token, target);
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        if (HarmPolicy.denies(player, target)) {
            event.setCanceled(true);
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        if (!weapon.hasTag() || !weapon.getTag().contains(KEY)) {
            return;
        }
        CompoundTag tag = weapon.getTag().getCompound(KEY);
        if (tag.getInt("Tide") > 0) {
            return;
        }
        float attack = tag.getFloat("Attack");
        float added = tag.getFloat("Added");
        int burn = tag.getInt("Burn");
        // 新印记的 Attack、Added 在写入时已经结算；缺版本旧印记保存的就是当时的最终金额。
        float amount = attack + added;
        boolean damageLanded = false;
        if (amount > 0f) {
            damageLanded = target.hurt(target.level().damageSources().playerAttack(player), amount);
        }
        if (burn > 0) {
            target.setSecondsOnFire(burn);
        }
        if (tag.getBoolean("Extinguish")) {
            target.clearFire();
        }
        int control = tag.getInt("Control");
        if (control > 0) {
            VineMarkRuntime.suppressLegacyControl(player, target);
        }
        float knockback = tag.getFloat("Knockback");
        if (knockback > 0f) {
            Vec3 delta = target.position().subtract(player.position());
            target.knockback(knockback, delta.x, delta.z);
        }
        boolean resourceBearing = tag.contains("ResourceIds", Tag.TAG_LIST);
        if (damageLanded && !resourceBearing) {
            FireSeedGrant.deposit(target, player, tag.getInt("SeedPoints"));
        }
        if (!resourceBearing) {
            weapon.getTag().remove(KEY);
        }
    }

    private static CompoundTag imprintTag(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag() || !stack.getTag().contains(KEY)) {
            return null;
        }
        CompoundTag tag = stack.getTag().getCompound(KEY);
        if (tag.getInt("Tide") > 0 || tag.contains("ResourceIds", Tag.TAG_LIST)) {
            return tag;
        }
        return null;
    }

    private static ResourceEnchantLoad loadFromTag(CompoundTag tag) {
        if (tag == null || !tag.contains("ResourceIds", Tag.TAG_LIST)) {
            return ResourceEnchantLoad.EMPTY;
        }
        ListTag storedIds = tag.getList("ResourceIds", Tag.TAG_STRING);
        List<Technique> techniques = new ArrayList<>();
        List<Integer> indexes = new ArrayList<>();
        for (int entryIndex = 0; entryIndex < storedIds.size(); entryIndex++) {
            Technique technique = TechniqueManager.getById(storedIds.getString(entryIndex));
            if (technique == null) {
                continue;
            }
            indexes.add(techniques.size());
            techniques.add(technique);
        }
        if (techniques.isEmpty()) {
            return ResourceEnchantLoad.EMPTY;
        }
        return ResourceEnchantCatalog.collect(new Skill("印记", SkillType.MAGIC, techniques), indexes);
    }

    private static CompoundTag tideTag(Player player) {
        if (!(player instanceof ServerPlayer)) {
            return null;
        }
        return tideTag(player.getMainHandItem());
    }

    private static CompoundTag tideTag(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag() || !stack.getTag().contains(KEY)) {
            return null;
        }
        CompoundTag tag = stack.getTag().getCompound(KEY);
        if (tag.getInt("Tide") <= 0) {
            return null;
        }
        return tag;
    }

    public static final class SwingTide {
        static final SwingTide NONE = new SwingTide(null, ItemStack.EMPTY, null, 0, TideMarkDamageScope.Token.NONE, null);

        private final ServerPlayer attacker;
        private final ItemStack stack;
        private final CompoundTag captured;
        private final int stamp;
        private final TideMarkDamageScope.Token token;
        private final Entity target;

        private SwingTide(ServerPlayer attacker, ItemStack stack, CompoundTag captured, int stamp,
                          TideMarkDamageScope.Token token, Entity target) {
            this.attacker = attacker;
            this.stack = stack;
            this.captured = captured;
            this.stamp = stamp;
            this.token = token;
            this.target = target;
        }

        public void finish(boolean landed) {
            boolean accepted = TideMarkDamageScope.finish(this.token, landed);
            if (!accepted || this.stack == null || this.captured == null || !this.stack.hasTag()) {
                return;
            }
            if (!this.stack.getTag().contains(KEY)) {
                return;
            }
            CompoundTag current = this.stack.getTag().getCompound(KEY);
            Integer liveStamp = LIVE_STAMP.get(this.stack);
            boolean stampMatches = liveStamp == null || liveStamp.intValue() == this.stamp;
            boolean tideImprint = consumeImprint(true, this.captured, current);
            boolean resourceImprint = this.captured.contains("ResourceIds", Tag.TAG_LIST)
                    && current.contains("ResourceIds", Tag.TAG_LIST);
            if ((!tideImprint && !resourceImprint) || !stampMatches) {
                return;
            }
            applyStored(this.target, this.captured);
            if (this.target instanceof LivingEntity living && !this.commitResource(living)) {
                return;
            }
            this.stack.getTag().remove(KEY);
            LIVE_STAMP.remove(this.stack);
        }

        private boolean commitResource(LivingEntity living) {
            if (this.captured == null || this.attacker == null) {
                return false;
            }
            if (!this.captured.contains("ResourceIds", Tag.TAG_LIST)) {
                FireSeedGrant.deposit(living, this.attacker, this.captured.getInt("SeedPoints"));
                return true;
            }
            ResourceEnchantLoad load = loadFromTag(this.captured);
            int legacyPoints = Math.max(0, this.captured.getInt("SeedPoints"));
            FireCastContext cast = new FireCastContext(UUID.randomUUID(), this.attacker.getUUID());
            HitResourceResult result = ResourceHitCommit.commit(living, cast, this.attacker, load, 0, legacyPoints, true);
            return result.status() == HitResourceResult.Status.APPLIED;
        }

        private void applyStored(Entity victim, CompoundTag tag) {
            if (!(victim instanceof LivingEntity living)) {
                return;
            }
            if (HarmPolicy.denies(this.attacker, living)) {
                return;
            }
            int burn = tag.getInt("Burn");
            if (burn > 0) {
                living.setSecondsOnFire(burn);
            }
            if (tag.getBoolean("Extinguish")) {
                living.clearFire();
            }
            int control = tag.getInt("Control");
            if (control > 0) {
                VineMarkRuntime.suppressLegacyControl(this.attacker, living);
            }
            float knockback = tag.getFloat("Knockback");
            if (knockback > 0f && this.attacker != null) {
                Vec3 delta = living.position().subtract(this.attacker.position());
                living.knockback(knockback, delta.x, delta.z);
            }
        }
    }
}

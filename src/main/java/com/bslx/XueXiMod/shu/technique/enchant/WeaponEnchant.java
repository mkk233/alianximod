package com.bslx.XueXiMod.shu.technique.enchant;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 没有攻击术式承载时，附魔记在主手武器上，直到下一次普通攻击消耗。再次写入会覆盖。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class WeaponEnchant {
    public static final String KEY = "xueximod_weapon_enchant";

    private WeaponEnchant() {}

    public static void imprint(ServerPlayer player, ItemStack weapon, EnchantBonus bonus) {
        if (player == null || weapon == null || weapon.isEmpty() || bonus == null || bonus.burnSeconds <= 0 && bonus.addedDamage <= 0f) {
            return;
        }
        CompoundTag tag = weapon.getOrCreateTagElement(KEY);
        tag.putFloat("Attack", bonus.attackDamage);
        tag.putFloat("Added", bonus.addedDamage);
        tag.putInt("Burn", bonus.burnSeconds);
        tag.putFloat("Radius", bonus.radiusMultiplier);
        tag.putInt("Control", bonus.controlTicks);
        tag.putBoolean("Extinguish", bonus.extinguish);
        tag.putFloat("Knockback", bonus.knockback);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a附魔已记在武器上，下一次攻击消耗"));
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        if (!weapon.hasTag() || !weapon.getTag().contains(KEY)) {
            return;
        }
        CompoundTag tag = weapon.getTag().getCompound(KEY);
        float attack = tag.getFloat("Attack");
        float added = tag.getFloat("Added");
        int burn = tag.getInt("Burn");
        float amount = attack + added;
        if (amount > 0f) {
            target.hurt(target.level().damageSources().playerAttack(player), amount);
        }
        if (burn > 0) {
            target.setSecondsOnFire(burn);
        }
        if (tag.getBoolean("Extinguish")) {
            target.clearFire();
        }
        int control = tag.getInt("Control");
        if (control > 0) {
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, control, 4));
        }
        float knockback = tag.getFloat("Knockback");
        if (knockback > 0f) {
            net.minecraft.world.phys.Vec3 delta = target.position().subtract(player.position());
            target.knockback(knockback, delta.x, delta.z);
        }
        weapon.getTag().remove(KEY);
    }
}

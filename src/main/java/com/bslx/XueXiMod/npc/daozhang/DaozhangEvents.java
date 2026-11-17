package com.bslx.XueXiMod.npc.daozhang;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.shu.technique.release.relations.CombatOwnership;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/** 徒弟的近战和投射物在效果发生前停下。保管箱只在真正打开后判定。 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class DaozhangEvents {
    private DaozhangEvents() {
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof DaozhangEntity daozhang)) {
            return;
        }
        if (daozhang.relation(event.getEntity().getUUID()) == DaozhangRelation.APPRENTICE) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof DaozhangEntity daozhang) || event.getSource() == null) {
            return;
        }
        UUID playerId = CombatOwnership.playerOwner(event.getSource().getEntity());
        if (playerId == null) {
            playerId = CombatOwnership.playerOwner(event.getSource().getDirectEntity());
        }
        if (playerId != null && daozhang.relation(playerId) == DaozhangRelation.APPRENTICE) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onProjectile(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof DaozhangEntity daozhang)) {
            return;
        }
        UUID playerId = CombatOwnership.playerOwner(projectile);
        if (playerId != null && daozhang.relation(playerId) == DaozhangRelation.APPRENTICE) {
            event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
        }
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DaozhangChests.noteClick(player, event.getPos());
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        AbstractContainerMenu menu = event.getContainer();
        if (menu instanceof ChestMenu chest) {
            DaozhangChests.confirmOpen(player, chest.getContainer());
        }
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        DaozhangChests.clear(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DaozhangChests.clear(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        DaozhangChests.clear(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            DaozhangChests.expire(player);
        }
    }
}

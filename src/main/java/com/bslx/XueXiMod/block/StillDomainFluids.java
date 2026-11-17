package com.bslx.XueXiMod.block;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Consumer;

/** 深海领域的临时静水。不注册方块物品，避免被拿去无限复制。 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class StillDomainFluids {
    public static final DeferredRegister<FluidType> TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, XueXiMod.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, XueXiMod.MOD_ID);

    public static final RegistryObject<FluidType> TYPE = TYPES.register("still_domain_water", StillDomainFluids::createType);
    public static final RegistryObject<StillDomainFluid.Source> SOURCE = FLUIDS.register("still_domain_water",
            StillDomainFluids::createSource);
    public static final RegistryObject<StillDomainFluid.Flowing> FLOWING = FLUIDS.register("still_domain_water_flowing",
            StillDomainFluids::createFlowing);
    public static final RegistryObject<LiquidBlock> BLOCK = ModBlocks.BLOCKS.register("still_domain_water",
            () -> new LiquidBlock(SOURCE, BlockBehaviour.Properties.copy(Blocks.WATER).noLootTable()));
    private static final ForgeFlowingFluid.Properties PROPERTIES = new ForgeFlowingFluid.Properties(TYPE, SOURCE, FLOWING)
            .block(BLOCK)
            .slopeFindDistance(1)
            .levelDecreasePerBlock(8)
            .explosionResistance(100.0F)
            .tickRate(40);

    private StillDomainFluids() {
    }

    public static void register(IEventBus eventBus) {
        TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }

    private static StillDomainFluid.Source createSource() {
        return new StillDomainFluid.Source(PROPERTIES);
    }

    private static StillDomainFluid.Flowing createFlowing() {
        return new StillDomainFluid.Flowing(PROPERTIES);
    }

    private static FluidType createType() {
        return new FluidType(FluidType.Properties.create()
                .descriptionId("block.xueximod.still_domain_water")
                .canSwim(true)
                .canDrown(true)
                .canExtinguish(true)
                .canHydrate(true)
                .supportsBoating(true)
                .canPushEntity(true)
                .canConvertToSource(false)
                .motionScale(0.014D)
                .fallDistanceModifier(0.0F)
                .density(1000)
                .viscosity(1000)
                .temperature(300)
                .pathType(BlockPathTypes.WATER)
                .adjacentPathType(null)
                .sound(net.minecraftforge.common.SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(net.minecraftforge.common.SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)) {
            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                com.bslx.XueXiMod.client.StillDomainWaterClient.attach(consumer);
            }
        };
    }

    @SubscribeEvent
    public static void onBlockUse(PlayerInteractEvent.RightClickBlock event) {
        refuseContainer(event.getItemStack().getItem() == Items.BUCKET || event.getItemStack().getItem() == Items.GLASS_BOTTLE,
                event.getLevel().getFluidState(event.getPos()).getType() instanceof StillDomainFluid, event);
    }

    @SubscribeEvent
    public static void onItemUse(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() == null || event.getLevel() == null) {
            return;
        }
        boolean container = event.getItemStack().getItem() == Items.BUCKET
                || event.getItemStack().getItem() == Items.GLASS_BOTTLE;
        if (!container) {
            return;
        }
        BlockHitResult trace = sourceTrace(event.getEntity());
        boolean ours = trace.getType() == HitResult.Type.BLOCK
                && event.getLevel().getFluidState(trace.getBlockPos()).getType() instanceof StillDomainFluid;
        refuseContainer(true, ours, event);
    }

    private static BlockHitResult sourceTrace(Player player) {
        float pitch = player.getXRot();
        float yaw = player.getYRot();
        Vec3 eye = player.getEyePosition();
        float yawCosine = Mth.cos(-yaw * ((float) Math.PI / 180F) - (float) Math.PI);
        float yawSine = Mth.sin(-yaw * ((float) Math.PI / 180F) - (float) Math.PI);
        float pitchCosine = -Mth.cos(-pitch * ((float) Math.PI / 180F));
        float pitchSine = Mth.sin(-pitch * ((float) Math.PI / 180F));
        double reach = player.getBlockReach();
        Vec3 far = eye.add(yawSine * pitchCosine * reach, pitchSine * reach, yawCosine * pitchCosine * reach);
        return player.level().clip(new ClipContext(eye, far, ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
    }

    private static void refuseContainer(boolean container, boolean ourFluid, PlayerInteractEvent event) {
        if (!container || !ourFluid) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
    }
}

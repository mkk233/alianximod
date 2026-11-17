package com.bslx.XueXiMod.block;

import com.bslx.XueXiMod.shu.technique.release.domain.motion.StillWaterLease;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.ForgeFlowingFluid;

/** 静止水源。不向外流，桶接口返回空气，无有效租约时收回成空气。 */
public abstract class StillDomainFluid extends ForgeFlowingFluid {
    protected StillDomainFluid(ForgeFlowingFluid.Properties properties) {
        super(properties);
    }

    @Override
    public Item getBucket() {
        return Items.AIR;
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos cell, Fluid fluid, Direction direction) {
        return false;
    }

    @Override
    protected boolean canSpreadTo(BlockGetter level, BlockPos from, BlockState fromBlock, Direction direction,
                                  BlockPos toward, BlockState towardBlock, FluidState towardFluid, Fluid fluid) {
        return false;
    }

    @Override
    public void tick(Level level, BlockPos cell, FluidState state) {
        if (level.isClientSide()) {
            return;
        }
        if (!isSource(state) || !StillWaterLease.liveAt(level, cell)) {
            level.setBlock(cell, Blocks.AIR.defaultBlockState(), 3);
            return;
        }
        level.scheduleTick(cell, this, getTickDelay(level));
    }

    /** 方块水位 0 对应的源头。本身没有 level 属性。 */
    public static final class Source extends StillDomainFluid {
        public Source(ForgeFlowingFluid.Properties properties) {
            super(properties);
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    /**
     * 流动状态只为液体方块缓存 level=1..8 而存在。
     * 源头流体没有 level，液体方块初始化时会调用 getFlowing 再写入该属性。
     */
    public static final class Flowing extends StillDomainFluid {
        public Flowing(ForgeFlowingFluid.Properties properties) {
            super(properties);
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}

package com.bslx.XueXiMod.item;

import com.bslx.XueXiMod.player.LearningGraph;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** 参悟用的阶段简。本身不能右键学会术式。 */
public class StudySlipItem extends Item {
    private final LearningGraph.Stage stage;

    public StudySlipItem(Properties properties, LearningGraph.Stage stage) {
        super(properties);
        this.stage = stage;
    }

    public LearningGraph.Stage stage() {
        return stage;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        String key = stack.getDescriptionId();
        tooltip.add(Component.translatable("tooltip." + key.substring(key.indexOf('.') + 1)));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}

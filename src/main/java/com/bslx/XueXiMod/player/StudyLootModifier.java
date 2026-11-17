package com.bslx.XueXiMod.player;

import com.bslx.XueXiMod.item.ModItems;
import com.bslx.XueXiMod.item.ShuEmbryoItem;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/** 每个箱子额外独立滚一次，最多加一件。不替换原有掉落。 */
public class StudyLootModifier extends LootModifier {
    private static final int ROLL = 100;
    private static volatile List<StudyLootOutcome> village;

    public static final Codec<StudyLootModifier> CODEC = RecordCodecBuilder.create(instance ->
            entriesCodec(instance).apply(instance, StudyLootModifier::new));

    private static Products.P2<RecordCodecBuilder.Mu<StudyLootModifier>, LootItemCondition[], Optional<List<StudyLootOutcome>>> entriesCodec(
            RecordCodecBuilder.Instance<StudyLootModifier> instance) {
        return codecStart(instance).and(ExtraCodecs.nonEmptyList(StudyLootOutcome.CODEC.listOf()).optionalFieldOf("entries")
                .forGetter(modifier -> modifier.entries));
    }

    private final Optional<List<StudyLootOutcome>> entries;

    public StudyLootModifier(LootItemCondition[] conditions) {
        this(conditions, Optional.empty());
    }

    public StudyLootModifier(LootItemCondition[] conditions, Optional<List<StudyLootOutcome>> entries) {
        super(conditions);
        this.entries = entries.map(StudyLootModifier::checked);
    }

    private static List<StudyLootOutcome> village() {
        List<StudyLootOutcome> current = village;
        if (current == null) {
            current = List.of(
                    StudyLootOutcome.oneOf(30,
                            ModItems.STUDY_SCROLL_SWORD.get(),
                            ModItems.STUDY_SCROLL_WATER.get(),
                            ModItems.STUDY_SCROLL_FIRE.get()),
                    StudyLootOutcome.item(25, ModItems.STUDY_SLIP_MORTAL.get()),
                    StudyLootOutcome.embryo(15, ShuAttribute.NONE));
            village = current;
        }
        return current;
    }

    private static List<StudyLootOutcome> checked(List<StudyLootOutcome> entries) {
        int weight = 0;
        for (StudyLootOutcome entry : entries) {
            weight += entry.weight;
        }
        if (weight > ROLL) {
            throw new IllegalArgumentException("study loot weights exceed " + ROLL + ": " + weight);
        }
        return List.copyOf(entries);
    }

    private List<StudyLootOutcome> outcomes() {
        return entries.orElseGet(StudyLootModifier::village);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        int roll = context.getRandom().nextInt(100);
        int cursor = 0;
        for (StudyLootOutcome entry : outcomes()) {
            cursor += entry.weight;
            if (roll < cursor) {
                generatedLoot.add(entry.create(context));
                break;
            }
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    public record StudyLootOutcome(int weight, List<Item> items, ShuAttribute attribute) {
        private static final Codec<ShuAttribute> ATTRIBUTE = Codec.STRING.comapFlatMap(StudyLootOutcome::attributeOf, ShuAttribute::getId);
        public static final Codec<StudyLootOutcome> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ExtraCodecs.POSITIVE_INT.fieldOf("weight").forGetter(StudyLootOutcome::weight),
                ExtraCodecs.nonEmptyList(ForgeRegistries.ITEMS.getCodec().listOf()).fieldOf("items").forGetter(StudyLootOutcome::items),
                ATTRIBUTE.optionalFieldOf("attribute", ShuAttribute.NONE).forGetter(StudyLootOutcome::attribute)
        ).apply(instance, StudyLootOutcome::new));

        private static DataResult<ShuAttribute> attributeOf(String identifier) {
            for (ShuAttribute attribute : ShuAttribute.values()) {
                if (attribute.getId().equals(identifier)) {
                    return DataResult.success(attribute);
                }
            }
            return DataResult.error(() -> "unknown shu attribute: " + identifier);
        }

        public StudyLootOutcome {
            if (attribute != ShuAttribute.NONE && (items.size() != 1 || items.get(0) != ModItems.shu_embryo.get())) {
                throw new IllegalArgumentException("only shu embryos carry an attribute");
            }
        }

        static StudyLootOutcome item(int weight, Item item) {
            return new StudyLootOutcome(weight, List.of(item), ShuAttribute.NONE);
        }

        static StudyLootOutcome oneOf(int weight, Item... choices) {
            return new StudyLootOutcome(weight, List.of(choices), ShuAttribute.NONE);
        }

        static StudyLootOutcome embryo(int weight, ShuAttribute attribute) {
            return new StudyLootOutcome(weight, List.of(ModItems.shu_embryo.get()), attribute);
        }

        ItemStack create(LootContext context) {
            Item chosen = items.get(items.size() == 1 ? 0 : context.getRandom().nextInt(items.size()));
            if (chosen == ModItems.shu_embryo.get()) {
                return ShuEmbryoItem.create(attribute);
            }
            return new ItemStack(chosen);
        }
    }
}

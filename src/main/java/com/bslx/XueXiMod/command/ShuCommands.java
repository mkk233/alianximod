package com.bslx.XueXiMod.command;

import com.bslx.XueXiMod.debug.DebugLog;
import com.bslx.XueXiMod.item.SkillBookItem;
import com.bslx.XueXiMod.item.ShuEmbryoItem;
import com.bslx.XueXiMod.item.SkillStoneItem;
import com.bslx.XueXiMod.item.ModItems;
import com.bslx.XueXiMod.shu.SkillStoneData;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.player.PlayerShuData;
import com.bslx.XueXiMod.network.ModNetwork;
import com.bslx.XueXiMod.network.ShuSyncPacket;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueManager;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.skill.SkillType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试命令
 * /shu give   - 给予空白原胚和木属性原胚
 * /shu stone <id> - 用空白原胚刻指定术，给予术式
 * /shu list   - 列出所有已注册的术
 * /shu learn <id> - 学习指定术
 * /shu reset  - 重置玩家术数据
 */
public class ShuCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("shu")
            .requires(cs -> cs.hasPermission(2))
            .then(Commands.literal("give")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ItemStack blank = ShuEmbryoItem.create(ShuAttribute.NONE);
                    blank.setCount(8);
                    ItemStack wood = ShuEmbryoItem.create(ShuAttribute.WOOD);
                    wood.setCount(8);
                    player.getInventory().add(blank);
                    player.getInventory().add(wood);
                    ctx.getSource().sendSuccess(() ->
                        Component.literal("§a已给予空白原胚×8、木属性原胚×8"), false);
                    return 1;
                })
            )

            .then(Commands.literal("stone")
                .then(Commands.argument("id", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        for (Technique t : TechniqueManager.getAll()) {
                            builder.suggest(t.getId());
                        }
                        return builder.buildFuture();
                    })
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        String id = StringArgumentType.getString(ctx, "id");
                        Technique t = TechniqueManager.getById(id);
                        if (t == null) {
                            ctx.getSource().sendFailure(Component.literal("§c未知的术: " + id));
                            return 0;
                        }
                        SkillStoneData data = SkillStoneData.imprint(t, ShuAttribute.NONE);
                        player.getInventory().add(SkillStoneItem.create(data));
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§a已给予术式：" + t.getName()), false);
                        return 1;
                    })
                )
            )

            // /shu list - 列出所有术
            .then(Commands.literal("list")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() ->
                        Component.literal("§6=== 已注册的术 ==="), false);
                    for (Technique t : TechniqueManager.getAll()) {
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§f" + t.getId() + " §7- " + t.getName() +
                                " §8[" + t.getType() + "] §c伤害:" + t.getBaseDamage() +
                                " §b冷却:" + t.getBaseCooldown() + "t"), false);
                    }
                    return 1;
                })
            )

            // /shu learn <id> - 学习术
            .then(Commands.literal("learn")
                .then(Commands.argument("id", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        for (Technique t : TechniqueManager.getAll()) {
                            builder.suggest(t.getId());
                        }
                        return builder.buildFuture();
                    })
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        String id = StringArgumentType.getString(ctx, "id");
                        Technique t = TechniqueManager.getById(id);
                        if (t == null) {
                            ctx.getSource().sendFailure(Component.literal("§c未知的术: " + id));
                            return 0;
                        }
                        PlayerShuData data = PlayerShuData.get(player);
                        data.learnTechnique(id);
                        PlayerShuData.save(player, data);
                        ModNetwork.sendToPlayer(player, new ShuSyncPacket(data));
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§a已学习: " + t.getName()), false);
                        return 1;
                    })
                )
            )

            // /shu reset - 重置数据
            .then(Commands.literal("reset")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    // 清空数据后重新初始化
                    CompoundTag emptyTag = new CompoundTag();
                    player.getPersistentData().put("xueximod_shu_data", emptyTag);
                    // 重新给予基础术
                    PlayerShuData data = PlayerShuData.get(player);
                    for (Technique t : TechniqueManager.getAll()) {
                        data.learnTechnique(t.getId());
                    }
                    PlayerShuData.save(player, data);
                    ModNetwork.sendToPlayer(player, new ShuSyncPacket(data));
                    ctx.getSource().sendSuccess(() ->
                        Component.literal("§a已重置术数据，所有术已重新学习"), false);
                    return 1;
                })
            )

            // /shu book - 技能书相关命令
            .then(Commands.literal("book")
                // /shu book give [sword|magic|cross_slash] - 给予技能书
                .then(Commands.literal("give")
                    .executes(ctx -> giveBook(ctx, "sword"))
                    .then(Commands.literal("sword")
                        .executes(ctx -> giveBook(ctx, "sword")))
                    .then(Commands.literal("magic")
                        .executes(ctx -> giveBook(ctx, "magic")))
                    .then(Commands.literal("cross_slash")
                        .executes(ctx -> giveBook(ctx, "cross_slash")))
                )

                // /shu book write <术ID1> [术ID2] ... - 写入技到主手技能书
                .then(Commands.literal("write")
                    .then(Commands.argument("id1", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (Technique t : TechniqueManager.getAll()) {
                                builder.suggest(t.getId());
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> writeBook(ctx, StringArgumentType.getString(ctx, "id1")))
                        .then(Commands.argument("id2", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                for (Technique t : TechniqueManager.getAll()) {
                                    builder.suggest(t.getId());
                                }
                                return builder.buildFuture();
                            })
                            .executes(ctx -> writeBook(ctx,
                                StringArgumentType.getString(ctx, "id1"),
                                StringArgumentType.getString(ctx, "id2")))
                            .then(Commands.argument("id3", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (Technique t : TechniqueManager.getAll()) {
                                        builder.suggest(t.getId());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> writeBook(ctx,
                                    StringArgumentType.getString(ctx, "id1"),
                                    StringArgumentType.getString(ctx, "id2"),
                                    StringArgumentType.getString(ctx, "id3")))
                            )
                        )
                    )
                )

                // /shu book list - 列出主手技能书内容
                .then(Commands.literal("list")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        ItemStack mainHand = player.getMainHandItem();

                        if (mainHand.isEmpty() || !(mainHand.getItem() instanceof SkillBookItem)) {
                            ctx.getSource().sendFailure(Component.literal("§c请先手持技能书"));
                            return 0;
                        }

                        Skill skill = SkillBookItem.getSkill(mainHand);
                        if (skill == null) {
                            ctx.getSource().sendFailure(Component.literal("§c这本书是空的"));
                            return 0;
                        }

                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§6=== " + skill.getName() + " ==="), false);
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§7类型: " + (skill.getType() == SkillType.SWORD ? "§c剑术" : "§9法术")), false);
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§7组成术:"), false);
                        for (Technique t : skill.getTechniques()) {
                            ctx.getSource().sendSuccess(() ->
                                Component.literal("  §f- " + t.getName() + " §7(" + t.getType() + ")"), false);
                        }
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§7总伤害: §c" + skill.getTotalBaseDamage()), false);
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§7总冷却: §b" + skill.getTotalBaseCooldown() + "t"), false);
                        return 1;
                    })
                )

                // /shu book bind [slot] - 将主手技能书绑定到副手武器
                .then(Commands.literal("bind")
                    .executes(ctx -> bindBookToWeapon(ctx, -1)) // 自动选择槽位
                    .then(Commands.argument("slot", IntegerArgumentType.integer(1, 4))
                        .executes(ctx -> bindBookToWeapon(ctx, IntegerArgumentType.getInteger(ctx, "slot") - 1))
                    )
                )
            )
        );
    }

    /**
     * 给予技能书
     */
    private static int giveBook(CommandContext<CommandSourceStack> ctx, String type) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();

        ItemStack book;
        Skill skill;
        String bookName;

        switch (type) {
            case "magic":
                book = new ItemStack(ModItems.SKILL_BOOK_MAGIC.get());
                List<Technique> magicTechs = new ArrayList<>();
                magicTechs.add(TechniqueManager.getById("magic_arcane_missile"));
                magicTechs.add(TechniqueManager.getById("universal_lock_target"));
                skill = new Skill("追踪飞弹", SkillType.MAGIC, magicTechs);
                bookName = "法术书";
                break;

            case "cross_slash":
                book = new ItemStack(ModItems.SKILL_BOOK_SWORD.get());
                List<Technique> crossTechs = new ArrayList<>();
                crossTechs.add(TechniqueManager.getById("sword_horizontal_slash"));
                crossTechs.add(TechniqueManager.getById("sword_vertical_slash"));
                skill = new Skill("十字斩", SkillType.SWORD, crossTechs);
                bookName = "剑术书「十字斩」";
                break;

            default: // sword
                book = new ItemStack(ModItems.SKILL_BOOK_SWORD.get());
                List<Technique> swordTechs = new ArrayList<>();
                swordTechs.add(TechniqueManager.getById("sword_thrust"));
                swordTechs.add(TechniqueManager.getById("movement_dash"));
                skill = new Skill("突击刺击", SkillType.SWORD, swordTechs);
                bookName = "剑术书";
                break;
        }

        SkillBookItem.setSkill(book, skill);
        player.getInventory().add(book);
        ctx.getSource().sendSuccess(() ->
            Component.literal("§a已给予" + bookName), false);
        return 1;
    }

    /**
     * 写入技到主手技能书
     */
    private static int writeBook(CommandContext<CommandSourceStack> ctx, String... ids) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack mainHand = player.getMainHandItem();

        if (mainHand.isEmpty() || !(mainHand.getItem() instanceof SkillBookItem)) {
            DebugLog.log("写书拒绝：主手不是技能书");
            ctx.getSource().sendFailure(Component.literal("§c请先手持技能书"));
            return 0;
        }

        SkillBookItem bookItem = (SkillBookItem) mainHand.getItem();
        List<Technique> techs = new ArrayList<>();
        List<ShuAttribute> embryos = new ArrayList<>();
        List<String> names = new ArrayList<>();

        boolean allowMismatchedCarrier = false;
        for (String id : ids) {
            Technique scan = TechniqueManager.getById(id);
            if (scan != null && scan.getType() == TechniqueType.UNIVERSAL && scan.isEnchantment()) {
                allowMismatchedCarrier = true;
                break;
            }
        }

        for (String id : ids) {
            Technique t = TechniqueManager.getById(id);
            if (t == null) {
                DebugLog.log("写书拒绝：未知术 " + id);
                ctx.getSource().sendFailure(Component.literal("§c未知的术: " + id));
                return 0;
            }
            if (bookItem.getSkillType() == SkillType.SWORD && !t.getType().canUseForSwordSkill()) {
                if (allowMismatchedCarrier && t.getBaseDamage() > 0) {
                    DebugLog.log("写书放行：" + t.getName() + " 随通用附魔进入剑术书");
                } else {
                    DebugLog.log("写书拒绝：" + t.getId() + " 类型=" + t.getType() + " 不能写入剑术书");
                    ctx.getSource().sendFailure(Component.literal("§c" + t.getName() + "不能用于剑术"));
                    return 0;
                }
            }
            if (bookItem.getSkillType() == SkillType.MAGIC && !t.getType().canUseForMagicSkill()) {
                if (allowMismatchedCarrier && t.getBaseDamage() > 0) {
                    DebugLog.log("写书放行：" + t.getName() + " 随通用附魔进入法术书");
                } else {
                    DebugLog.log("写书拒绝：" + t.getId() + " 类型=" + t.getType() + " 不能写入法术书");
                    ctx.getSource().sendFailure(Component.literal("§c" + t.getName() + "不能用于法术"));
                    return 0;
                }
            }
            techs.add(t);
            embryos.add(findEmbryo(player, t.getId()));
            names.add(t.getName());
        }

        // 生成技名称
        String skillName = String.join("+", names);
        Skill skill = new Skill(skillName, bookItem.getSkillType(), techs, embryos);
        SkillBookItem.setSkill(mainHand, skill);

        List<String> embryoIds = new ArrayList<>();
        for (ShuAttribute embryo : embryos) {
            if (embryo != ShuAttribute.NONE) {
                embryoIds.add(embryo.getId());
            }
        }
        String extra = embryoIds.isEmpty() ? "" : " §7原胚:" + String.join(",", embryoIds);
        DebugLog.log("写书成功：" + bookItem.getSkillType() + " " + skillName + extra.replace("§7", ""));
        final String message = "§a已写入技「" + skillName + "」到技能书" + extra;
        ctx.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static ShuAttribute findEmbryo(ServerPlayer player, String spellId) {
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() instanceof SkillStoneItem) {
                SkillStoneData data = SkillStoneItem.getData(stack);
                if (data != null && spellId.equals(data.getSpellId())) {
                    return data.getEmbryoAttribute();
                }
            }
        }
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty() && offhand.getItem() instanceof SkillStoneItem) {
            SkillStoneData data = SkillStoneItem.getData(offhand);
            if (data != null && spellId.equals(data.getSpellId())) {
                return data.getEmbryoAttribute();
            }
        }
        return ShuAttribute.NONE;
    }

    /**
     * 将主手技能书绑定到副手武器
     */
    private static int bindBookToWeapon(CommandContext<CommandSourceStack> ctx, int slot) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        // 检查主手是否是技能书
        if (mainHand.isEmpty() || !(mainHand.getItem() instanceof SkillBookItem)) {
            ctx.getSource().sendFailure(Component.literal("§c请在主手持有技能书"));
            return 0;
        }

        // 检查副手是否是武器
        if (offHand.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("§c请在副手持有武器"));
            return 0;
        }

        Skill skill = SkillBookItem.getSkill(mainHand);
        if (skill == null) {
            ctx.getSource().sendFailure(Component.literal("§c技能书是空的"));
            return 0;
        }

        // 如果指定了槽位，直接绑定
        if (slot >= 0 && slot < 4) {
            if (SkillBookItem.bindBookToWeapon(mainHand, offHand, slot)) {
                ctx.getSource().sendSuccess(() ->
                    Component.literal("§a已将「" + skill.getName() + "」绑定到槽位" + (slot + 1)), false);
                // 消耗技能书
                mainHand.shrink(1);
                return 1;
            } else {
                ctx.getSource().sendFailure(Component.literal("§c绑定失败，该槽位可能已有技或武器类型不兼容"));
                return 0;
            }
        }

        // 自动选择第一个空槽位
        CompoundTag weaponTag = offHand.getTag();
        for (int i = 0; i < 4; i++) {
            if (weaponTag == null || !weaponTag.contains("BoundSkill_" + i)) {
                if (SkillBookItem.bindBookToWeapon(mainHand, offHand, i)) {
                    final int boundSlot = i;
                    ctx.getSource().sendSuccess(() ->
                        Component.literal("§a已将「" + skill.getName() + "」绑定到槽位" + (boundSlot + 1)), false);
                    // 消耗技能书
                    mainHand.shrink(1);
                    return 1;
                }
            }
        }

        ctx.getSource().sendFailure(Component.literal("§c没有空闲的技槽位"));
        return 0;
    }
}

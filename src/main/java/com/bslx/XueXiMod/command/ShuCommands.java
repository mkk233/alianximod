package com.bslx.XueXiMod.command;

import com.bslx.XueXiMod.debug.DebugLog;
import com.bslx.XueXiMod.network.CastCooldown;
import com.bslx.XueXiMod.item.SkillBookItem;
import com.bslx.XueXiMod.item.ShuEmbryoItem;
import com.bslx.XueXiMod.item.SkillStoneItem;
import com.bslx.XueXiMod.item.ModItems;
import com.bslx.XueXiMod.shu.SkillStoneData;
import com.bslx.XueXiMod.shu.balance.BalanceDisplay;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.player.PlayerShuData;
import com.bslx.XueXiMod.network.ModNetwork;
import com.bslx.XueXiMod.network.ShuSyncPacket;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueAccess;
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
 * /shu learn all - 学会全部需要学习的术式，并把剑术、法术熟练升到能解锁它们
 * /shu reset  - 重置玩家术数据
 * /shu mana <上限> - 设置魔力上限并补满
 * /shu rank - 查看剑术和法术的熟练等级、熟练点
 * /shu rank <sword|magic> <等级> [熟练点] - 直接改到该等级，用于解锁高复杂度术式
 * /shu entities <数量> - 设置技能每次生成的实体数量
 * /shu cd off|on - 临时忽略或恢复全部释放冷却
 */
public class ShuCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("shu")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("give")
                .executes(commandContext -> {
                    ServerPlayer player = commandContext.getSource().getPlayerOrException();
                    ItemStack blank = ShuEmbryoItem.create(ShuAttribute.NONE);
                    blank.setCount(8);
                    ItemStack wood = ShuEmbryoItem.create(ShuAttribute.WOOD);
                    wood.setCount(8);
                    player.getInventory().add(blank);
                    player.getInventory().add(wood);
                    commandContext.getSource().sendSuccess(() ->
                        Component.literal("§a已给予空白原胚×8、木属性原胚×8"), false);
                    return 1;
                })
            )

            .then(Commands.literal("stone")
                .then(Commands.argument("id", StringArgumentType.word())
                    .suggests((commandContext, builder) -> {
                        for (Technique technique : TechniqueManager.playerCatalog()) {
                            builder.suggest(technique.getId());
                        }
                        return builder.buildFuture();
                    })
                    .executes(commandContext -> {
                        ServerPlayer player = commandContext.getSource().getPlayerOrException();
                        String techniqueId = StringArgumentType.getString(commandContext, "id");
                        Technique technique = TechniqueManager.getById(techniqueId);
                        if (technique == null) {
                            commandContext.getSource().sendFailure(Component.literal("§c未知的术: " + techniqueId));
                            return 0;
                        }
                        if (!TechniqueAccess.allowsPlayerUse(technique)) {
                            commandContext.getSource().sendFailure(Component.literal(TechniqueAccess.DENIAL_TEXT));
                            return 0;
                        }
                        SkillStoneData data = SkillStoneData.imprint(technique, ShuAttribute.NONE);
                        player.getInventory().add(SkillStoneItem.create(data));
                        commandContext.getSource().sendSuccess(() ->
                            Component.literal("§a已给予术式：" + technique.getName()), false);
                        return 1;
                    })
                )
            )

            // /shu list - 列出所有术
            .then(Commands.literal("list")
                .executes(commandContext -> {
                    commandContext.getSource().sendSuccess(() ->
                        Component.literal("§6=== 已注册的术 ==="), false);
                    for (Technique technique : TechniqueManager.playerCatalog()) {
                        commandContext.getSource().sendSuccess(() ->
                            Component.literal("§f" + technique.getId() + " §7- " + technique.getName() +
                                " §8[" + technique.getType() + "] §c伤害:" + BalanceDisplay.shownStructuralDamage(technique.getId(), technique.getBaseDamage()) +
                                " §b冷却:" + BalanceDisplay.cooldown(technique.getId(), technique.getBaseCooldown()) + "t"), false);
                    }
                    return 1;
                })
            )

            // /shu learn all - 学会全部需要学习的术式
            // /shu learn <id> - 学习术
            .then(Commands.literal("learn")
                .then(Commands.literal("all")
                    .executes(ShuCommands::learnAllRequired))
                .then(Commands.argument("id", StringArgumentType.word())
                    .suggests((commandContext, builder) -> {
                        for (Technique technique : TechniqueManager.playerCatalog()) {
                            builder.suggest(technique.getId());
                        }
                        return builder.buildFuture();
                    })
                    .executes(commandContext -> {
                        ServerPlayer player = commandContext.getSource().getPlayerOrException();
                        String techniqueId = StringArgumentType.getString(commandContext, "id");
                        Technique technique = TechniqueManager.getById(techniqueId);
                        if (technique == null) {
                            commandContext.getSource().sendFailure(Component.literal("§c未知的术: " + techniqueId));
                            return 0;
                        }
                        if (!TechniqueAccess.allowsPlayerUse(technique)) {
                            commandContext.getSource().sendFailure(Component.literal(TechniqueAccess.DENIAL_TEXT));
                            return 0;
                        }
                        PlayerShuData data = PlayerShuData.get(player);
                        data.learnTechnique(techniqueId);
                        PlayerShuData.save(player, data);
                        ModNetwork.sendToPlayer(player, new ShuSyncPacket(data));
                        commandContext.getSource().sendSuccess(() ->
                            Component.literal("§a已学习: " + technique.getName()), false);
                        return 1;
                    })
                )
            )

            // /shu reset - 重置数据
            .then(Commands.literal("reset")
                .executes(commandContext -> {
                    ServerPlayer player = commandContext.getSource().getPlayerOrException();
                    // 清空数据后重新初始化
                    CompoundTag emptyTag = new CompoundTag();
                    player.getPersistentData().put("xueximod_shu_data", emptyTag);
                    // 重新给予基础术
                    PlayerShuData data = PlayerShuData.get(player);
                    for (Technique technique : TechniqueManager.playerCatalog()) {
                        data.learnTechnique(technique.getId());
                    }
                    PlayerShuData.save(player, data);
                    ModNetwork.sendToPlayer(player, new ShuSyncPacket(data));
                    commandContext.getSource().sendSuccess(() ->
                        Component.literal("§a已重置术数据，所有术已重新学习"), false);
                    return 1;
                })
            )

            .then(Commands.literal("mana")
                .then(Commands.argument("upperBound", IntegerArgumentType.integer(1))
                    .executes(ShuCommands::setManaUpperBound))
            )

            .then(Commands.literal("rank")
                .executes(ShuCommands::showRank)
                .then(Commands.literal("sword")
                    .then(Commands.argument("level", IntegerArgumentType.integer(0, PlayerShuData.TYPE_LEVEL_UPPER_BOUND))
                        .executes(commandContext -> setRank(commandContext, TechniqueType.SWORD, false))
                        .then(Commands.argument("points", IntegerArgumentType.integer(0, 100000))
                            .executes(commandContext -> setRank(commandContext, TechniqueType.SWORD, true)))))
                .then(Commands.literal("magic")
                    .then(Commands.argument("level", IntegerArgumentType.integer(0, PlayerShuData.TYPE_LEVEL_UPPER_BOUND))
                        .executes(commandContext -> setRank(commandContext, TechniqueType.MAGIC, false))
                        .then(Commands.argument("points", IntegerArgumentType.integer(0, 100000))
                            .executes(commandContext -> setRank(commandContext, TechniqueType.MAGIC, true)))))
            )

            .then(Commands.literal("entities")
                .then(Commands.argument("entityCount", IntegerArgumentType.integer(1, SkillEntityCount.UPPER_BOUND))
                    .executes(ShuCommands::setEntityCount))
            )

            // /shu book - 技能书相关命令
            .then(Commands.literal("book")
                // /shu book give [sword|magic|cross_slash] - 给予技能书
                .then(Commands.literal("give")
                    .executes(commandContext -> giveBook(commandContext, "sword"))
                    .then(Commands.literal("sword")
                        .executes(commandContext -> giveBook(commandContext, "sword")))
                    .then(Commands.literal("magic")
                        .executes(commandContext -> giveBook(commandContext, "magic")))
                    .then(Commands.literal("cross_slash")
                        .executes(commandContext -> giveBook(commandContext, "cross_slash")))
                )

                // /shu book write <术ID1> [术ID2] ... - 写入技到主手技能书
                .then(Commands.literal("write")
                    .then(Commands.argument("id1", StringArgumentType.word())
                        .suggests((commandContext, builder) -> {
                            for (Technique technique : TechniqueManager.playerCatalog()) {
                                builder.suggest(technique.getId());
                            }
                            return builder.buildFuture();
                        })
                        .executes(commandContext -> writeBook(commandContext, StringArgumentType.getString(commandContext, "id1")))
                        .then(Commands.argument("id2", StringArgumentType.word())
                            .suggests((commandContext, builder) -> {
                                for (Technique technique : TechniqueManager.playerCatalog()) {
                                    builder.suggest(technique.getId());
                                }
                                return builder.buildFuture();
                            })
                            .executes(commandContext -> writeBook(commandContext,
                                StringArgumentType.getString(commandContext, "id1"),
                                StringArgumentType.getString(commandContext, "id2")))
                            .then(Commands.argument("id3", StringArgumentType.word())
                                .suggests((commandContext, builder) -> {
                                    for (Technique technique : TechniqueManager.playerCatalog()) {
                                        builder.suggest(technique.getId());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(commandContext -> writeBook(commandContext,
                                    StringArgumentType.getString(commandContext, "id1"),
                                    StringArgumentType.getString(commandContext, "id2"),
                                    StringArgumentType.getString(commandContext, "id3")))
                            )
                        )
                    )
                )

                // /shu book list - 列出主手技能书内容
                .then(Commands.literal("list")
                    .executes(commandContext -> {
                        ServerPlayer player = commandContext.getSource().getPlayerOrException();
                        ItemStack mainHand = player.getMainHandItem();

                        if (mainHand.isEmpty() || !(mainHand.getItem() instanceof SkillBookItem)) {
                            commandContext.getSource().sendFailure(Component.literal("§c请先手持技能书"));
                            return 0;
                        }

                        Skill skill = SkillBookItem.getSkill(mainHand);
                        if (skill == null) {
                            commandContext.getSource().sendFailure(Component.literal("§c这本书是空的"));
                            return 0;
                        }

                        commandContext.getSource().sendSuccess(() ->
                            Component.literal("§6=== " + skill.getName() + " ==="), false);
                        commandContext.getSource().sendSuccess(() ->
                            Component.literal("§7类型: " + (skill.getType() == SkillType.SWORD ? "§c剑术" : "§9术法")), false);
                        commandContext.getSource().sendSuccess(() ->
                            Component.literal("§7组成术:"), false);
                        for (Technique technique : skill.getTechniques()) {
                            commandContext.getSource().sendSuccess(() ->
                                Component.literal("  §f- " + technique.getName() + " §7(" + technique.getType() + ")"), false);
                        }
                        commandContext.getSource().sendSuccess(() ->
                            Component.literal("§7总伤害: §c" + skill.displayedTotalDamage()), false);
                        commandContext.getSource().sendSuccess(() ->
                            Component.literal("§7总冷却: §b" + skill.displayedTotalCooldown() + "t"), false);
                        return 1;
                    })
                )

                // /shu book bind [slot] - 将主手技能书绑定到副手武器
                .then(Commands.literal("bind")
                    .executes(commandContext -> bindBookToWeapon(commandContext, -1)) // 自动选择槽位
                    .then(Commands.argument("slot", IntegerArgumentType.integer(1, 4))
                        .executes(commandContext -> bindBookToWeapon(commandContext, IntegerArgumentType.getInteger(commandContext, "slot") - 1))
                    )
                )
            )
            .then(Commands.literal("cd")
                .executes(ShuCommands::reportCooldown)
                .then(Commands.literal("off")
                    .executes(commandContext -> setCooldownIgnored(commandContext, true)))
                .then(Commands.literal("on")
                    .executes(commandContext -> setCooldownIgnored(commandContext, false)))
            )
            .then(CloneCommands.branch())
        );
    }

    /**
     * 给予技能书
     */
    private static int giveBook(CommandContext<CommandSourceStack> commandContext, String type) throws CommandSyntaxException {
        ServerPlayer player = commandContext.getSource().getPlayerOrException();

        ItemStack book;
        Skill skill;
        String bookName;

        switch (type) {
            case "magic":
                book = new ItemStack(ModItems.SKILL_BOOK_MAGIC.get());
                List<Technique> magicTechs = new ArrayList<>();
                magicTechs.add(TechniqueManager.getById("magic_arcane_missile"));
                magicTechs.add(TechniqueManager.getById("universal_lock_target"));
                skill = new Skill("追踪灵符", SkillType.MAGIC, magicTechs);
                bookName = "术法集";
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
        commandContext.getSource().sendSuccess(() ->
            Component.literal("§a已给予" + bookName), false);
        return 1;
    }

    /**
     * 写入技到主手技能书
     */
    private static int writeBook(CommandContext<CommandSourceStack> commandContext, String... ids) throws CommandSyntaxException {
        ServerPlayer player = commandContext.getSource().getPlayerOrException();
        ItemStack mainHand = player.getMainHandItem();

        if (mainHand.isEmpty() || !(mainHand.getItem() instanceof SkillBookItem)) {
            DebugLog.log("写书拒绝：主手不是技能书");
            commandContext.getSource().sendFailure(Component.literal("§c请先手持技能书"));
            return 0;
        }

        SkillBookItem bookItem = (SkillBookItem) mainHand.getItem();
        List<Technique> techs = new ArrayList<>();
        List<ShuAttribute> embryos = new ArrayList<>();
        List<String> names = new ArrayList<>();

        boolean allowMismatchedCarrier = false;
        for (String techniqueId : ids) {
            Technique scan = TechniqueManager.getById(techniqueId);
            if (scan != null && scan.getType() == TechniqueType.UNIVERSAL && scan.isEnchantment()) {
                allowMismatchedCarrier = true;
                break;
            }
        }

        for (String techniqueId : ids) {
            Technique technique = TechniqueManager.getById(techniqueId);
            if (technique == null) {
                DebugLog.log("写书拒绝：未知术 " + techniqueId);
                commandContext.getSource().sendFailure(Component.literal("§c未知的术: " + techniqueId));
                return 0;
            }
            if (!TechniqueAccess.allowsPlayerUse(technique)) {
                commandContext.getSource().sendFailure(Component.literal(TechniqueAccess.DENIAL_TEXT));
                return 0;
            }
            if (bookItem.getSkillType() == SkillType.SWORD && !technique.getType().canUseForSwordSkill()) {
                if (allowMismatchedCarrier && technique.getBaseDamage() > 0) {
                    DebugLog.log("写书放行：" + technique.getName() + " 随通用附魔进入剑术书");
                } else {
                    DebugLog.log("写书拒绝：" + technique.getId() + " 类型=" + technique.getType() + " 不能写入剑术书");
                    commandContext.getSource().sendFailure(Component.literal("§c" + technique.getName() + "不能用于剑术"));
                    return 0;
                }
            }
            if (bookItem.getSkillType() == SkillType.MAGIC && !technique.getType().canUseForMagicSkill()) {
                if (allowMismatchedCarrier && technique.getBaseDamage() > 0) {
                    DebugLog.log("写书放行：" + technique.getName() + " 随通用附魔进入法术书");
                } else {
                    DebugLog.log("写书拒绝：" + technique.getId() + " 类型=" + technique.getType() + " 不能写入法术书");
                    commandContext.getSource().sendFailure(Component.literal("§c" + technique.getName() + "不能用于术法"));
                    return 0;
                }
            }
            techs.add(technique);
            embryos.add(findEmbryo(player, technique.getId()));
            names.add(technique.getName());
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
        commandContext.getSource().sendSuccess(() -> Component.literal(message), false);
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
    private static int bindBookToWeapon(CommandContext<CommandSourceStack> commandContext, int slot) throws CommandSyntaxException {
        ServerPlayer player = commandContext.getSource().getPlayerOrException();
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        // 检查主手是否是技能书
        if (mainHand.isEmpty() || !(mainHand.getItem() instanceof SkillBookItem)) {
            commandContext.getSource().sendFailure(Component.literal("§c请在主手持有技能书"));
            return 0;
        }

        // 检查副手是否是武器
        if (offHand.isEmpty()) {
            commandContext.getSource().sendFailure(Component.literal("§c请在副手持有武器"));
            return 0;
        }

        Skill skill = SkillBookItem.getSkill(mainHand);
        if (skill == null) {
            commandContext.getSource().sendFailure(Component.literal("§c技能书是空的"));
            return 0;
        }

        // 如果指定了槽位，直接绑定
        if (slot >= 0 && slot < 4) {
            if (SkillBookItem.bindBookToWeapon(mainHand, offHand, slot)) {
                commandContext.getSource().sendSuccess(() ->
                    Component.literal("§a已将「" + skill.getName() + "」绑定到槽位" + (slot + 1)), false);
                // 消耗技能书
                mainHand.shrink(1);
                return 1;
            } else {
                commandContext.getSource().sendFailure(Component.literal("§c绑定失败，该槽位可能已有技或武器类型不兼容"));
                return 0;
            }
        }

        // 自动选择第一个空槽位
        CompoundTag weaponTag = offHand.getTag();
        for (int slotIndex = 0; slotIndex < 4; slotIndex++) {
            if (weaponTag == null || !weaponTag.contains("BoundSkill_" + slotIndex)) {
                if (SkillBookItem.bindBookToWeapon(mainHand, offHand, slotIndex)) {
                    final int boundSlot = slotIndex;
                    commandContext.getSource().sendSuccess(() ->
                        Component.literal("§a已将「" + skill.getName() + "」绑定到槽位" + (boundSlot + 1)), false);
                    // 消耗技能书
                    mainHand.shrink(1);
                    return 1;
                }
            }
        }

        commandContext.getSource().sendFailure(Component.literal("§c没有空闲的技槽位"));
        return 0;
    }

    /** 复杂度大于 0 的术式进服不会自动学会。这里全部学会，并把对应熟练升到能解锁的等级。 */
    private static int learnAllRequired(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        ServerPlayer player = commandContext.getSource().getPlayerOrException();
        PlayerShuData data = PlayerShuData.get(player);
        int newlyLearned = 0;
        int swordRequirement = data.getSwordLevel();
        int magicRequirement = data.getMagicLevel();
        for (Technique technique : TechniqueManager.playerCatalog()) {
            if (technique.getUnlockLevel() <= 0) {
                continue;
            }
            if (!data.hasLearned(technique.getId())) {
                data.learnTechnique(technique.getId());
                newlyLearned++;
            }
            if (technique.getType() == TechniqueType.SWORD) {
                swordRequirement = Math.max(swordRequirement, technique.getComplexity());
            } else if (technique.getType() == TechniqueType.MAGIC) {
                magicRequirement = Math.max(magicRequirement, technique.getComplexity());
            }
        }
        int swordLevel = Math.min(PlayerShuData.TYPE_LEVEL_UPPER_BOUND, swordRequirement);
        int magicLevel = Math.min(PlayerShuData.TYPE_LEVEL_UPPER_BOUND, magicRequirement);
        if (swordLevel != data.getSwordLevel()) {
            data.setTypeRank(TechniqueType.SWORD, swordLevel, PlayerShuData.pointsAtLevel(swordLevel));
        }
        if (magicLevel != data.getMagicLevel()) {
            data.setTypeRank(TechniqueType.MAGIC, magicLevel, PlayerShuData.pointsAtLevel(magicLevel));
        }
        PlayerShuData.save(player, data);
        PlayerShuData refreshed = PlayerShuData.get(player);
        ModNetwork.sendToPlayer(player, new ShuSyncPacket(refreshed));
        int learnedCount = newlyLearned;
        commandContext.getSource().sendSuccess(() -> Component.literal("§a已学会 " + learnedCount
                + " 条需要学习的术式。剑术熟练 " + refreshed.getSwordLevel()
                + "，术法熟练 " + refreshed.getMagicLevel()), false);
        return learnedCount;
    }

    private static int showRank(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        ServerPlayer player = commandContext.getSource().getPlayerOrException();
        PlayerShuData data = PlayerShuData.get(player);
        commandContext.getSource().sendSuccess(() -> Component.literal(rankLine("§c剑术", TechniqueType.SWORD, data)), false);
        commandContext.getSource().sendSuccess(() -> Component.literal(rankLine("§9术法", TechniqueType.MAGIC, data)), false);
        return 1;
    }

    private static int setRank(CommandContext<CommandSourceStack> commandContext, TechniqueType type, boolean customPoints)
            throws CommandSyntaxException {
        ServerPlayer player = commandContext.getSource().getPlayerOrException();
        int level = IntegerArgumentType.getInteger(commandContext, "level");
        int points = customPoints ? IntegerArgumentType.getInteger(commandContext, "points") : PlayerShuData.pointsAtLevel(level);
        PlayerShuData data = PlayerShuData.get(player);
        data.setTypeRank(type, level, points);
        PlayerShuData.save(player, data);
        PlayerShuData refreshed = PlayerShuData.get(player);
        ModNetwork.sendToPlayer(player, new ShuSyncPacket(refreshed));
        String label = type == TechniqueType.SWORD ? "剑术" : "术法";
        commandContext.getSource().sendSuccess(() -> Component.literal(rankLine("§a" + label, type, refreshed)
                + "。复杂度不超过 " + refreshed.getTypeLevel(type) + " 的" + label + "可以解锁"), false);
        return 1;
    }

    private static String rankLine(String label, TechniqueType type, PlayerShuData data) {
        int currentLevel = data.getTypeLevel(type);
        int currentPoints = type == TechniqueType.SWORD ? data.getSwordUsage() : data.getMagicUsage();
        int stillNeeded = data.getUsageToNextLevel(type);
        if (stillNeeded < 0) {
            return label + " §f熟练等级 " + currentLevel + "，熟练点 " + currentPoints + "，已满级";
        }
        return label + " §f熟练等级 " + currentLevel + "，熟练点 " + currentPoints + "，距下一级还差 " + stillNeeded;
    }

    private static int setManaUpperBound(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        ServerPlayer player = commandContext.getSource().getPlayerOrException();
        int upperBound = IntegerArgumentType.getInteger(commandContext, "upperBound");
        PlayerShuData data = PlayerShuData.get(player);
        data.setMagicMax(upperBound);
        PlayerShuData.save(player, data);
        ModNetwork.sendToPlayer(player, new ShuSyncPacket(data));
        commandContext.getSource().sendSuccess(() ->
            Component.literal("§a法力上限已设为 " + upperBound + "，当前法力已补满"), false);
        return 1;
    }

    private static int reportCooldown(CommandContext<CommandSourceStack> commandContext) {
        String state = CastCooldown.cooldownIgnored() ? "§e释放冷却已关闭" : "§a释放冷却生效中";
        commandContext.getSource().sendSuccess(() -> Component.literal(state + "§7。/shu cd off 关闭，/shu cd on 恢复"), false);
        return 1;
    }

    private static int setCooldownIgnored(CommandContext<CommandSourceStack> commandContext, boolean ignored) {
        CastCooldown.setCooldownIgnored(ignored);
        String state = ignored ? "§e已关闭全部释放冷却，重新进服后会恢复" : "§a已恢复释放冷却";
        commandContext.getSource().sendSuccess(() -> Component.literal(state), false);
        return 1;
    }

    private static int setEntityCount(CommandContext<CommandSourceStack> commandContext) {
        int entityCount = IntegerArgumentType.getInteger(commandContext, "entityCount");
        SkillEntityCount.setEntityCount(entityCount);
        commandContext.getSource().sendSuccess(() ->
            Component.literal("§a技能每次生成的实体数量已设为 " + entityCount), false);
        return 1;
    }
}

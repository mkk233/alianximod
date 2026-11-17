package com.bslx.XueXiMod.shu;

import com.bslx.XueXiMod.shu.technique.EnchantmentDefinitions;
import com.bslx.XueXiMod.shu.technique.TechniqueEffectRegistry;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinitions;
import com.bslx.XueXiMod.shu.technique.release.clone.CloneBalance;
import com.bslx.XueXiMod.shu.technique.release.clone.CloneTechniqueRegistry;
import com.bslx.XueXiMod.shu.technique.release.clone.water.WaterPhantomBalance;
import com.bslx.XueXiMod.shu.technique.release.clone.water.WaterPhantomDefinition;
import com.bslx.XueXiMod.shu.technique.release.batch.GloomDomainEffect;
import com.bslx.XueXiMod.shu.technique.release.WeepingSettings;
import com.bslx.XueXiMod.shu.technique.effects.CastAnchorEffect;
import com.bslx.XueXiMod.shu.technique.effects.FentianEffect;
import com.bslx.XueXiMod.shu.technique.effects.FireBurstEffect;
import com.bslx.XueXiMod.shu.technique.effects.FireOrbEffect;
import com.bslx.XueXiMod.shu.technique.effects.LiaoliEffect;
import com.bslx.XueXiMod.shu.technique.effects.LiaoyuanEffect;
import com.bslx.XueXiMod.shu.technique.effects.HeatBeamEffect;
import com.bslx.XueXiMod.shu.technique.effects.Scatter3dEffect;
import com.bslx.XueXiMod.shu.technique.effects.ScorchEffect;
import com.bslx.XueXiMod.shu.technique.effects.SpaceCutEffect;
import com.bslx.XueXiMod.shu.technique.effects.TripleCastEffect;
import com.bslx.XueXiMod.shu.technique.effects.MagicArcaneMissileEffect;
import com.bslx.XueXiMod.shu.technique.effects.MagicCreateWaterEffect;
import com.bslx.XueXiMod.shu.technique.effects.MagicIgniteEffect;
import com.bslx.XueXiMod.shu.technique.effects.MovementDashEffect;
import com.bslx.XueXiMod.shu.technique.effects.MovementLeapEffect;
import com.bslx.XueXiMod.shu.technique.effects.SwordHorizontalSlashEffect;
import com.bslx.XueXiMod.shu.technique.effects.SwordThrustEffect;
import com.bslx.XueXiMod.shu.technique.effects.SwordVerticalSlashEffect;
import com.bslx.XueXiMod.shu.technique.effects.TestSustainedEffect;
import com.bslx.XueXiMod.shu.technique.effects.MetalNeedleEffect;
import com.bslx.XueXiMod.shu.technique.effects.PhantomSwordEffect;
import com.bslx.XueXiMod.shu.technique.effects.ParasiteSeedEffect;
import com.bslx.XueXiMod.shu.technique.effects.SwordComeEffect;
import com.bslx.XueXiMod.shu.technique.effects.SwordReturnAllEffect;
import com.bslx.XueXiMod.shu.technique.effects.StoneHammerEffect;
import com.bslx.XueXiMod.shu.technique.effects.TideSeverEffect;
import com.bslx.XueXiMod.shu.technique.effects.UniversalLockTargetEffect;
import com.bslx.XueXiMod.shu.technique.effects.WeepingCurtainEffect;
import com.bslx.XueXiMod.shu.technique.effects.WaterYieldingEffect;
import com.bslx.XueXiMod.shu.technique.release.WaterCurveFunctions;

/**
 * 内置术注册
 * 注册所有预设的术到 TechniqueManager
 */
public class ModTechniques {

    // === 剑术 ===
    public static final Technique SWORD_THRUST = new Technique(
            "sword_thrust", "刺击", TechniqueType.SWORD,
            6.0f, 15, 20, "正前方单体突刺"
    );
    public static final Technique SWORD_HORIZONTAL_SLASH = new Technique(
            "sword_horizontal_slash", "横劈", TechniqueType.SWORD,
            4.0f, 20, 20, "正前方扇形AOE"
    );
    public static final Technique SWORD_VERTICAL_SLASH = new Technique(
            "sword_vertical_slash", "竖劈", TechniqueType.SWORD,
            8.0f, 25, 20, "高倍率单体重击"
    );

    // === 动作 ===
    public static final Technique MOVEMENT_DASH = new Technique(
            "movement_dash", "冲刺", TechniqueType.MOVEMENT,
            2.0f, 30, 20, "向bind前方高速移动，碰撞伤害"
    );
    public static final Technique MOVEMENT_LEAP = new Technique(
            "movement_leap", "跳跃", TechniqueType.MOVEMENT,
            0f, 40, 20, "向bind上方弹射"
    );

    // === 法术 ===
    public static final Technique MAGIC_ARCANE_MISSILE = new Technique(
            "magic_arcane_missile", "奥术飞弹", TechniqueType.MAGIC,
            5.0f, 20, 20, "发射追踪弹幕"
    );
    public static final Technique MAGIC_CREATE_WATER = new Technique(
            "magic_create_water", "生成水", TechniqueType.MAGIC,
            0f, 60, 20, "在目标位置放置水源"
    );
    public static final Technique TEST_SUSTAINED = new Technique(
            "test_sustained", "持续试射", TechniqueType.MAGIC,
            1.0f, 10, 20, "测试用持续弹", 0
    );
    public static final Technique HEAT_BEAM = new Technique(
            "heat_beam", "热光束", TechniqueType.MAGIC,
            3.0f, 20, 20, "持续释放一束随准星转向的火属性光束，对接触的生物造成伤害。每次打中挂 1 点火种", 0, ShuAttribute.FIRE, 1.0f
    );

    // === 通用（剑术书和法术书都能写入）===
    public static final Technique MAGIC_IGNITE = new Technique(
            "magic_ignite", "点火", TechniqueType.UNIVERSAL,
            0f, 40, 20, "点燃目标/方块。附在术式上，或记在武器上时，打中的那次攻击额外挂 2 点火种", 0, ShuAttribute.FIRE, 1.0f, true
    );
    public static final Technique UNIVERSAL_LOCK_TARGET = new Technique(
            "universal_lock_target", "锁定实体", TechniqueType.UNIVERSAL,
            0f, 50, 20, "对最近实体施加发光效果"
    );
    public static final Technique RULE_DUAL_RELEASE = new Technique(
            "rule_dual_release", "双重释放", TechniqueType.UNIVERSAL,
            0f, 20, 20, "按顺序读取后面两格。第一个实际成员立刻出手，之后每个实际成员隔 3 tick。修饰、附魔和内层多重各占一格"
    );
    public static final Technique TRAJECTORY_DROP = new Technique(
            "trajectory_drop", "垂落术式", TechniqueType.UNIVERSAL,
            0f, 20, 20, "将下一个指向性术式的这次路径改为垂落"
    );
    public static final Technique SPACE_CUT = new Technique(
            "space_cut", "空间切割", TechniqueType.SWORD,
            4f, 20, 20, "在落点留下一片静止的切割"
    );
    public static final Technique RULE_CAST_ANCHOR = new Technique(
            "rule_cast_anchor", "落点指定", TechniqueType.UNIVERSAL,
            0f, 0, 20, "有怪则落在怪处，否则落在准星前方"
    );
    public static final Technique RULE_TRIPLE_CAST = new Technique(
            "rule_triple_cast", "一分为三", TechniqueType.UNIVERSAL,
            0f, 0, 20, "复制后面第一个攻击术式三份并汇总伤害"
    );
    public static final Technique RULE_DOMAIN_BIND = new Technique(
            "rule_domain_bind", "领域绑定", TechniqueType.UNIVERSAL,
            0f, 0, 20, "放在领域术式后面，绑定后续攻击链，每20tick自动释放一次"
    );
    public static final Technique RULE_GATHER_ENERGY = new Technique(
            "rule_gather_energy", "聚能", TechniqueType.MAGIC,
            0f, 0, 20, "把后面这次元素法术的尺寸和有限持续时间乘 2。多挂一条再乘一次"
    );
    public static final Technique TRAJECTORY_3D_INTERLEAVE = new Technique(
            "trajectory_3d_interleave", "三维交错", TechniqueType.UNIVERSAL,
            0f, 0, 20, "让每一份在落点周围错开"
    );
    public static final Technique VINE_MARK = new Technique(
            "vine_mark", "缠枝", TechniqueType.UNIVERSAL,
            0f, 40, 20, "命中后短暂缠住目标", 0, ShuAttribute.WOOD, 1.0f, true
    );
    public static final Technique PARASITE_SEED = new Technique(
            "parasite_seed", "寄生种子", TechniqueType.MAGIC,
            4f, 20, 20, "撒出三颗种子，落地成陷阱，踩中爆炸并持续吸取生命", 0, ShuAttribute.WOOD, 1.0f
    );
    public static final Technique TIDE_MARK = new Technique(
            "tide_mark", "潮印", TechniqueType.UNIVERSAL,
            0f, 40, 20, "每条在命中成功后附加2点伤害，目标潮湿+5，施术者水势+2", 0, ShuAttribute.WATER, 1.0f, true
    );
    public static final Technique TIDE_SEVER = new Technique(
            "tide_sever", "薄水切", TechniqueType.SWORD,
            6f, 20, 20, "一道薄而长的水切", 0, ShuAttribute.WATER, 1.0f
    );
    public static final Technique WATER_PHANTOM = new Technique(
            WaterPhantomDefinition.TECHNIQUE_ID, "水幻形", TechniqueType.MAGIC,
            0f, WaterPhantomBalance.COOLDOWN_TICKS, 1,
            "法杖槽0先装水幻形，再装薄水切。普通释放留下水形，潜行立刻散开。默认一次收取部署魔力10和薄水切魔力12，冷却20加20。未适配的下一攻击在收费前拒绝。",
            1, ShuAttribute.WATER, 0f, false, TechniqueAvailability.PLAYER
    );
    public static final Technique SINKING_EDGE = new Technique(
            "sinking_edge", "沉锋", TechniqueType.UNIVERSAL,
            0f, 40, 20, "给攻击加上重量、范围和短促击退", 0, ShuAttribute.EARTH, 1.0f, true
    );
    public static final Technique STONE_HAMMER = new Technique(
            "stone_hammer", "岩槌", TechniqueType.SWORD,
            8f, 25, 20, "厚重的落点重击", 0, ShuAttribute.EARTH, 1.0f
    );
    public static final Technique KEEN_MARK = new Technique(
            "keen_mark", "锐锋印", TechniqueType.UNIVERSAL,
            0f, 40, 20, "给攻击加上金属性伤害并缩短冷却", 0, ShuAttribute.METAL, 1.0f, true
    );
    public static final Technique PHANTOM_SWORD = new Technique(
            "phantom_sword", "幻剑术", TechniqueType.SWORD,
            2f, 40, 20, "在身后凝出复制主手剑的幻剑，停滞后沿准星飞出并插地"
    );
    public static final Technique SWORD_COME = new Technique(
            "sword_come", "剑来", TechniqueType.SWORD,
            0f, 20, 20, "召回周围的剑"
    );
    public static final Technique SWORD_RETURN_ALL = new Technique(
            "sword_return_all", "万剑归宗", TechniqueType.SWORD,
            8f, 200, 20, "把已放出的幻剑和真剑收到身边四面环绕，再逐把飞向锁定目标"
    );
    public static final Technique METAL_NEEDLE = new Technique(
            "metal_needle", "金针", TechniqueType.MAGIC,
            5f, 20, 20, "飞出的金属细针", 0, ShuAttribute.METAL, 1.5f
    );
    public static final Technique FIRE_ORB = new Technique(
            "fire_orb", "火球", TechniqueType.MAGIC,
            5f, 20, 20, "飞出火球，命中造成伤害并点燃。弹道可被改写。每个打中的目标挂 3 点火种", 0, ShuAttribute.FIRE, 1.2f
    );
    public static final Technique FIRE_BURST = new Technique(
            "fire_burst", "爆燃", TechniqueType.MAGIC,
            8f, 40, 20, "撞上生物或方块，或两秒后在三格内爆炸并点燃", 0, ShuAttribute.FIRE, 0.6f
    );
    public static final Technique FIRE_HEAT_UP = new Technique(
            "fire_heat_up", "升温", TechniqueType.MAGIC,
            0f, 500, 20, "进入普通炉心。状态没变则不扣魔力", 0, ShuAttribute.FIRE, 0f
    );
    public static final Technique FIRE_BURN = new Technique(
            "fire_burn", "燃烧", TechniqueType.MAGIC,
            0f, 500, 20, "进入或升级超频炉心，并保留已有加成", 0, ShuAttribute.FIRE, 0f
    );
    public static final Technique FIRE_COOL = new Technique(
            "fire_cool", "冷却", TechniqueType.MAGIC,
            0f, 0, 20, "主动冷却炉心，并放出一次蒸汽", 0, ShuAttribute.FIRE, 0f
    );
    public static final Technique FIRE_LIAOYUAN = new Technique(
            "fire_liaoyuan", "燎原", TechniqueType.MAGIC,
            8f, 600, 20, "铺开半径10格的贴地火场，展开前沿即可命中。本体基础8点、每10刻一次并受一次炉心加成；有火种时足额段为4×(1+0.1n+F)，每段请求10点火种", 20, ShuAttribute.FIRE, 0f
    );
    public static final Technique FIRE_SCORCH_FIELD = new Technique(
            "fire_scorch_field", "灼地", TechniqueType.MAGIC,
            1f, 30, 20, "在脚前合法地表铺开边长4格的贴地火场，持续100刻，接触的敌人按首次和停留周期获得火种",
            0, ShuAttribute.FIRE, 0f
    );
    public static final Technique FIRE_LIAOLI = new Technique(
            "fire_liaoli", "燎里", TechniqueType.SWORD,
            6f, 300, 20, "持剑向前刺击。命中时花掉当时剩余的火种，并打一刀物理", 20, ShuAttribute.FIRE, 0f
    );
    public static final Technique FIRE_FENTIAN = new Technique(
            "fire_fentian", "焚天", TechniqueType.MAGIC,
            2f, 1200, 20, "球内有火种的目标挂上焚天。伤害在挂上时锁定，最多40刻，每段消耗10点火种", 20, ShuAttribute.FIRE, 0f
    );
    public static final Technique TRAJECTORY_TRACKING = new Technique(
            "trajectory_tracking", "追踪弹道", TechniqueType.UNIVERSAL,
            0f, 20, 20, "飞行弹丸以自身5格搜索并持续转向"
    );
    public static final Technique TRAJECTORY_STORM = new Technique(
            "trajectory_storm", "风暴轨迹", TechniqueType.UNIVERSAL,
            0f, 20, 20, "把后面的可变弹道改成龙卷风：从落点上方旋入准星落点，持续术式到达后停在那里"
    );
    public static final Technique WATER_YIELDING_RIBBON = new Technique(
            "water_yielding_ribbon", "断潮", TechniqueType.SWORD,
            40f, 600, 20, "先铺开水面，再从中间劈开。水分开没有伤害，只有中间那一刀", 0, ShuAttribute.WATER, 0f
    );
    public static final Technique WATER_WEEPING_ARC = new Technique(
            "water_weeping_arc", "垂丝", TechniqueType.MAGIC,
            WeepingSettings.BASE_DAMAGE, 20, 20, "前上方垂下一截水帘。只有当前还在的那段水能打到人，聚能只加宽加厚", 0, ShuAttribute.WATER, 0f
    );
    public static final Technique FIRE_METEORITE = new Technique(
            "fire_meteorite", "陨岩", TechniqueType.MAGIC,
            24f, 40, 20, "沿抛物线落下的岩体。物理24，中心半径1.5，总高3。路径可改写", 6, ShuAttribute.FIRE, 0.8f
    );
    public static final Technique FIRE_SHOCK_RING = new Technique(
            "fire_shock_ring", "热波", TechniqueType.MAGIC,
            4f, 16, 20, "固定环体，判定一次。物理4，内外半径1.5和5，总高3，无击退", 2, ShuAttribute.FIRE, 0f
    );
    public static final Technique FIRE_SPARK = new Technique(
            "fire_spark", "火苗", TechniqueType.MAGIC,
            1f, 5, 20, "直线火苗。火伤1，本体产种基数2。命中或撞块后消失", 0, ShuAttribute.FIRE, 0.8f
    );
    public static final Technique RULE_SURFACE_ANCHOR = new Technique(
            "rule_surface_anchor", "地表定位", TechniqueType.UNIVERSAL,
            0f, 0, 20, "视线内第一个向上承托面。失败则整组不释放", 1
    );
    public static final Technique FIRE_UPWELL = new Technique(
            "fire_upwell", "喷涌", TechniqueType.UNIVERSAL,
            0f, 0, 20, "把后面的可变弹道改成喷涌", 2, ShuAttribute.FIRE, 1.0f
    );
    public static final Technique RULE_STAGGER = new Technique(
            "rule_stagger", "错时", TechniqueType.UNIVERSAL,
            0f, 0, 20, "每一份在分支基准后再晚10刻", 2
    );
    public static final Technique RULE_HIT_CONTINUE = new Technique(
            "rule_hit_continue", "命中接续", TechniqueType.UNIVERSAL,
            0f, 0, 20, "父段成立后再放出后面的子段。只有一层", 3
    );
    public static final Technique FIRE_FLAME_TRAIL = new Technique(
            "fire_flame_trail", "留焰", TechniqueType.UNIVERSAL,
            0f, 5, 20, "沿载体留下不产种的火焰轨迹", 2, ShuAttribute.FIRE, 0f, true
    );
    public static final Technique FIRE_SEED_SOW = new Technique(
            "fire_seed_sow", "种火", TechniqueType.UNIVERSAL,
            0f, 5, 20, "命中被接受后并入本体基数，基础产种10", 1, ShuAttribute.FIRE, 0f, true
    );
    public static final Technique FIRE_KINDLE = new Technique(
            "fire_kindle", "引燃", TechniqueType.UNIVERSAL,
            0f, 5, 20, "按当时余额挂引燃。不附加直接伤害", 2, ShuAttribute.FIRE, 0f, true
    );
    public static final Technique FIRE_DETONATE = new Technique(
            "fire_detonate", "引爆", TechniqueType.UNIVERSAL,
            0f, 5, 20, "按当时余额引爆一次。不附加直接伤害", 2, ShuAttribute.FIRE, 0f, true
    );
    public static final Technique FIRE_NETHER_GLOOM = new Technique(
            "fire_nether_gloom", "幽冥", TechniqueType.UNIVERSAL,
            0f, 5, 20, "单独释放时在脚下打开领域并按周期产种，也可以附在已有领域上。不减速，不附加直接伤害", 3, ShuAttribute.FIRE, 0f, true
    );
    public static final Technique EARTH_BREAK_GROUND = new Technique(
            "earth_break_ground", "崩地", TechniqueType.UNIVERSAL,
            0f, 5, 20, "在成立的落点挖浅碗。不附加直接伤害", 3, ShuAttribute.EARTH, 0f, true
    );

    /**
     * 注册所有术到管理器
     */
    public static void registerAll() {
        WaterCurveFunctions.register();
        TechniqueManager.register(SWORD_THRUST);
        TechniqueManager.register(SWORD_HORIZONTAL_SLASH);
        TechniqueManager.register(SWORD_VERTICAL_SLASH);
        TechniqueManager.register(MOVEMENT_DASH);
        TechniqueManager.register(MOVEMENT_LEAP);
        TechniqueManager.register(MAGIC_ARCANE_MISSILE);
        TechniqueManager.register(MAGIC_IGNITE);
        TechniqueManager.register(MAGIC_CREATE_WATER);
        TechniqueManager.register(TEST_SUSTAINED);
        TechniqueManager.register(HEAT_BEAM);
        TechniqueManager.register(UNIVERSAL_LOCK_TARGET);
        TechniqueManager.register(RULE_DUAL_RELEASE);
        TechniqueManager.register(TRAJECTORY_DROP);
        TechniqueManager.register(SPACE_CUT);
        TechniqueManager.register(RULE_CAST_ANCHOR);
        TechniqueManager.register(RULE_TRIPLE_CAST);
        TechniqueManager.register(RULE_DOMAIN_BIND);
        TechniqueManager.register(RULE_GATHER_ENERGY);
        TechniqueManager.register(TRAJECTORY_3D_INTERLEAVE);
        TechniqueManager.register(VINE_MARK);
        TechniqueManager.register(PARASITE_SEED);
        TechniqueManager.register(TIDE_MARK);
        TechniqueManager.register(TIDE_SEVER);
        TechniqueManager.register(WATER_PHANTOM);
        TechniqueManager.register(SINKING_EDGE);
        TechniqueManager.register(STONE_HAMMER);
        TechniqueManager.register(KEEN_MARK);
        TechniqueManager.register(METAL_NEEDLE);
        TechniqueManager.register(FIRE_ORB);
        TechniqueManager.register(FIRE_BURST);
        TechniqueManager.register(FIRE_HEAT_UP);
        TechniqueManager.register(FIRE_BURN);
        TechniqueManager.register(FIRE_COOL);
        TechniqueManager.register(FIRE_LIAOYUAN);
        TechniqueManager.register(FIRE_SCORCH_FIELD);
        TechniqueManager.register(FIRE_LIAOLI);
        TechniqueManager.register(FIRE_FENTIAN);
        TechniqueManager.register(TRAJECTORY_TRACKING);
        TechniqueManager.register(TRAJECTORY_STORM);
        TechniqueManager.register(WATER_YIELDING_RIBBON);
        TechniqueManager.register(WATER_WEEPING_ARC);
        TechniqueManager.register(FIRE_METEORITE);
        TechniqueManager.register(FIRE_SHOCK_RING);
        TechniqueManager.register(FIRE_SPARK);
        TechniqueManager.register(RULE_SURFACE_ANCHOR);
        TechniqueManager.register(FIRE_UPWELL);
        TechniqueManager.register(RULE_STAGGER);
        TechniqueManager.register(RULE_HIT_CONTINUE);
        TechniqueManager.register(FIRE_FLAME_TRAIL);
        TechniqueManager.register(FIRE_SEED_SOW);
        TechniqueManager.register(FIRE_KINDLE);
        TechniqueManager.register(FIRE_DETONATE);
        TechniqueManager.register(FIRE_NETHER_GLOOM);
        TechniqueManager.register(EARTH_BREAK_GROUND);
        TechniqueManager.register(PHANTOM_SWORD);
        TechniqueManager.register(SWORD_COME);
        TechniqueManager.register(SWORD_RETURN_ALL);

        TechniqueEffectRegistry.register("sword_thrust", new SwordThrustEffect());
        TechniqueEffectRegistry.register("sword_horizontal_slash", new SwordHorizontalSlashEffect());
        TechniqueEffectRegistry.register("sword_vertical_slash", new SwordVerticalSlashEffect());
        TechniqueEffectRegistry.register("movement_dash", new MovementDashEffect());
        TechniqueEffectRegistry.register("movement_leap", new MovementLeapEffect());
        TechniqueEffectRegistry.register("magic_arcane_missile", new MagicArcaneMissileEffect());
        TechniqueEffectRegistry.register("magic_ignite", new MagicIgniteEffect());
        TechniqueEffectRegistry.register("magic_create_water", new MagicCreateWaterEffect());
        TechniqueEffectRegistry.register("test_sustained", new TestSustainedEffect());
        TechniqueEffectRegistry.register("heat_beam", new HeatBeamEffect());
        TechniqueEffectRegistry.register("universal_lock_target", new UniversalLockTargetEffect());
        TechniqueEffectRegistry.register("rule_dual_release", (player, technique, weapon, cast) -> {});
        TechniqueEffectRegistry.register("trajectory_drop", (player, technique, weapon, cast) -> {});
        TechniqueEffectRegistry.register("space_cut", new SpaceCutEffect());
        TechniqueEffectRegistry.register("rule_cast_anchor", new CastAnchorEffect());
        TechniqueEffectRegistry.register("rule_triple_cast", new TripleCastEffect());
        TechniqueEffectRegistry.register("rule_domain_bind", (player, technique, weapon, cast) -> {});
        TechniqueEffectRegistry.register("rule_gather_energy", (player, technique, weapon, cast) -> {});
        TechniqueEffectRegistry.register("trajectory_3d_interleave", new Scatter3dEffect());
        TechniqueEffectRegistry.register("parasite_seed", new ParasiteSeedEffect());
        TechniqueEffectRegistry.register("tide_sever", new TideSeverEffect());
        TechniqueEffectRegistry.register("stone_hammer", new StoneHammerEffect());
        TechniqueEffectRegistry.register("metal_needle", new MetalNeedleEffect());
        TechniqueEffectRegistry.register("fire_orb", new FireOrbEffect());
        TechniqueEffectRegistry.register("fire_nether_gloom", new GloomDomainEffect());
        TechniqueEffectRegistry.register("fire_burst", new FireBurstEffect());
        TechniqueEffectRegistry.register("fire_heat_up", (player, technique, weapon, cast) -> {});
        TechniqueEffectRegistry.register("fire_burn", (player, technique, weapon, cast) -> {});
        TechniqueEffectRegistry.register("fire_cool", (player, technique, weapon, cast) -> {});
        TechniqueEffectRegistry.register("fire_liaoyuan", new LiaoyuanEffect());
        TechniqueEffectRegistry.register("fire_scorch_field", new ScorchEffect());
        TechniqueEffectRegistry.register("fire_liaoli", new LiaoliEffect());
        TechniqueEffectRegistry.register("fire_fentian", new FentianEffect());
        TechniqueEffectRegistry.register("trajectory_tracking", (player, technique, weapon, cast) -> {});
        TechniqueEffectRegistry.register("trajectory_storm", (player, technique, weapon, cast) -> {});
        TechniqueEffectRegistry.register("water_yielding_ribbon", new WaterYieldingEffect());
        TechniqueEffectRegistry.register("water_weeping_arc", new WeepingCurtainEffect());
        TechniqueEffectRegistry.register("phantom_sword", new PhantomSwordEffect());
        TechniqueEffectRegistry.register("sword_come", new SwordComeEffect());
        TechniqueEffectRegistry.register("sword_return_all", new SwordReturnAllEffect());

        EnchantmentDefinitions.registerAll();
        CloneTechniqueRegistry.registerAll();
        ReleaseDefinitions.registerAll();
        com.bslx.XueXiMod.shu.technique.release.cooperative.CooperativeContent.register();
        WaterTechniques.register();
    }

    /**
     * 外部内容登记完、封存目录前调用一次。这里声明已有专项，并调用分身专项声明一次，不封存。
     */
    public static void declareSpecialBalances() {
        ReleaseDefinitions.declareSpecialBalances();
        WaterTechniques.declareSpecialBalances();
        CloneBalance.declareSpecials();
        WaterPhantomBalance.declareSpecials();
        com.bslx.XueXiMod.shu.technique.release.cooperative.CooperativeContent.declareSpecials();
    }
}

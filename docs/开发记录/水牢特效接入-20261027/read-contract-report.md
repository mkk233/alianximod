我先完整读取这份任务说明，再按其中的范围和要求执行。任务是只读定位：查清水牢客户端现有目标、尺寸、同步和寿命，并给出最小改动方案。我先读指定的几个类和必要上下游。可以零改协议，只换客户端绘制。命中壳仍是提交时冻结的外壳；新画面按各客户端相机，用目标当下碰撞箱等比包成一张正方形公告板。本阶段没有改文件，也没有编译。

## 结论

服务端快照、同步包和客户端锁人逻辑已经够用。`PrisonSyncPacket` 不需要新字段。朝向、帧和平面尺寸都在各客户端自己算。

必须改的是现在真正画盒子的 `PrisonShellRenderer`，以及把已确认竖条贴图放进游戏资源。`PrisonView`、`PrisonShell`、`PrisonRuntime`、`PrisonAim`、`PrisonSyncPacket`、`PrisonClientBridge`、`PrisonClientEntry`、`PrisonClientState` 都可以不动。若把实体查找写成小函数，也只放在客户端渲染里，不要改快照里的 `bounds`。

## 现有链路

目标在提交时定死，之后不再改盒。

`WaterPrisonEffect` 用施术者眼睛向前 **20** 格选一个生物。射线先被方块截断，再取这段里最近的生物或末影龙部位；龙部位会换成龙本体。湿度数值不大于 0，或没有潮湿效果，就失败。前面挡住的干目标不会让位给后面的湿目标。

```18:19:src/main/java/com/bslx/XueXiMod/shu/technique/release/conditional/WaterPrisonEffect.java
    public static final WaterPrisonEffect INSTANCE = new WaterPrisonEffect();
    private static final double TARGET_RANGE = 20.0D;
```

```62:64:src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonAim.java
        if (WetnessState.read(chosen) <= 0 || chosen.getEffect(ModMobEffects.WETNESS.get()) == null) {
            return null;
        }
```

提交时记下脚底 `anchor`、身体碰撞箱外扩后的壳、湿度档、出生刻和结束刻。身份是目标 UUID。网络编号只在发包时现查，不进 `PrisonView`。修订号固定为 `1`，创建后没有第二张更新包。

```91:111:src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonRuntime.java
        Vec3 anchor = target.position();
        AABB bodyBox = target.getBoundingBox();
        double shellScale = frozenInput.sizeMultiplier();
        AABB bounds = PrisonShell.expand(bodyBox, shellScale);
        // ...
        PrisonView view = new PrisonView(
                UUID.randomUUID(),
                REVISION,
                reservation.caster().getUUID(),
                target.getUUID(),
                level.dimension(),
                anchor,
                bounds,
                tier,
                birthTick,
                endTick);
```

命中壳是身体盒先各方向外扩 **0.20**，再按聚能倍率绕身体中心放大。接触只算壳的外层，盒子内部不算。这层壳和内盒在创建时冻结，箭的反弹也直接用这只 `bounds`。

```9:26:src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonShell.java
public final class PrisonShell {
    public static final double PADDING = 0.20D;
    // ...
    public static AABB expand(AABB body, double sizeMultiplier) {
        // ...
        AABB outer = body.inflate(PADDING);
        return scaleAbout(body, outer, sizeMultiplier);
    }
```

寿命先按湿度档，再乘同一次冻结倍率：

| 湿度 | 档 | 基准寿命 |
|---|---|---|
| 1–25 | 1 | 20 tick |
| 26–50 | 2 | 100 tick |
| 51–75 | 3 | 剩余潮湿时间的一半，至少 1 tick |
| 76–100 | 4 | 剩余潮湿时间 |

4 档还会每 20 tick 打 4 点环境伤害。湿度效果被清掉、到期、死亡、登出、换维或真正离开锚点的传送，都会结束这只牢。

聚能不是水牢自己的字段。法术技能里，水牢前面的「聚能」`rule_gather_energy` 提供 **2** 倍，多条相乘，在计划建立时冻进 `ReleaseInput`。水牢的 `kind()` 是命中体积，属性是水，所以吃得到这个倍率。倍率同时拉长有限寿命和外壳。它不改湿度数值，也不改目标碰撞箱。聚能排在水牢后面就不会作用到这次水牢。技能不是法术时，输入保持 1 倍。

包只有两种，服务器到客户端：

- `SNAPSHOT = 1`：创建时发给正在追踪该目标的玩家（目标自己是玩家时也发给自己）。有人之后开始追踪这个实体，再补一张。
- `REMOVE = 2`：结束时发给该维度全体玩家。

包里已有牢编号、修订、施术者、目标 UUID、目标网络编号、维度、锚点、壳的六个数、湿度档、出生刻、结束刻。客户端按修订丢掉旧包；到期、换维、断线会自己清掉，不等删除包。本地玩家被钉在锚点上。服务端位置包的落点离开壳超过 **0.05** 格，才放开这次预测。这些判断用的都是同步来的壳，不是画面尺寸。

现在的画面把这只壳画成六面静止水。相机贴得近于 **0.4** 格的面会被挖掉，而且每一面正反各画一次。

```110:113:src/main/java/com/bslx/XueXiMod/client/PrisonShellRenderer.java
        face(consumer, matrix, light, alpha, startX, startY, startZ, alongX, alongY, alongZ, farX, farY, farZ, closeX, closeY, closeZ,
                normalX, normalY, normalZ);
        face(consumer, matrix, light, alpha, closeX, closeY, closeZ, farX, farY, farZ, alongX, alongY, alongZ, startX, startY, startZ,
                -normalX, -normalY, -normalZ);
```

## 要守住的不变量

- 目标仍由准星、20 格、潮湿和单次提交决定。占用中的目标不能再被关一次。
- 锚点、禁锢、跳跃、击退和传送破裂仍用服务端逻辑。客户端 `holdLocal` 仍把本地玩家钉在包里的锚点。
- `bounds` 继续表示冻结外壳。传送豁免、接触和箭反弹继续用它。不要把它改成正方形，也不要按相机改写。
- 修订号、两种包、墓碑、换维清理和登出清理保持原样。
- 聚能仍只乘寿命和外壳。湿度档、4 档伤害和结束结算不改。
- 关不进去时，现有释放路径会退回这一段魔力，并给施术者发红色原因。

## 新旧尺寸不要混用

| | 旧命中壳 | 新公告板 |
|---|---|---|
| 来源 | 提交当时的 `getBoundingBox()` | 每个客户端、每一帧的 `getBoundingBox()` |
| 形状 | 轴对齐长方体，玩家通常高而窄 | 一张正方形，两轴同一个边长 |
| 外扩 | 先加 0.20，再乘聚能 | 不外扩，也不乘聚能 |
| 是否跟随姿势 | 创建后冻结 | 蹲下、幼年等会跟着碰撞箱变 |
| 朝向 | 世界轴 | 当前相机，含俯仰；各客户端互不同步 |

聚能之后，别人碰到的范围变大、关押变长，泡泡仍只包住身体碰撞箱。用壳的宽高分别拉平面，会把原图拉成椭圆，这正是 v4 预览要避开的。

实体还没出现在客户端时不要退回画这只壳。网络编号要和包里的目标 UUID 一致，避免编号被复用后套到别人身上。

## 最小改动

1. 把 `docs/资源库/过程归档/水牢泡泡_2D_v3_斜向错层-20261027/water_prison_bubble_v3_strip.png` 复制进 `assets/xueximod/textures/`。它是 96×1536，16 帧竖条。用渲染时间取帧：100 毫秒一帧，16 帧循环。世界时间每秒 20 tick，所以一帧等于 2 tick。UV 只切当前那一格，不要把整条竖图贴到一个面上。
2. 重写 `PrisonShellRenderer`。继续在实体之后画，这样透明水纹可以盖住目标正面，地形深度仍能挡住墙后的部分。去掉原版 `water_still` 和六面体。
3. 混合方式对齐水幕已有的透明渲染：半透明、深度测试开、只写颜色不写深度。水幕关了背面剔除；这里要开剔除，并且只提交朝向相机的一个四边形。预览里的单次通过，就是为了避免现在这种正反两次叠色。
4. 尺寸和位置按 `preview-billboard.js` 的结果做，不调用 Three.js。把碰撞箱 8 个角变到当前相机空间，取出屏幕上的宽和高。统一边长沿用预览第 21 行：`hypot(屏幕宽 × 96/70, 屏幕高 × 96/74) × 1.04`。70 和 74 是 96 画布里泡泡不透明区域的宽高，不是 0.20 的命中外扩。平面中心放在这个投影中心，深度放在离相机最近的角再靠前 0.08，同时不要近于近裁剪面加 0.05。法线与相机一致。
5. 全亮、不附加现有的 170,220,255 染色，避免改原图像素。

`PrisonClientState.visible()` 已经给出目标网络编号、UUID、维度和结束刻，渲染器自己查实体即可。

第一人称被关住时，相机在碰撞箱内部，预览公式会把平面钳到近裁剪面，可能铺满画面。现有代码靠跳过 0.4 格内的面来避开。这一支只在绘制里处理，例如相机在碰撞箱内时不铺这张近平面。不要因此改 `holdLocal`。

## 实测前的装配和潮湿

水牢 id 是 `water_prison`，法术，复杂度 0，伤害 0，冷却 400 tick，显式魔力 40。单独装上时倍率是 1。要看聚能，把它装进法术技能，并且排在水牢前面。

潮湿必须是数值和效果同时在。缺月每次命中加 6。一次是 1 档，牢只持续 20 tick，短于 1.6 秒动画，只能看到开头。打到 26 以上进入 2 档，基准 100 tick，够看完一轮。雾雨是进入领域后每 10 tick 加 2，第一次要等 10 tick。湿度效果本身每次成功施加刷新 1000 tick。

看画面时用另一个生物或另一名玩家当目标。施术者站在 20 格内、中间没有方块，准星放在已经潮湿的那个身上。干生物挡在前面会失败，聊天出现红色原因；这次不扣成功结算。成功后目标停在锚点。绕到墙后，泡泡应被挡住。两名观察者分别俯仰，各自看到朝向自己的正方形，边长相同，不会被拉成长椭圆。

核对命中时另用一个生物去蹭。碰到的是冻结外壳，不是泡泡边。加上聚能再关一次：关押更久、外壳更大，泡泡仍贴着身体碰撞箱。1 档加一条聚能后寿命变为 40 tick，画面尺寸仍不乘 2。

游戏内效果要等后续指定的人编译、再由你进游戏看。这次没有编译，也没有启动游戏。
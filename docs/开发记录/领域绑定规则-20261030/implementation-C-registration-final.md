# C 最终报告：领域绑定身份注册与几何接口

作者 C。模型 grok-4.7-high-fast。本阶段是实现，实现复核仍是 0/3。本单只完成规则注册和真实范围几何。领域绑定功能尚未完成。`DomainInstance`、`DomainService` 和领域运行调度都没有改。

## 注册事实

标识 `rule_domain_bind`，名称「领域绑定」，描述：

> 放在领域术式后面，绑定后续攻击链，每20tick自动释放一次

`Technique` 使用七参数构造，因此是通用、无属性、非附魔。

| 项 | 值 |
|---|---|
| 类型 | `TechniqueType.UNIVERSAL` |
| 属性 | `ShuAttribute.NONE` |
| 基础伤害 | `0f` |
| 基础冷却 | `0` |
| 等级上限 | `20` |
| 复杂度 | `0` |
| 弹速默认 | `1.0f` |
| 种类 | `ReleaseKind.RULE` |
| 角色 | `ChainRole.RULE_DOMAIN_BIND` |
| 标签 | `ReleaseTag.TRIGGER` |
| 弹道 | `Trajectory.STRAIGHT` |
| 移动 | `false` |
| 寿命 | `1` |
| 速度 / 厚度 | `0f` / `0f` |
| `pullAfterA` | `0` |
| 弹道可变 | `false` |
| 操纵剑实体 | `false` |
| 拒绝混合链 | `false` |
| `energyScale` | `1f` |
| `chainEnergyScale` | `1f` |
| 显式魔力 / 显式装配 | `-1` / `-1` |
| 对外规则魔力 | `0` |
| 对外装配消耗 | `0` |
| 效果 | 空 lambda |

登记走 `rule_triple_cast` 同一条短 `define`。短定义把 `chainEnergyScale` 固定为 `1f`，显式魔力和显式装配消耗固定为 `-1`。聚能那条长重载才把角色写成 `ChainRole.ATTACK`、把 `chainEnergyScale` 写成 `2f`；本定义没有调用那条重载。

对外魔力：`Technique.manaCost()` 在种类为 `ReleaseKind.RULE` 时返回 `0`。基础伤害也是 `0`。对外装配消耗：显式装配消耗小于 `0` 时继续看类型和伤害；本术式不是附魔、不是动作、基础伤害为 `0`，所以 `assemblyCost()` 返回 `0`。

图标沿用已有 `textures/gui/technique/other_none.png`。装配界面用类型加属性拼路径：非剑术得到 `other`，`ShuAttribute.NONE` 的 id 是 `none`。没有新贴图、语言条目或物品模型。

20 tick 只写在描述里。基础冷却是 `0`，这条规则不会给链冷却合计加上 20。首发时刻、周期调度和支付都不在本单。

效果已注册为空 lambda，和双重释放、聚能的空效果同一写法。定义本身是 `ReleaseKind.RULE` 加 `ChainRole.RULE_DOMAIN_BIND`，基础伤害为 `0`，链能量倍率为 `1`。

位置：

- `ChainRole.java` 第 16 行：`RULE_DOMAIN_BIND`，加在 `RULE_DUAL` 之后，原有常量序号不变。
- `ModTechniques.java` 第 115–118 行常量，第 241 行 `TechniqueManager.register`，第 284 行空效果。
- `ReleaseDefinitions.java` 第 28 行 `ReleaseRegistry.register`。

全仓只有一份 `ChainRole`、`ModTechniques`、`ReleaseDefinitions`、`DomainGeometry`、`AxisBoxGeometry`。`DomainGeometry` 的实现者只有 `AxisBoxGeometry`。

## 几何接口和边缘行为

`DomainGeometry` 新增：

- `net.minecraft.world.phys.Vec3 samplePoint(net.minecraft.util.RandomSource random)`
- `net.minecraft.world.phys.Vec3 clampPoint(net.minecraft.world.phys.Vec3 point)`

`AxisBoxGeometry` 只读自己保存的 `bounds`。内缩量是该类已有的 `EDGE = 1.0E-4`。`fits`、`fullyContains`、`intersects`、`fullInterval`、`clipInside`、`bounds`、`atFeet`、`scaledFromGround` 的判定都没有改。

采样按 X、Y、Z 各抽一次 `nextDouble()`。随机源非空时固定消耗三次。单轴长度大于 `2 * EDGE` 时，采样区间是 `[min + EDGE, max - EDGE)`。`nextDouble()` 取 `[0, 1)`，所以高端内缩面本身抽不到，低端内缩面可以抽到。两点都在真实盒内。

单轴长度不大于 `2 * EDGE`，或长度不是有限数时，该轴只剩中点。中点也不是有限数时，改用仍有限的那一端；两端都没有时用 `0`。退化轴仍会消耗一次 `nextDouble()`，返回值不用那次抽签。

`random == null` 时抛出 `IllegalArgumentException("domain sample needs a random source")`，不抽签，也不改回盒心。

`clampPoint(null)` 返回 `null`。非空点使用和采样相同的内缩区间，区间两端都包含。已经在区间内的坐标保持原值。越出的坐标贴到该轴内缩面。可用轴上的非有限坐标收到该轴内缩区间中点。退化轴收到唯一允许点。

因此，盒外或盒面上的点会被收进内缩范围。高于盒顶的点，在该轴够宽时 Y 变成 `maxY - EDGE`，X、Z 仍按各自区间处理。这个方法只返回位置，不改方向。交错偏移后的点按轴收回，不做射线回投。

## 本单未改的接点

这些文件仍归原作者。现有 `switch` 都有 `default`，只增加枚举常量不会让它们失去可编译性。

- `SkillSegmentMath.isChainModifier`、`TechniqueChain.waitsForHost`、`ChainSteps.waitsForHost` 仍由 A 拥有。`RULE_DOMAIN_BIND` 还没有写进这三处。链能量倍率是 `1`，倍率分支也不会把它收成聚能修饰。绑定捕获按定案在切步前完成，那部分是 A 的结构单。
- `ImprintTreeView.ruleKind` 的 `default` 返回 `-1`。定案的最小方案不改 UI。装配名称和图标来自 `Technique` 注册，不依赖这个分组。
- 没有改 A 的 network、`TechniqueChain`，没有改 B 的帧和实体，没有读或写候选查询作者占用的命中、运动、体积、追踪、锁定、批次、接触附魔、`query/` 和对应 mixin。
- 没有改 `DomainInstance`、`DomainService`、制作表、资源贴图。

## 文件、备份、哈希

| 文件 | 改动 | 改前备份 |
|---|---|---|
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/ChainRole.java` | 追加角色 | `backup-C-registration/` 下同相对路径 |
| `src/main/java/com/bslx/XueXiMod/shu/ModTechniques.java` | 常量、注册、空效果 | 无字节副本；逆补丁见证据 JSON |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseDefinitions.java` | 一行 `define` | 无字节副本；逆补丁见证据 JSON |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainGeometry.java` | 两个接口方法 | `backup-C-registration/` 下同相对路径 |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/AxisBoxGeometry.java` | 采样和内缩实现 | `backup-C-registration/` 下同相对路径 |

最终 SHA-256 和 mtime：未取得。本会话的 Shell 调用，包括 `echo`、`dir`、`python`、`Get-FileHash` 和 `tools/check-name-gate.ps1`，都立即返回 `Rejected`，没有输出。三份小文件备份是改前正文的 UTF-8 写入，没有和原字节核对换行。两份大文件在复制被拒绝后只做了插入，其余原文仍留在工作区文件里。既有未提交改动没有用 reset、clean 或 checkout 覆盖。

证据：`docs/开发记录/领域绑定规则-20261030/evidence-C-registration.json`。

## 未验证

- 待统一编译。本单没有跑 `compileJava`，没有跑 Gradle，没有打包，没有启动游戏。
- 命名门脚本没有跑成。新增局部名都不少于 3 个字符，按驼峰拆开后也没有命中禁止缩写；这是阅读结论，不是脚本结果。
- 没有新增测试，没有做采样分布的数值检查。
- 改前和改后哈希、mtime 没有文件系统读数。

## 写权限

本单写入已经结束，交还这些文件的写权限。后续修改 `ChainRole`、`ModTechniques`、`ReleaseDefinitions`、`DomainGeometry`、`AxisBoxGeometry` 仍只由 C 进行。主会话可以继续派领域运行单。本报告不表示领域绑定已经可以释放、扣费或选敌。

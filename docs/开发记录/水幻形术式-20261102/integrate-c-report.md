# 水幻形 C 组正式接入报告

日期：2026-11-02。只登记 `water_phantom` 和它的玩家说明。没有编译，没有进游戏。本报告写完后停止写源码。表现资源仍由 D 接入，这次登记不算整术式已经交付。

## 改动

| 路径 | 行号 | 作用 |
|---|---|---|
| `shu/ModTechniques.java` | 149–154 | `PLAYER` 术式：法术、水系、伤害 0、冷却 20、等级上限 1、复杂度 1 |
| `shu/ModTechniques.java` | 317 | `TechniqueManager.register(WATER_PHANTOM)` |
| `shu/ModTechniques.java` | 406 | 封存前调用 `WaterPhantomBalance.declareSpecials()` |
| `shu/technique/release/ReleaseDefinitions.java` | 39 | `ReleaseRegistry.register(new WaterPhantomDefinition())` |
| `shu/technique/release/clone/water/WaterPhantomDefinition.java` | 16–17 | 注释改为已经由释放表登记 |
| `shu/technique/release/clone/water/WaterPhantomBalance.java` | 13–16 | 注释写明专项声明的调用方 |
| `assets/xueximod/lang/zh_cn.json` | 127–128 | 中文名和同一句简述 |
| `assets/xueximod/lang/en_us.json` | 70–71 | 英文名和对应简述 |
| `docs/开发记录/水幻形术式-20261102/player-usage.md` | 全文 | 现有入口下的取得、装配和释放 |

没有把水幻形放进 `DAMAGE_HOSTS`。没有改 `clone_phantom`、`clone_body`、`clone_pulse` 的 `DEVELOPMENT_ONLY`。没有新的按编号施法分支，没有改装配门、分发器、客户端、纹理或模型。

命名门禁：`powershell -NoProfile -File tools/check-name-gate.ps1`，退出码 0。

## 启动顺序

`XueXiMod.commonSetup` 120 行调用 `ModTechniques.registerAll()`。其中 317 行先登记术式，394 行 `CloneTechniqueRegistry.registerAll()` 顺带登记断潮适配器，395 行才 `ReleaseDefinitions.registerAll()`，39 行挂上水幻形定义。定义的 `explicitManaCost()` 返回 10。

122 行 `noteBuiltins` 把已登记术式记成内置。124 行 `sealAfterContent` 先按术式补标准键。此时释放定义已经在册，`Technique.manaCost()` 读到显式 10，不会按 0 点伤害算出 0。基础伤害是 0，也不在伤害宿主名单里，所以不补伤害倍率。91 行调用 `declareSpecialBalances`，406 行声明 `lifetimeTicks` 默认 0（0..1200000 刻）和 `decoyRadius` 默认 8（0..16 格）。94 行才 `BalanceRegistry.seal`。标准 `manaCost`、`cooldownTicks` 不在专项里再声明一次。

报价在施放时由现有 `ActorCaptureDispatcher` 111–115 行各读一次：宿主魔力、宿主冷却、`lifetimeTicks`、`decoyRadius`。缺配置用上述默认。这次没有写配置文件，已有世界配置保持原样。

## 玩家能看见的字

图鉴和装配画的是 `Technique.getName()` / `getDescription()`：`ShuManagementScreen` 126、133 行，`SpellAssemblyScreen` 334 行。中文名是「水幻形」。简述写明法杖槽 0、水幻形再到断潮、普通留置、潜行立刻散开、默认魔力 10+12 一次、冷却 20+20，以及未适配载荷在收费前拒绝。语言文件写了同一句，当前没有 Java 读取 `technique.xueximod.water_phantom`。

取得和操作见 `player-usage.md`。复杂度 1 不会在登录时自动学会。

## 未跑和边界

未运行 Gradle，未 `compileJava`，未打包，未进游戏。没有改制作表。水形、散水和运行贴图不在这次写入里。统一编译留给随后指定的编译。C 组到此停止写入。

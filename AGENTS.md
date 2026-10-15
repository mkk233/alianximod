# XueXi Mod（术）— Agent 须知

下面两条压过后面的功能和 MCP。先遵守，再干活。

## 1. 并行调度优先

通读、审查、并行修改，按 `.grok/skills/max-dispatch/SKILL.md` 做。主代理只调度，不自己通读整仓，不大面积自己改。

- 通读 `deepseek-flash`，开满并发，一块一个，两个人不写同一文件。
- 齐了只开一个 `gpt-6-luna` 裁决。没拍板不改代码。
- 改动用 `deepseek-v4-pro`。一项结束立刻派 `grok-4.7` 审，不要等全部改完。
- 每次 spawn 必须写 `model`。禁止 `grok-4.5`，禁止省略 model。

## 2. 动态判断难度，偏难先问

动手前先判这一步的难度。只要比「照着改、读一个文件、跑一条命令」高一点，或者明显难，就先问 `gpt-6-luna`（`brain` / `gpt6`），不要先自己试。

自己做：单文件小改、查一个符号、跑 `gradlew`、已经有明确步骤的执行。

先问脑子：根因不明、改动牵涉多处、接口或架构怎么定、正确性/并发/NBT、需求含糊、改错成本高、你觉得「有点吃力」。问完再调度落地。

## 3. 其他能力

Forge **1.20.1**（ForgeGradle `47.3.33`），模组 ID `xueximod`，包名 `com.bslx.XueXiMod`。设计见根目录 `README.md`。

在项目根目录用 `gradlew`。`org.gradle.jvmargs=-Xmx3G`，daemon 关着。没跑过的编译或游戏内验证，要在汇报里写明。

## 4. 项目 MCP

操控正在跑的客户端，只走 **`minecraft-mod-mcp`**（stdio：`npx -y minecraft-mod-mcp`）。桥自己扫 `9876`→`9000`。不要把本文件当成那个仓库的贡献规范。

- 不要把 `http://127.0.0.1:9876` 配成 SSE / URL MCP。游戏里的 HTTP 不是 MCP。
- 不要跑 `just daemon`。
- 动手前先 `ping` 或 `get_minecraft_status`。没连上就停。
- 不要对这个开发实例调用 `launch_minecraft` 去拉另一个版本。测的是当前这个 1.20.1 客户端。
- 看画面用 `screenshot_to_file` 存到文件再读图。不要盲点。
- 切出游戏前先用模组的「释放鼠标」，否则会进暂停菜单。
- 多个客户端同时开时，桥只锁扫描到的第一个。测这个模组时不要再开第二份。

## 5. 当前进度（2026-10-16）

细节在 `docs/交接-2026-10-16-当前进度.md`。`docs/交接索引.md` 说明哪份交接还有效。`docs/交接-2026-10-14-*` 里关于操纵剑标记和插地夹角的句子已过时。

设计依据是 `docs/逻辑修正.txt`（根目录若还有同名文件，以代码和本段为准）。底座是逻辑类型。新术式在 `ReleaseDefinitions` 登记 `ChainRole`，走同一套收集和结算。不要按术式 id 单独接落点、交错、一分为三或附魔。

`ChainRole`：`SEEK_SPACE` 落点指定、`SEEK_ENTITY` 锁定实体、`TRAJECTORY_SCATTER` 三维交错、`TRAJECTORY_DROP` 垂落、`RULE_TRIPLE` 一分为三、`RULE_DUAL` 双重释放、`ACTION` 冲刺、`ATTACK` 攻击。结算顺序是索敌 → 弹道 → 规则 → 攻击。`trajectoryMutable == false` 的斩击只改生成位置。飞弹、热光束、金针、寄生种子、幻剑为 true。

索敌只决定往哪打，不改弹道类型。`entityLock` 时，`SkillUsePacket.runSegment` 在绑定 `CastFrame` 前调用 `LockTargetService.resolve`（准星 20 格、离视线 8 格内最近）。找到则标记并发光，找不到则清标记。直线、垂落、跟随准星只改方向或落点。幻剑 `acceptsTargetOverride` 为 true：悬停时朝锁定目标中心，起飞后冻结方向直线飞；没有目标则跟准星。生成位置不因索敌改变。

攻击体积走 `TechniqueHitSpec.spawn`。命中在 `TechniqueHitEntity.tryHit`。一分为三共用 `ReleaseBatch.Tally`，约 6 tick 后一次 `playerAttack`，伤害乘命中份数。不改无敌帧。没有攻击时，附魔记在主手，下一次普通攻击消耗。

已注册术式含上面的属性术式，外加幻剑术、剑来。不要做火雨。旧的「交错排布」「伤害汇总」已从可装配列表去掉。

`manipulatesSwordEntities()` 默认 false。目前只有 `sword_come` 为 true。幻剑仍要手持剑才生成。空手只在该槽有操纵术式、且客户端能看见自己的存活剑实体时才发释放包；否则把按键留给原版，不提示。服务端条件不够就静默返回。

幻剑生成在眼睛后方 2 格的同一竖直平面：左右各 3 格加中间 1 格，自眼睛往上 4 格随机。有 `overrideGeometry` 时仍用链上的 `origin`。剑身与飞行方向平行，插地只把剑尖插入方块。不要恢复 45° 夹角限制。

剑来：低于眼睛先抬起，再按每把剑自己的水平方向飞回眼睛外侧 1 格。回收途中可再命中一次，不打主人。

`compileJava --offline --no-daemon` 在索敌这次改动后已通过。锁定实体加幻剑、幻剑门平面、剑来散开回收、24 区块帧数，游戏里都还没复核。法杖、魔力、术式收纳箱、自绘装配界面已接入，改之前先读对应类。

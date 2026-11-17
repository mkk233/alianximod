# 作者 C 整合报告

批次：分身底座-20261102。作者：C。阶段：实现，正式复核 0/3。本单是候选实现和必要定向自检。

候选未接入、待统一编译、游戏未测。

最后一份候选源码写入：`ModTechniques.java`，2026-11-02T04:15:32.166+08:00，sha256 `a69e59d327fe59a6ee53faa07b863babe4da50a112935fdaadbc6ab103931a02`。之后只写本报告、证据和接入清单。作者 C 停止写入并释放候选源码与这三份记录。

## 范围

正式 `D:/alianximod/src/main` 未改。20 个既有文件的原始字节在 `docs/开发记录/分身底座-20261102/before-C/`，sha256 与当时正式文件相同。候选在 `docs/开发记录/分身底座-20261102/候选源码/`，后缀保留 `src/main/java` 与 `src/main/resources`。

没有改 A 的 `clone/core`、B 的 `clone/action` 和 `CloneBalance.java`，没有读这些进行中的源码。引用只按冻结合同。没有改设计、冻结合同、工作簿，没有提交，没有跑 `gradlew`，没有打包，没有启动游戏。命名门禁留到统一接入后；本次不为门禁写正式 `src`。

## 已完成能力

- `TechniqueAvailability` 只有 `PLAYER` 与 `DEVELOPMENT_ONLY`。旧四条 `Technique` 构造转入 `PLAYER`。`availability()` 是注册期终态，没有从 NBT 写入的入口。
- `TechniqueAccess` 提供玩家目录、单术式准入和整链 `skillAllowed`。已登记且为开发项的任一段使整链失败。未登记段不在这里删除。
- `TechniqueManager.getAll()` 仍是全量，封存继续用它。`playerCatalog()` 委托玩家目录。`getByType`、`getSwordCompatible`、`getMagicCompatible` 也走玩家目录。
- 玩家入口：登录时复杂度 0 自动学习、`/shu` 的 list、learn、learn all、reset 全学会、stone、book write 的列表与显式点名、刻印菜单两处目录和刻印动作、术管理分类、法杖与剑技两条装配、装配菜单 `rejectReason`/`mayPlace`、`StaffSkills.castRefusal`。显式开发项回复 `该术式只属于开发入口`，不发空石、不改写技能书、不改学习状态。旧存档加载仍按原字符串写入，不清洗。未知未登记编号仍按原逻辑保存。
- `SkillUsePacket.offerNow` 在技能已解析后、持剑门、冷却忙碌和使用计数之前检查整链。失败返回 false，按住重试会被丢掉。`executeSkill` 在批量编译、纯附魔、施法会话和普通预扣之前再查一次。失败不推进游标、不写学习或升级。
- 三个真实术式：`clone_phantom`、`clone_body`、`clone_response_pulse`。数值和显式耗魔来自 `CloneBalance` 常量。释放定义是普通 `ACTION`，生成角色 `ACTION`，脉冲角色 `ATTACK`。`explicitManaCost` 分别为生成 10 与脉冲 2，不是默认 -1。定义上的 1 刻只满足接口，不是分身寿命。旧效果登记会抛 `DEVELOPMENT_ENTRY_ONLY`，不是空成功。`ClonePulseAction.register()` 只在 `CloneTechniqueRegistry.registerAll()` 调用一次。`ModTechniques.registerAll` 在 `ReleaseDefinitions.registerAll` 之前登记术式；释放定义由 `ReleaseDefinitions.registerAll` 挂接。`declareSpecials()` 只在 `ModTechniques.declareSpecialBalances()` 调用一次。
- `CloneEntityTypes` 使用独立 `DeferredRegister`，路径 `clone_actor`，体积 0.6×1.95，跟踪 10，更新间隔 3，`noSave`。`XueXiMod` 在模组总线注册类型，并只放一行属性：`CloneEntity.createAttributes().build()`。渲染注册在现有客户端事件里，专用服务器类不引用 `CloneRenderer`。
- `/shu clone` 的 `spawn phantom|normal`、`list`、`respond <uuid>`、`dismiss <uuid>|all` 都要求权限 2，发起者是在线玩家本人。回执使用 `detail`、`commandLine()`、已撤回、不在本人索引和结束数量。成功回执不另加伤害结论。列表可点击填入本人的 respond 命令，悬停写明 dismiss 写法。没有替他人指定主人的参数。
- `PerfMetric` 在 `COMPUTE_BACKEND` 后追加 `CLONE_TICK`、`CLONE_RESPONSE`。`PerfCounter` 在 `COMPUTE_JAVA_FALLBACKS` 后追加六项分身计数。没有改旧序号、`PerformanceRecorder` 或 `ticks.csv`。C 不调用这些计数。
- 语言只新增 `entity.xueximod.clone_actor`。中文「分身」，英文 `Clone`。

## 配置消费者

C 不调用 `BalanceService`。C 把三个真实术式送进既有封存，并把 `CloneBalance.PULSE_ID` 放进 `DAMAGE_HOSTS`，让现有 `addDamage` 为脉冲声明 `damageMultiplier`。两个生成编号不进入该名单。

`ModTechniques.declareSpecialBalances()` 调用一次 `CloneBalance.declareSpecials()`。专项键仍由 B 声明：两个生成编号的 `lifetimeTicks`、`maxHealth`、`decoyRadius`；脉冲的 `fixedDamage`、`range`。标准 `manaCost` 与 `cooldownTicks` 走既有封存，读取术式自身的冷却和 `explicitManaCost`。脉冲伤害倍率只声明这一次。

实际读取不在 C：生成报价由 A 的 `readSpawn` 消费，脉冲报价由 B 的 `readPulse` 消费。世界配置下次开世界生效。命令回执里的实扣数以 A 的 `detail` 为准。调整脉冲伤害不改 C 写死的默认耗魔常量。

## 接口依赖

C 调用、但不实现：

- A：`CloneService.spawn/request/dismiss/dismissAll/list`，`CloneResults` 的回执与 `commandLine()`，`CloneEntity.createAttributes()` 与 `actionSequence()`，`CloneKind`，`CloneHandle`，`CloneTriggerInput`。
- B：`CloneBalance` 常量与 `declareSpecials()`，`ClonePulseAction.register()`。

C 提供给 A/B 的冻结入口：`CloneEntityTypes.cloneType()`、`CloneTechniqueRegistry.profile/technique/pulse/actionKey`、三个术式的 `availability()`。

## 签名说明

1. 渲染。合同写法是 `ZombieModel<CloneEntity>`。1.20.1 的 `ZombieModel` 类型上界是 `Zombie`，这个参数写不出来。候选类是 `LivingEntityRenderer<CloneEntity, HumanoidModel<CloneEntity>>`，模型层用 `ModelLayers.ZOMBIE`，贴图用 `textures/entity/zombie/zombie.png`。两种分身同一模型。没有新模型，没有下载皮肤。客户端只在动作序号相对上次看见的值发生变化时挥一次主手，不发包，不调用 `CloneService`。
2. 主动响应。冻结方法要求完整 `CloneHandle`，同节又规定 session、维度和代次由 A 按 UUID 从本人索引补齐，并禁止 C 用代次 0 探活。候选只调用一次 `request`。句柄里的 UUID 来自命令；sessionToken 为 0，维度是主人当前维度，代次为 1。这不是事先探活。若 A 把传入代次或 session 做成精确相等、而不按 UUID 覆盖，这次调用就还不是无损接入，需要 A 提供按 UUID 取本人句柄的正式方法。C 没有另写服务，也没有用反射。

`DomainLearning` 仍遍历 `getAll()`。它不在 C 的文件里。它只处理带领域自动学习的术式；分身定义没有领域。`learnTechnique` 对已登记开发项直接返回，间接学习也不会写入。

## 自检与未做

自检：正式文件与 `before-C` 哈希一致；新文件没有出现在正式 `src`；两份语言 JSON 可解析。未做命名门禁脚本、未编译、未进游戏。

未做：没有把候选接入运行源码；没有统一编译；没有游戏实测；没有改表；没有给开发项做普通玩家装配成功路径；没有旧链按编号特判；没有新美术。A/B 候选在本单写作期间仍有写入，本报告不把它们当成已经收束的编译输入。

待接入清单和逐项 sha256 见 `integration-apply-manifest.json`。应用时先重核原目标哈希；变了只合并本次增量，不能整文件覆盖。

作者 C 停止写入。候选未接入。待统一编译。游戏未测。

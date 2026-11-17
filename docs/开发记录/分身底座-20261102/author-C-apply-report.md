# 作者 C 接入报告

批次 `分身底座-20261102`。这是候选接入，不是正式复审。设计 1/2 已冻结，实现复核仍是 0/3。

**待统一编译。游戏未测。** 本次没有调用编译器，没有打包，没有启动游戏。

C 停止写入，并释放写锁，留给之后的集中只读复核。后续编译需要单独派发。

## 接入结果

A 的 20 个 core 文件、B 的 6 个 action 文件和 `CloneBalance.java` 已按作者最终证据中的完成清单写入正式 `src/main/java`。C 的新增文件和 20 个既有目标上的增量也已写入正式源码与两份语言文件。

`/shu clone respond` 使用 `CloneService.handleFor(ServerPlayer, UUID)`。句柄为空时回 `不在本人索引`，不再调用 `request`。已去掉候选里的 session 0、代次 1 占位。没有放宽 A 的 session 与代次校验。成功回执只转发服务返回的说明，不提前宣称伤害。

## 字节与快照

正式应用前，20 个 C 修改目标的行数与 `before-C/` 一致，抽查锚点仍是接入前文本，正式树里还没有本批分身符号。A/B 与 C 的新文件目标当时不存在。

本次 shell 被环境拒绝，进程没有启动。因此没有重算 sha256，没有写入 `before-apply/` 的原始字节，也没有拿到接入后的 mtime。修改目标的接入前 sha256 仍以 `integration-apply-manifest.json` 的 `originalSha256` 为准。A/B 候选的宣布哈希仍以两份作者 evidence 为准。接入后的正式文件哈希未计算，不能把候选哈希说成已经落到正式文件。

A/B 文件按候选全文落入正式路径，行数与候选一致。C 没有改 A/B 逻辑。`CloneCommands.java` 是唯一相对候选有意改动的正式文件，改动只是改走 `handleFor`。

## 配置消费者

C 只做声明接线：

- `ModTechniques.registerAll` 在 `ReleaseDefinitions.registerAll` 之前调用一次 `CloneTechniqueRegistry.registerAll`。其中登记三个术式、三个会抛 `TechniqueAccess.DEVELOPMENT_ENTRY_ONLY` 的防御效果，并调用一次 `ClonePulseAction.register`。
- `ModTechniques.declareSpecialBalances` 在既有调用之后调用一次 `CloneBalance.declareSpecials`。专项键是两个生成术式的 `lifetimeTicks`、`maxHealth`、`decoyRadius`，以及脉冲的 `fixedDamage`、`range`。
- `BalanceCatalog.DAMAGE_HOSTS` 只追加 `CloneBalance.PULSE_ID`。生成术式不是伤害宿主。
- 标准 `manaCost` 与冷却仍由术式封存补齐。脉冲伤害倍率只走目录里已有的一次 `damageContribution`。
- C 不调用 `BalanceService`。生效值在下次打开世界时进入。命令回执里的实扣由 A 的生成结果给出。调整脉冲伤害不改默认耗魔。

## 衔接现状

留给集中复核，C 没有改 A/B：

- A 的 `CloneEntity` 已有 `generation()`。`ClonePositionPolicy` 组帧时读取它。B 的 `ClonePulseAction.verifyActor` 仍只拒绝帧上 `actorGeneration < 1`，注释写明不和实体代次比对。A 的 `CloneService.request` 用真实句柄对照 `state.actorGeneration`，session 不一致则拒绝。
- 取消令牌为真时，B 使用 `RejectReason.BUSY`，文字是「这次响应已经取消」。A 对来源深度大于 0 的忙碌也使用 `BUSY`，文字是「这次响应还在提交，不能再触发」。这是现状记录，不扩展枚举。
- 没有 `PreparedAction` 射程读取口。射程由 B 在同一次 `prepare` 里用 `readPulse` 的 `rangeBlocks` 判断。
- 渲染器正式类型是 `LivingEntityRenderer<CloneEntity, HumanoidModel<CloneEntity>>`，烘焙 `ModelLayers.ZOMBIE`，贴图是原版 `textures/entity/zombie/zombie.png`。冻结签名里的 `ZombieModel<CloneEntity>` 与 1.20.1 的 `ZombieModel<T extends Zombie>` 冲突，没有改成非法类型参数。
- 客户端渲染注册只在 `XueXiMod.ClientModEvents.onRegisterEntityRenderers` 内，用全限定名 `com.bslx.XueXiMod.client.CloneRenderer::new`。外层类不导入该渲染器。
- 1.20.1 命令参数使用 `net.minecraft.commands.arguments.UuidArgument`。这一点尚未编译验证。

## 命名门禁

`tools/check-name-gate.ps1` 没有跑起来。记录见 `naming-gate.log`。没有禁用脚本，也没有把未跑写成通过。C 的新增名字没有为了门禁再改。A/B 的名字没有改。已有文件里的历史短名保持原样。

## 未做

没有改设计、冻结合同、制作表或 `记录.md`。没有复审旧批次。没有编译，没有游戏实测。

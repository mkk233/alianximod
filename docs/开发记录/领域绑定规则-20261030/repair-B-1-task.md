# B返修：发布回执、静默空剑、垂丝复制横排

固定Cursor CLI --model grok-4.7-high-fast，继续B原可写会话。读review-decision-1.md、repair-commit-contract.md及自己的implementation-B-final.md，完整修B负责的R1/R3发布接点、R4/R5。实现1/3返修，不另审；设计2/2耗尽。

你的独占范围保持原B清单；本单重点TechniqueHitSpec、PhantomSwordEffect、SwordEntity/SwordVortex的真实发布/状态提交位置，并新增明确写权限SwordReturnAllEffect.java、WeepingCurtainEffect.java。其他B原范围只有必要接线才改，禁止A的network/bind、C的domain/登记、query/Mixin/旧计费。

统一调用A的PublicationAttempt（精确签名见repair-commit-contract.md）。B不造另一观察器，不自己收费。真正发布/改变控制状态之前查token及当前attempt.allowsPublication；addFreshEntity实际返回true或剑真正进入有效控制状态后立即reportAccepted，同计划复制份多个实体报告幂等由A处理。返回false/早退/未改状态不能报告。已经发布成功后，即使期间cancel令牌变无效，也要报告该真实成功，不能先再次核验token把成功通知丢掉。宿主组reportAccepted返回false是其原子路径正常处理，不删已发布实体、不擅自改成松散成功。手动无attempt保持原发布行为。

自动SwordReturnAllEffect无可操纵剑静默失败，不每20tick提示；手动提示保持。必要时让真正控制方法返回/报告真实提交，不能仅在调用前猜成功，不按术式ID例外判断。

自动垂丝恢复原计划复制序号0.72横向布局：用本轮显式源点/方向，不读眼位准星；原有落点/交错等规则的位置优先级保持，不无条件再偏移覆盖规则位置；每份最终生成源点钳域内，边界重合可接受；函数轨迹、世界竖直下落、唇口、寿命和无复制时行为不变。

A并行实现回执/预算，你按已落盘契约编码，不看他中间代码、不改其文件。C已修句柄交接，不由你改。若新接口确实无法落地，返回具体事实而非改签名。

所有拟改旧文件先用获准原生工具完整保存当前正文到backup-B-repair-1/相对路径，读回确认后再改，不能再省略大文件备份。这是返修前快照，不是假称首次实施备份。不能完整保存就停该文件报告；Shell若Rejected不绕过，不更改审批/模型。

不编译、不测试、不打包、不游戏、不改表、不提交、不另审。完成后写repair-B-1-final.md及evidence-B-repair-1.json，released=true，列真实发布接点、空剑/幻剑早退结果、原子宿主处理、垂丝规则优先级与源点限制、未验证项，待统一编译。保留全部其他未提交变更和查询接线。

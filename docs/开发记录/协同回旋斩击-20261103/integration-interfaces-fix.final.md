我先读任务文件和这次的修复提示，只按它们写明的范围改接口。复核指出两类签名问题。我先对照 `TechniqueExecutionCapabilities` 和执行族计划的实际源码，再只改冲突的名字和文档。源码里静态工厂和实例判断同名同参，Java 无法编译。检查本身只把 `cancelId` 对上取消令牌，完成令牌是另一枚。我保留工厂名，并把实例判断改成 `is` 前缀。文档里“完成令牌不参与比较，又不能等于 cancelId”仍容易读岔。我改成：完成令牌只要求与取消令牌不同，`cancelId` 只对取消令牌。接口里的两处签名问题已经按源码改完，清单与代码一致。

`TechniqueExecutionCapabilities` 里，静态工厂和实例判断原来同名同参，Java 无法只靠 `static` 或返回值区分。工厂名保留。实例判断现为：

- `boolean isTimedRelease()`
- `boolean isStandaloneDeployment()`
- `boolean isExplicitAttackAdapter()`
- `boolean isCooperativeRule()`

`ExecutionFamilyPlan` 的检查本来就符合合同：每个 `AttackPreparation.cancelId` 必须等于 `cancelToken.tokenId()`，`completionToken` 必须是另一枚令牌。交付文档里那句容易读成两枚令牌都等于 `cancelId`，已改成这个含义。

范围内没有其它调用点。本次未编译。

<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done","summary":"实例判断改为 isTimedRelease、isStandaloneDeployment、isExplicitAttackAdapter、isCooperativeRule。cancelId 只等于取消令牌，完成令牌保持独立。","changedFiles":["D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/cooperative/TechniqueExecutionCapabilities.java","D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/cooperative/ExecutionFamilyPlan.java","D:/alianximod/docs/开发记录/协同回旋斩击-20261103/interfaces-delivery.md"],"checks":["对照 TechniqueExecutionCapabilities 源码与交付文档的最终签名","对照 ExecutionFamilyPlan 中 cancelId 只比较 cancelToken"],"notRun":["未编译","未游戏验证","未新增正式复审","本次未重新执行命名门禁"],"writesSettled":true,"blocker":null,"nextAction":"主会话转交接口修订后的最终清单给C/E/V，不另开逐项正式复审"}
<<<END_CURSOR_TASK_REPORT>>>

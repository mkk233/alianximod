模型：Cursor CLI --model grok-4.7-high-fast --mode ask。阶段：实现第1/3轮集中只读复核。只在主会话明确派发后执行；派发即表示A/B/C候选最终反馈已齐、真实接入完成并已停写。无独立测试阶段。禁止改任何文件、编译、测试、打包、启动游戏、改表、提交、派其他代理。保留工作区所有旧改动。

依据：docs/分身底座设计与实现指导.md；本批 implementation-contract.md、author-A-core-report.md、author-B-action-report.md、author-C-integration-report.md、author-C-apply-report.md、对应evidence/manifest及记录.md。只审本批新增和相对before-C/实际接入基线的本次增量，不把全仓未提交修改当成本批，不复审其他聊天落点/热波或旧配置批次。需要调用关系时只读相关方法。

范围：设计第14.1节与冻结合同第2节实际接入文件；core、action、CloneBalance、新的注册/能力/命令/渲染，以及C所有公共入口增量。源代码必须位于真实src/main，不以候选已写代替接入。核对作者完成声明与实际代码，有缺漏明确报告。

集中检查：
1. 默认PLAYER/旧构造与注册顺序兼容，getAll完整，开发过滤覆盖学习/获取/刻印/书/装配/强塞NBT。SkillUsePacket在四种收费分支之前整链拒绝，不推进游标/使用计数/保留按住循环；普通既有术式无行为改写。
2. 配置seal时三个真实Technique/释放定义都已登记；标准键无重复，pulse伤害白名单、真实消费者、SERVER侧、默认/非法值与配置0语义，伤害与费用/冷却只缩放一次，任何static初始化不提前读快照。不能虚构service id或把无效键写成有效。
3. owner/actor/session/generation/token隔离、实体出生ticket/容量/索引发布与回滚、真实addFreshEntity事件先后；出生不因与主人重叠失败，也不能卡实心/越界/强载。noSave/未知NBT/summon拒绝、离线/死亡/换维/区块世界卸载/停服同JVM重开清理。长期驻留不能被意外自然despawn；保留重力/伤害且不生成经验/掉落。
4. 一次响应只在提交线消费；主动态与受击竞争、下tick队列过期/上限/公平/每tick单次、冷却0、owner记录上限与回收；ENDING优先避免finally复活状态；source作用域在异常/嵌套恢复，分身不互相递归。原始super.hurt接受且非致死才反击，环境/主人/友方保护符合合同。
5. actor眼睛几何、方向冻结与反击UUID重解、方块阻挡和精确AABB最近交点，CandidateQueries复用但权限实时，owner/分身/友方/可识别宠物排除。明确DamagePermission在正伤前调用，classifyRespecting不Root，0贡献成立、不种火/潮印、不共享Tally/旧ChainScheduler，不从owner眼睛偷发。
6. 收据原池/原杖退款，失败前最多退一次；成功/提交后免疫取消异常不退款，不共享lastReceipt；容量reservation防重入，取消生命周期与请求不泄漏/不越界扣钱。
7. 诱敌有真实入口/已加载范围/原版僵尸骷髅白名单/目标关系/可见性，有限租约及结束只清仍指向自己的目标；空实例不全实体或全玩家扫描，预算与周期错开；无新线程，所有世界/伤害/费用服务端线程。
8. attributes/entity/renderer专服隔离与Forge跟踪、同步字段变化/动作消费、权限2本人命令拒绝与异步结果提示；只原版验证资源无新美术。性能枚举追加不改CSV或重复统计，现有关闭门控保持。

只输出可行动问题（严重度、准确文件行号、触发条件、影响与最小修法），再说明剩余不确定/未验证与审查覆盖。不要凭空写游戏已通过/兼容已保证。若未发现阻断也如实给边界，不生成测试或打包。缺少最终接入/作者未停写等前置时只报告阻塞，不审中间版本。本单所有发现一次性集中报告，之后停止。报告控制在6000中文字以内。

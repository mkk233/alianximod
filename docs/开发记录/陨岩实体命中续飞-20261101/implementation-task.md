# 陨岩命中实体后沿当前弹道继续飞行

固定 Cursor CLI --model grok-4.7-high-fast。本阶段：单作者实现、必要作者检查，完成后停写等单独最终编译。禁止本阶段Gradle、打包或游戏。Codex只调度，不审代码。

## 当前用户授权

用户最新原话：“接下来修正一下陨岩，陨岩现在命中后会在命中位置生成石块并落下这个效果不好，撞到实体应该继续以目前弹道移动直到触地才变成方块”。

主会话已按用户意图明确：命中实体后沿当前弹道继续移动，触地才转方块；保留已有首次命中结算，避免续飞造成重复伤害。这是现有实体碰撞/运动生命周期修正，不重画素材或改模型，不需要重开美术选稿。

新决定取代旧文档“空中命中立即生成无伤残骸再直坠”的描述。不要因旧描述阻塞，也不要重复询问是否执行。原批最新记录设计2/2、实现3/3已耗尽（旧C ownership的2/3已过时）。这次是已授权返修，不追加正式复审，不调用子代理或额外检查会话，不重置配额。允许完成必要实现检查、编译及写回；不得写成新增正式复审通过。

## 输入与范围

读取 D:/alianximod/AGENTS.md、.grok/skills/max-dispatch/SKILL.md、docs/统一开发与术式批量制作流程.md、docs/配置平衡指南.md 的适用约束。只读当前 docs/术式制作表.xlsx 中 B=fire_meteorite 的完整N/O/DV、必要最新详细快照；历史来源见 docs/开发记录/术式批量制作/待制作-20261031/approved-v1.md 和 author-c-content-final.md，最新用户决定优先。

先定位当前实体命中→飞行体结束→残骸/落块这条直接调用链，再最小实现；不通读无关整仓。你是唯一源码作者，已知旧作者均已收束。保留已有未提交改动，禁止reset/clean/checkout覆盖。

## 必须实现的行为

- 第一次合法命中实体时，已有中心冲击、附魔、接续与费用按原语义只结算一次。实体接触后继续维持飞行陨岩的表现与运动，不在接触点立即生成静态方块/方块式直坠残骸、不清零横向速度、不重置为初始弹道，也不重新读准星或目标位置。
- 继承命中瞬间实际的位置、速度向量、弹道状态及当前重力/修饰结果。抛物线继续抛物线；垂落/喷涌/已生效改写仍按其当前状态延续。不能把“继续弹道”实现成无重力无限直线。保持位置/表现连续，不在命中点停住、跳回起点或凭空多出第二块。
- 沿途再次重叠同一或其他实体不会反复打伤害、反复触发中心/热波/命中接续/消费，不能因同一碰撞每tick卡在实体内部；这是第一次攻击已结束后的运动延续，不新增穿透多次伤害。
- 实体命中后的飞行继续处理地形碰撞，首次合法地形接触后才走既有安全落块流程：有合法空气位/支撑/权限才安放，不穿墙、不覆盖任意地形、不无条件强载区块。无合法位置时保持既有无伤等待/重试与持久保存。已结算的中心/热波等不得在落地再次触发；崩地和安放按既有落地约束执行，避免重复。
- 没有提前命中实体而直接触地的陨岩，保持原来的一次撞击、一次接续和落块行为。未命中时的原有攻击超时策略不无故重做；已命中续飞分支不能被旧攻击寿命提前清除、空中变方块或再次计费。服务器为准，客户端表现同步连续，存档/区块卸载重载不重放伤害、不复制残骸；必要状态沿用项目现有约定。
- 保留当前伤害、半径、体积、费用、配置参数及缺失配置回默认。与最新热波v2及陨岩中心排除语义兼容。不顺带改聚能、其他术式、火种账本、地形总开关规则或全局碰撞策略。

本阶段只做与上述授权直接相关的改动。若无法在这些明确语义内实现，最终报告具体阻塞，不擅自扩大设计或重开正式复审。

## 文件所有权

允许最小修改：
- D:/alianximod/src/main/java/com/bslx/XueXiMod/carrier/BatchMeteoriteEffect.java
- 同目录 BatchMeteorLanding.java、BatchFlightRecord.java、BatchFlightBook.java、MeteorContactKind.java、MeteorContactResult.java、BatchCarrierHooks.java、BatchCarrierVisual.java，以及仅为本陨岩续飞必要的新小型帮助类。
- D:/alianximod/src/main/java/com/bslx/XueXiMod/terrain/MeteoriteWreckageEntity.java
- D:/alianximod/src/main/java/com/bslx/XueXiMod/client/MeteoriteWreckageRenderer.java、BatchCarrierVisualRenderer.java（仅确有续飞同步/外观连续性必要，不改贴图或其他特效）
- D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitEntity.java、TechniqueHitSpec.java（仅当现有通用生命周期缺少必要能力；新钩子必须通用、默认保持其他术式行为，不按陨岩id硬编码公共命中逻辑）
- 本目录 implementation-ownership.json、implementation-report.md、implementation-result.json、requirement-amendment.md、必要作者检查记录。

源码范围外确有必要先报告具体最小文件与原因，不擅自改。不要改制作表、总资源索引、已批准美术文件、其他任务记录或共享文档。源码作者不写表；唯一writer之后处理。

## 交付

作者必要检查聚焦本次状态流：空中命中→续飞→触地；直接触地；再次实体接触不重复；不同弹道/实际速度继承；地形不可安放的后续；客户端同步与已结算状态存档。沿用现有可用检查手段，不新增独立测试阶段、不重做整仓正式正确性审查，没做的实际运行验证必须明说。游戏由用户测试。

保存 requirement-amendment.md，包含用户原话、被替代的旧行为、完整新行为及不改变的首次结算/安全安放约束，交唯一writer更新当前需求。最终 implementation-report.md/result.json 给出实际根因路径行号、修改文件、首次命中后如何保持真实弹道与渲染、重复结算如何避免、检查已做/未做、source_writes_closed:true、needs_compile:true、blockers。报告真实CLI会话编号；不知道就留null，不猜造。不要启动编译，停写后等 compile-task.md。

# 水幻形D：无候选资源的表现协议与客户端容器

实现任务，固定Cursor CLI `--model grok-4.7-high-fast`，不降档、不spawn。完整阅读AGENTS.md、max-dispatch技能、现行流程、配置指南、本批 implementation-contract.md 和 docs/水幻形术式设计与实现指导.md。按已冻结A/B/D合同，不重裁决架构。设计轮2仅处理C载体门，不修改你的协议接口。

A作者正在clone/core，B正在clone/action和clone/capture；不要读它们进行中实现，不替它们改文件。A的新DTO方法以最终 implementation-contract.md 签名为准。C稍后写release/capture、clone/water及公共释放/装配/性能枚举。你唯一写入：`src/main/java/com/bslx/XueXiMod/client/CloneRenderer.java`、新CloneAfterimagePlayback/Snapshot/ActorVolumeVisual等客户端纯表现类、`network/ClonePresentationS2CPacket.java`、`network/ModNetwork.java`、`XueXiMod.java`中必要客户端挂接；以及本批 implementation-d-report.md。不写其它源码/资源/表/艺术文件。

最新视觉：用户否决圆润v1，v2为真实Blockbench直接绘制的标准Steve六方体透明水线，刚展示水感修整候选，等待用户确认。你不操作美术MCP、不复制候选图片、不写死其几何/UV/速度/碎片，更不注册正式water_phantom。你现在做的仅是设计15.1与合同8的D独立范围。

实现：
- S2C类型化schema与所有字段/方向/有限值/时长/维度/session/版本/资源枚举校验。ModNetwork现实时检查包号，取当前空号，不盲用历史16。接收仅enqueue客户端线程，不回服务器业务。
- 安装A的ClonePresentationSink，世界坐标事件无需被移除实体引用；按A合同接收者发出，避免重复。不得客户端类装载污染专用服务端。
- 纯客户端世界绑定、有界256快照、去重/序号/终止标记/到期清理；实体remove不抹掉独立散水；世界对象切换、换维、断线清空；迟到维持不复活已结束token，终止表也有上限与过期。
- 对旧LEGACY_HUMANOID保持原CloneRenderer行为；WATER_OUTLINE暂早退不画僵尸。因为尚无正式注册不会被玩家取得，不能宣称水幻形已正式完成。
- TIDE_SLASH_FULL使用既有正式贴图 `xueximod:textures/entity/xueximod/horizontal_slash.png` 全UV，世界尺寸/位置方向与ActorVolumeSpec相同；不生成TechniqueHitEntity、不借旧伤害业务。只提取纯渲染，不能修旧水系玩法。
- 为以后已确认Steve人形/散水留明确入口，容器无绑定候选帧数。缺其它组class按冻结签名写消费者，最终报告等待合并，不写别人的提供者。

保留全部既有改动，无reset/clean/checkout覆盖，不打包、不游戏、不Gradle，本任务待统一编译。可运行命名门禁，只修自身命名，不改其它组正在写的源码。最终一次性报告文件/行号、包号及兼容策略、线程/世界清理、未完成的确认依赖、未编译未实测、停止写入。不自行另开审查。主会话只等最终反馈。

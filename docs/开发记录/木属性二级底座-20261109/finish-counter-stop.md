# C补齐本阶段最后停服接线

保持author-integration原可写session、固定grok-4.7-high-fast。主会话已对账你上一轮最终反馈与写入收束；这是同阶段依赖到齐后的接续，不是另一个复核轮次。

B已完成停服API，完整最终证据在B停服API最终回执.md；attempt a20261109100522753b9cf，reported_done/valid/writesSettled=true。现在可只读B收束源码与通用效果作者交付.md，禁止修改其私有代码。

将CounterRuntimeEvents的ServerStopping处理替换为无条件调用CounterService.onServerStopping(event.getServer())，不再扫在线玩家模拟全清。同步清掉C自己维护的当前线程事件槽/计数（如有），不保留同JVM重开服残留。没有在线玩家时同样运行，正常停止按服务端线程接线。保持已有木系停止接线与其他生命周期行为。

你上一轮明确报告既有src/test/java/com/bslx/XueXiMod/network/GuardSyncWireCheck.java仍断言协议2。TaskFile现已授权你唯一修改该文件中由本次协议升到3引起的必要适配；保留原有其他断言，不削弱原网络检查。说明应如何运行该既有检查，若它测试的是旧客户端拒绝，也要维护正确含义而非机械替换全部2。不要执行检查或Gradle。

只做已列待接事项和相应交付说明更新，不重审或重写已完成模块。更新整合交付.md和迎击整合交付.md，删除“停服API尚未接入”的过时阻塞，明确本阶段完整交付而仍待统一编译。若你上一轮已根据主会话回执完成此调用，则核对并如实报告，无需重复修改。

不运行编译/脚本/游戏，不改变炉心、术式、美术或配置，不改批次记录/运行元数据/制作表。最终报告写入已收束，列实际改动与未运行项。

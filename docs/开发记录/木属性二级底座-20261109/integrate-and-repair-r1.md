# C 当前阶段：资源公共接线与第1轮定向返修

沿用原author-integration可写session，固定grok-4.7-high-fast。你的迎击接线attempt a2026110909435920551bc已reported_done/valid/writesSettled=true；集中只读复核a20261109095136677a42a已结束（实现1/3），具体报告本目录实现复核1.md。先读TaskFile和该最终报告，不重审整批。

执行本目录integrate-resources.md的全部资源公共接线，A已经最终交付，无写入。同步完成C范围内两项返修：
1. 伤害事件帧栈每次进入包装压入一个明确槽，事件只写当前槽，正常/异常退出恰好弹出当前槽，不能用notes.size与depth推断是否属于当前调用。正常退出只把当前槽实际事件的最终取消传回服务，异常只丢弃。保留ElementDamageScope的正常/异常finally与原hurt返回。补可执行的必要嵌套回归，覆盖外层无伤害事件、内层先完成且被晚取消、异常退出、空槽，测试真实使用的帧栈（可在你拥有的combat/event目录提取最小纯数据辅助类）。不得复制公式假造测试。
2. 死亡监听LOWEST，并显式只处理未取消事件；资源死亡事件同样不能对已取消死亡清账。

B现正独立补CounterService停服全表清理API。禁止读写B运行中的counter私有目录或MagicPool；你可依据已完成原B交付API修C帧栈，新增停服API必须等主会话带入B本轮最终回执后才调用。先把资源接线和上述两项修复完成，若尚未收到B新回执，交回已收束结果明确“等待停服API接线”，不要声称整合全部完成。主会话随后Resume本C任务补最后停服调用，不开新作者、不改计数。

只能改TaskFile明确C路径，资源A私有目录只读；保留全部已有脏改动。禁止编译、运行检查、打包、启动游戏、具体花谱或术式。更新整合交付.md、迎击整合交付.md及必要检查说明；最终JSON如仍等B停服API则workStatus=blocked且writesSettled=true，列明精确待接点，已完成部分不可回滚。全部接口到位才reported_done，依然待统一编译。

# 水幻形实现第1轮复核返修
固定Cursor CLI `--model grok-4.7-high-fast`。实施已明确修复，不是追加正式审查；设计2/2已用完，实现已1/3。本次不复审旧范围、不spawn、不打包/游戏/Gradle/改表/候选资源/正式注册。完整读本批 review-nonvisual-r1-final.md 与自己的最终 implementation-*-report.md。源码作者当前均已停写，按本单重新取得指定文件唯一写入权。其它组并行修各自文件，勿越界。保留已有修改，禁止reset/clean/checkout。跑命名门禁可只修自己改动。最终报告明确改动/行号/未编译未游戏/停止写入，不自行追加复审。所有最终反馈齐全再由主会话集中复核。

A唯一范围：clone/core内实现一个给D服务端发送使用的主人查询，不改D/C/B文件。报告fix-r1-a-report.md。
复核问题2的最小修复需要D按deploymentId补发同维owner。请在CloneService公开精确接口：
`public static @Nullable ServerPlayer findPresentationOwner(MinecraftServer server, UUID deploymentId)`
从本次session的部署/lease表确定主人，只接受当前服务器线程、当前server/session的真实部署，查在线主人；不依赖已discard身体，不以旧世界同UUID记录作答。按当前字段/表写一个只读查询，可调用core包内辅助；不要改S2C线格式和ClonePresentationNotice构造签名。
核对本次已存在的SCATTER/VOLUME同步publish时点，确保该查询此时确实能查到仍有效的部署主人；若表在实际发布前已经摘除而无法在现有合同内实现，请具体交回，不靠临时全局无限表或返回无关玩家掩盖。不要重新判断其它两个问题或扩展新功能。D将调用上述精确签名，与其互不写文件。


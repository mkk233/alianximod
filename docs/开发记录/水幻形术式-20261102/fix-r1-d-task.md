# 水幻形实现第1轮复核返修
固定Cursor CLI `--model grok-4.7-high-fast`。实施已明确修复，不是追加正式审查；设计2/2已用完，实现已1/3。本次不复审旧范围、不spawn、不打包/游戏/Gradle/改表/候选资源/正式注册。完整读本批 review-nonvisual-r1-final.md 与自己的最终 implementation-*-report.md。源码作者当前均已停写，按本单重新取得指定文件唯一写入权。其它组并行修各自文件，勿越界。保留已有修改，禁止reset/clean/checkout。跑命名门禁可只修自己改动。最终报告明确改动/行号/未编译未游戏/停止写入，不自行追加复审。所有最终反馈齐全再由主会话集中复核。

D唯一范围：client/ActorVolumeVisual.java、network/ClonePresentationS2CPacket.java，必要自己客户端辅助但不改A/C/B。报告fix-r1-d-report.md。
问题1：VOLUME notice.worldPosition已经是服务端volumeCenter，ActorVolumeVisual.draw用snapshot.anchor()直接作中心，去掉二次forward*depth/2；MAINTAIN/SCATTER脚底锚点不改。
问题2：relay在附近160格接收者之外，补充同维在线主人。A同期提供只读精确接口 `CloneService.findPresentationOwner(MinecraftServer server, UUID deploymentId)` 返回nullable ServerPlayer。你按冻结签名消费，不写A helper；已discard身体也可根据部署租约查主人。不改包布局或内部Notice构造签名。接收者去重，同一主人已在附近时只发一次；保留既有actionToken+phase/序号发送账规则，同维检查不能省，别扩成全服广播。保留线程/世界清理。
只修这两处，不接候选材质，不正式注册、不重做协议其它部分。若A接口尚未落地，按签名写消费者后最终报告依赖；主会话会等两边最终反馈，不用你改别人的源码。


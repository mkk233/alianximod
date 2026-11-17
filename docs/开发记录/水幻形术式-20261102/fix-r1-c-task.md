# 水幻形实现第1轮复核返修
固定Cursor CLI `--model grok-4.7-high-fast`。实施已明确修复，不是追加正式审查；设计2/2已用完，实现已1/3。本次不复审旧范围、不spawn、不打包/游戏/Gradle/改表/候选资源/正式注册。完整读本批 review-nonvisual-r1-final.md 与自己的最终 implementation-*-report.md。源码作者当前均已停写，按本单重新取得指定文件唯一写入权。其它组并行修各自文件，勿越界。保留已有修改，禁止reset/clean/checkout。跑命名门禁可只修自己改动。最终报告明确改动/行号/未编译未游戏/停止写入，不自行追加复审。所有最终反馈齐全再由主会话集中复核。

C唯一范围：release/capture/ActorCaptureDispatcher.java及该包必要的生命周期辅助，不改A/D/B或公共XueXiMod事件。报告fix-r1-c-report.md。
复核问题3：ACCEPTED_TICK静态owner→tick表不应跨服务器会话残留，也不应无限积累历史owner。按最小修复，使它只保存当前server/session、当前server tick成功的部署记录，同tick拒绝第二次成功仍保留；切换server或tick清旧记录，停服清空，主人登出移除其记录。生命周期订阅可放你自己的capture包辅助或本类，不要求A改公共生命周期。不要只靠相同tick数字区分两个server。不要存活玩家对象，不改付款/发布成功才计数的边界。普通链不创建此状态。线程使用现有服务端调用合同。
修复不涉及载体或装配设计，不重构这些已经通过的入口，不触碰正式注册/资源。最终明确停止/登出挂接与已成功部署节流语义。


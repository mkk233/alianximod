# 性能记录：实体与命令只读定位

模型固定 Cursor CLI --model grok-4.7-high-fast。当前是只读信息采集，不是正式正确性复审。你就是执行 Cursor，不再委托任何代理。
用户已授权只开始性能记录；不实施缓存、查询复用、C/JNI、异步或玩法优化。
允许读取 D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitEntity.java、TechniqueHitSpec.java、shu/technique/sword/SwordEntity.java、command/ShuCommands.java、debug、XueXiMod.java 及现有服务器事件设施；可搜索文件名定位服务器 tick 生命周期设施，再读取直接有关的方法。相对路径均在 D:/alianximod/src/main/java/com/bslx/XueXiMod 下。
目标：定位服务端实体更新、运动、候选查询、精确碰撞、实际伤害、生成、显式同步的插桩边界，以及命令注册、服务器 tick/start/stop 接入点。只能读定向代码，不整仓通读。可读 AGENTS.md 和 docs/底座功能点.md。
输出真实路径/行号/方法，当前线程和客户端分支，安全的 disabled 快路径候选，已有调试/性能功能，哪些网络耗时只能测到入队不能当真实带宽和传输耗时。指出同 tick、多实体、延迟结算的计数语义。
禁止写入、实现、编译、游戏、打包、提交、制作表操作；保留所有未提交改动。git 用单次 -c safe.directory=D:/alianximod，不改全局配置。
只返回最终定位报告（建议不超过 6500 汉字），不做正式复审结论。报告写明没有改动。

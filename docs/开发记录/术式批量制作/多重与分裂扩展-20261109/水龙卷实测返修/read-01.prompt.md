这是 D:/alianximod 实际开发执行的必要故障定位，不是正式正确性复审。直接使用喜鹊当前路由的执行能力。原 task author-adapters，batch release-variants-20261109，原 Cursor session 4729bce8-8440-4379-8bf8-849f2cdfc5f5 已结束；现按2026-11-09新流程换HTTP工具客户端，旧会话ID不能用于新提供方。原批次设计2/2、实现3/3已用完，不追加正式复审。

用户刚报告：“我测试了三重释放，装配链路上三重+深海重压+上升气泡+旋流，如果我在空中释放它没有正确组成水龙卷，如果我在地面释放虽然会正确组成水龙卷但是一旦卷入实体就会立刻溃散”。这是已授权开发任务的游戏返修。定位两个现象的直接源码原因，不能猜是三重必然有错，也不能取消既定水势不足/空间溢出溃败机制。

本轮只允许通过read_files/search读相关Java和既有需求/交付文档，不写源码，不编译、不启动游戏、不写表。主会话只调度，代码读取由你完成。优先读取：
docs/开发记录/术式批量制作/水流组合-20261103/需求快照.md
docs/开发记录/术式批量制作/多重与分裂扩展-20261109/适配作者交付.md
src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/motion/VolumeMotionRuntime.java
src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/motion/StillWaterLease.java
src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainService.java
src/main/java/com/bslx/XueXiMod/shu/WaterTechniques.java
src/main/java/com/bslx/XueXiMod/network/MulticastCompileAdapter.java
之后按实际依赖读取必要类，避免整仓。

既有批准：深海重压生成真实临时水域支持气泡；旋流+水域+上升气泡基于通用能力组合，不能ID套装特判。水龙卷入卷按潮湿分档水势收费，每次释放同实体一次，复制共账本；高湿支付/返还原子结算，不能中间余额0导致溃败。水势不足和负载超限允许立即溃败，但要查默认库存/收费/扣费时点/实际体积是否产生一只普通实体就必溃败的错误。气泡竖直运动优先深海下压。空中释放应通过正常空间/落点机制组合；不擅自把全部术式都贴地或扩大范围。
还请排查水势归属账本、临时水的空中可放置约束、气泡建立/旋流识别的时序，以及释放者在空中时三个子成员的实际origin/aim是否一致。

输出中文最终故障报告：分别给已证实源码事实、尚需游戏验证的推断、最小修复建议、精确需修改文件路径；如果现有机制本身符合批准但用户期望涉及设计变化则明确指出，不擅改平衡。缺证据就列精确缺失信息。不要额外做全批审查，不输出“复审通过”。可检查现有配置默认与run/config实际覆盖但不要读取无关个人信息。必要时使用源文件搜索定位文件名。

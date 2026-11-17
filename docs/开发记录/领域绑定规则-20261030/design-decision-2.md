# 领域绑定规则：设计第 2/2 轮实施契约裁决

本轮只读取已完成的 implement-A-contract-retry-final.md，承接 design-decision-1.md，不读取 Java，不采集新源码事实。implementation-contract.md 为本轮唯一正式接口定案，A/B/C 可据此立即推进不冲突文件。

保留草稿确切 bind 包、BindingLayout/BoundReleasePlan/BoundMember、ReleaseExecutionContext、BoundReleases、AutomaticRound、GroupBudget、DomainService 重载及 aim.LaunchAim 等类型安排，修正以下实施偏差：

- 持剑/装备资格在实际成员生成时检查当前主手，武器副本仅冻结本轮数值；已生成自动实体沿用自身寿命。
- 目标候选最终完整包含于真实领域；跨 tick 仍经 TargetCheck 验证。LaunchAim 构造补 TargetCheck，已生成实体不持领域生命令牌。
- RoundHandle 补 done()，明确正常/失败/取消终态和实例清理，避免历史轮无限积累。
- 上下文 bind/unbind 必须可嵌套恢复，异常 finally 配对；空手 ItemStack.EMPTY 合法，副本防御复制。
- BindingLayout 用可安装 Compiler 桥接现有 network 结构解析，避免 bind 反向依赖 network；普通未绑定领域不被误判非法。
- 外部文件已由主会话确认 released：TechniqueHitEntity/TrackingSupport 交给 B，ReleaseBatch 按需交给 A，保留已完成的候选查询优化；其余释放文件不自动扩大本次写权限。

结论：契约可实施，无新增玩法阻塞，不需再向用户提问。C 已实施的角色登记和几何 samplePoint/clampPoint 可继续。其他作者以正式契约修正草稿后编码，同文件唯一作者，未交接不覆盖外部写入。

设计累计 2/2，已用完；实现仍 0/3。上一任务计数不变。本轮未实现、编译、测试或运行游戏；后续不自动追加设计正式复审。

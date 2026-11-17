# A返修：落实真实提交与部分退款

固定Cursor CLI --model grok-4.7-high-fast，继续A原可写会话。读review-decision-1.md与自己已完成的repair-commit-contract.md，按精确签名完整实现R1/R2/R3的A部分。实现累计1/3，本单是返修不另审，设计2/2已耗尽。

写权限：你现有AutomaticRound、GroupBudget、SkillSegmentRunner和bind包（含新增PublicationAttempt、必要ReleaseExecutionContext接线）；确需时ReleaseBatch仍由你独占。禁止改B/C源码。B将并行按repair-commit-contract调用current/allowsPublication/reportAccepted；不要另改签名或让B猜。必要内部初始化桥/工厂由你实现，保持bind不反向依赖network。

彻底移除effect.execute正常返回就成功的依据。松散每计划复制份一个attempt，真实发布接受后记成功，不按实体数量收费；HostGroup继续原原子COMMITTED回执。整轮报价不变，份额确定分解总和等于原价，规则固定份额最多保留一次。部分失败只收成功份额，当前资格失败、空效果、没有剑、addFreshEntity=false都不虚构成功。

取消请求/在途数量/真正结清分开；cancel立刻阻止新尝试与继续发布，有在途暂不退，accepted回执即使重入cancel也必须保住已生成份额；close先提交真实成功份再退其余，异常传播但必须先完成回执和预算清算。done真实代表预算结清。回执栈嵌套恢复、同份重复报告幂等，不能把宿主组暂存发布当松散成功。按契约处理返回true后异常、复制循环中断、第二成员取消，不能全异常统一收费或统一退款。

本次先用原生获准文件工具把所有拟改旧文件完整现行正文存backup-A-repair-1/相对路径，读回确认后才能改。首次实施backup-A缺失已经记录，不再用事后数据冒充；本轮必须有真实返修前内容备份。不能完整保存就停该文件报告，不继续修改。Shell若Rejected不绕过，不改审批、不换壳执行同拒绝命令。

禁止编译、测试、打包、游戏、表格、提交或额外复审。保持候选查询优化和所有非本次变动。结束后写repair-A-1-final.md、evidence-A-repair-1.json并released=true，给接口实现、实际收费单位、四种失败/异常/重入路径的代码接点、未完成项和待统一编译。只读自己必要依赖，不看B实时日志/中间代码，不越权替他修。

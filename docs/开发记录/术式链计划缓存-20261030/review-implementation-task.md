# 实现阶段集中只读复核：累计第2/3轮

仅由主会话确认A/B最终返回、相关写入收束后派发。本轮承接整体性能改造实现1/3，登记为2/3；设计2/2已用完。不追加设计判断、不再派代理或额外复审。固定 Cursor CLI --model grok-4.7-high-fast，工作区D:/alianximod，仅只读。

先读本目录design-decision.md、implementation-A-final.md、implementation-B-final.md、evidence-A.json、evidence-B.json和记录.md。仅检查两作者实际修改/新增源码，以及验证具体问题必要的直接依赖。对比各作者backup-A/backup-B/中的开工前版本，不拿HEAD中其他已有未提交改动当本次新增。不要通读全仓。

围绕已批准契约集中给一次实现正确性结论：各入口是否实际消费缓存；fallback与命中是否保留费用/冷却表达式、舍入/读取时点、5tick/6tick/蓄力、随机调用次数；同一segments成员身份；ModifierPlan是否每次新建；缓存递归不可变且无运行对象/闭包；未知定义是否编译前bypass；registry/Water fluent失效；延迟epoch变化是否保持旧图并只回退原动态读取；内容键和progressSignature语义是否分离；长度/容量/预算与服务器线程生命周期；hit/miss/bypass计数互斥与真实split/role-scan/compile口径。不要重设计功能或引入新范围。

如有问题，给文件绝对路径和准确行号、可复现触发、影响、最小修复建议及归属作者A/B，按严重程度排序；无问题明确未发现阻塞缺陷，但说明静态检查不替代编译/游戏实测。报告需列实际检查覆盖范围、未验证项，以及是否存在未完成交付。不要用泛泛建议制造返工，不做独立测试，不运行任何gradlew，不修改任何源码/文档/表/配置，不提交。

最终返回完整一次性报告，主会话包装器会将你的最终result保存为review-implementation-final.md；你不要自行写报告文件。结束时明确只读、没有后台作业、没有持有源码写入权。若有输出截断，同一会话补全事实报告，不重新开一轮审查。

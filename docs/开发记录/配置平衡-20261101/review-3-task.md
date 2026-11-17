固定Cursor CLI --model grok-4.7-high-fast，续用独立只读复核会话。本阶段实现第3/3轮（最后一轮）集中复核，承接已完成2轮，设计2/2已用尽。只在主会话明确派发、记录.md确认repair-2 A/B及B-tracking、文档都最终交回并释放写入后开始。禁止写文件、修复、编译、测试、游戏、派代理/CLI或提交；本轮终稿后不自行追加复审。
读取review-2-final.md、repair-2-A/B-final.md、repair-2-B-tracking-final.md、repair-2-docs-final.md、本批命中裁决接口.md及最新manifest/pre-repair-2；按本批实际差异和必要依赖复核，不拿全仓历史改动作为本批。
核对本轮闭环：
1. 统一typed裁决：classifyHit/classifyRoot/classifyConfirmed/classifyRespecting有限恰好0在合法权限下为ESTABLISHED_NO_DAMAGE，负数和非有限无效；正伤维持旧无敌帧/领域合并等语义，不能因包装重入打两次或叠两次领域。旧hurt* bool仅指正伤接受。A普通命中/含flow或无flow汇总/资源退休/HitVolumes、B幻剑和接触状态/领域付款/confirmTracked使用continues与grantsSeeds正确分离；0保留成立/接续/状态与原周期，0不因bool混淆产种；非法目标/权限失败不应因0绕过。
2. 原始单价0的普通规则/动作/索敌/剑来等标准manaCost覆盖为正数真正进入原计费次数，默认0仍0；领域/屏障/护环的0宿主占位仍排除，实际生成费不重扣。原有资源附魔、自身状态和条件成功单价的互斥分支不被这一修改破坏。
3. 检查受这次收费覆盖影响的金额求和/乘份数，以及本批接触单价乘类型数（DomainContactCharge/Billing等）仍不会整数回绕成免费，退款为原收据，不加无授权收费点。
4. 指南状态/0伤/产种/免费改价/10000火伤上限与最终代码一致，示例未变坏，未编译/游戏未测如实。
上一轮已确认且本轮无依赖变化的火伤/JSON/同步/尺寸默认路径不重复全量审查；只回看真实关联影响。对B先前未逐行对比全部pre-B的限制如实保留，不为了凑通过写成全量验证。
最终一次完整stdout给通过/需返修/阻塞、准确行号、触发影响、明确最小修复与作者归属、本轮覆盖/限制；集中列出所有发现。提醒累计实现3/3已用尽，本轮之后不自动另开复核。你不自动修复，也不以额度用尽判通过。统一编译仍由主会话后续指定执行者，游戏未测。OriginalHumanoid既有两处不扩修。


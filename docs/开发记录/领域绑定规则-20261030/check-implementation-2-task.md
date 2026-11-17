# 领域绑定返修集中核验：实现第2/3轮事实输入

固定Cursor CLI --model grok-4.7-high-fast，继续原只读核验会话。仅在A/B/C本轮返修全部返回最终反馈并released之后派发。不修改任何文件、不编译、不测试、不打包、不游戏、不改表、不提交、不绕过Shell拒绝。本轮给唯一gpt-6-astra正式裁决提供代码事实，不另作验收通过判断。

先读review-decision-1.md、repair-commit-contract.md、repair-A-1-final.md、repair-B-1-final.md、repair-C-1-final.md、相应evidence以及记录.md。本次设计2/2用尽，实施1/3后的返修现在进入2/3，不重设额度。只核验R1-R5与直接回归，不重走全部设计/旧接触计费/外部候选缓存审查。你上次check-implementation-1报告可作基线。

源码范围：本轮A/B/C实际变动类型（PublicationAttempt及bind接线、AutomaticRound/GroupBudget/SkillSegmentRunner、TechniqueHitSpec/PhantomSwordEffect/SwordEntity/SwordVortex/SwordReturnAllEffect/WeepingCurtainEffect、DomainInstance/DomainBindingRuntime）和为验证真实调用必要的直接依赖。不通读整仓，不读取实时日志。旧快照只作本轮修改对照，不用git基线覆盖已存在变更。

必须回答具体路径，给确切行号和最小行为例，不用“约xx行”：
1. execute空返回/幻剑两道资格早退/无可操纵剑/addFreshEntity=false如何得到未提交并退款；实际true或控制状态提交之后谁立刻reportAccepted。没有调用成功通知的真实生成点是否仍存在，HostGroup原子回执与松散观察是否隔离。
2. 三复制只成功一份的价格与原报价总和，规则份额只保留一次，同一份多个实体不会重复收费；部分失败、首成员失败后B、全部失败分别如何清算。
3. 正常发布true后异常、复制中途异常、发布过程中重入cancel：在途保护、accepted通知、close/finish/settle顺序、done状态是否能保留已成功份额且禁止后续生成。不要另造未知引擎内部半写异常；依据已批准边界检查。
4. C在executeAutomatic返回及trackRound接点复查失效，能否取消不重新挂回；回执/上下文嵌套恢复和延迟回调是否仍连接本轮预算。
5. 自动空剑静默、手动原提示；垂丝三份恢复0.72横排，规则位置优先级与域内钳制不冲突，无复制/不可变函数/唇口仍保留。A/B调用签名是否真实对上，有无明确编译型缺口。

只输出这些必修项的证据、仍存在的具体问题及未运行验证，不给泛化“整体通过”。报告尽量3500汉字内，有问题优先，无新问题就直说未发现新的具体反例并写实际覆盖路径。源码全部收束后读取，Shell拒绝时用正常原生只读工具，不换路执行被拒命令。完整结果由外层保存。

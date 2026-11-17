模型：Cursor CLI --model grok-4.7-high-fast。任务：按第 1 轮最终复核报告返修唯一阻断。你是 A 的原作者，原候选已接入正式 src/main，之后 C 只调整自己的命令。现在只允许修改真实 D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/core/CloneService.java，不要继续写候选副本或改其他组文件。

先完整读 docs/开发记录/分身底座-20261102/review-1-final.md。复核发现普通分身首次成功后 state.commitLine 遗留 true，使后续失败或过期无法 restoreReady/cancelQueued/cancelPending，永久 RESERVED。既有合同明确该标记只属于当前提交；最小修法是在 settleCommitted 中普通分身从 COMMITTING 收束到 COOLDOWN 时，把 state.commitLine 还原 false。保留本次 Attempt.commitLine 的退款语义，保留 ENDING/ENDED 优先，不让 finally 复活实体，不改变一次幻影的结束顺序。按明确修法实施，不重设计、不扩大到其他事项。若不能在该范围正确落实，停止并说明具体原因，不擅改其他文件。

所有其他作者与复核者已停止；你独占这个文件。保留现有未提交修改。只完成对应修改及必要作者自检，不做新一轮正式复审。可以运行 tools/check-name-gate.ps1 并记录输出，仅报告无关历史问题不要修它们。没有编译授权：不 gradlew、不打包、不启动游戏、不跑独立测试、不提交、不改表。

最终写本批 author-A-repair-1-report.md：准确路径行号、修改、未做项、命名门禁是否真实执行及结果。最终反馈明确停止写入、待统一编译。报告 <=2500 字。若 shell 自动审批拒绝，记录准确命令和原话原因，不绕过；不要以未执行作通过。

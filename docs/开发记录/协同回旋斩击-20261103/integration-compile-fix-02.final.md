先读任务文件和这次编译修复提示，按它们写明的范围做。编译日志里的访问错误要对上这两处调用，再用最小入口改掉，不改锚点语义。领域锚点只在这次预付调用里设置，调用结束就清掉。网络包不能直接碰到包内字段，所以改成同一类上的两个入口。领域预付释放不再直接读写包内的 `FORCED_ANCHOR`。锚点仍只在这一次调用里生效，`finally` 结束时清掉；接触点和落点的优先级没有改。命名门禁已通过。没有重新编译，也没有进游戏。

<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done","summary":"FORCED_ANCHOR改为私有，预付释放改走beginForcedAnchor和endForcedAnchor。设置和finally清理仍在同一次调用内，接触点与落点几何未改。","changedFiles":["D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/cooperative/FamilyExecutionService.java","D:/alianximod/src/main/java/com/bslx/XueXiMod/network/CooperativeCastRouter.java","D:/alianximod/docs/开发记录/协同回旋斩击-20261103/integration-compile-fix-02-delivery.md"],"checks":["powershell -NoProfile -File tools/check-name-gate.ps1 退出码 0"],"notRun":["未统一编译","未游戏验证","本阶段额度已用完，返修结果未追加复审"],"writesSettled":true,"blocker":null,"nextAction":"主会话转达作者结果，停写后原compile-final继续编译"}
<<<END_CURSOR_TASK_REPORT>>>

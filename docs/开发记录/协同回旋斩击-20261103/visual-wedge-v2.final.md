先读任务文件和这次的提示，只按它们写明的范围做。用户已确认薄壳斩光 v2。我先对照交接说明和现有斩击渲染，只替换独立斩击的发光面。确认过的薄壳是五道折线，贴在现有斩击实体上。横劈仍走原来的表现，贴图字节不动。独立斜斩的发光面已换成确认过的薄壳。每个斩击实体最多 32 个三角形，中心仍是实体位置，宽、纵深和倾角用同步几何，0.20 秒相位缩放整个壳。贴图哈希未变。横劈、水幻形、空间切割和上一轮动作返修都留在原处。

游戏不加载 Blockbench 文件。还没有统一编译，也没有游戏验证。开机刻与世界年龄不一致的问题仍在共享执行里，本单没有改那一侧。

<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done","summary":"独立斜斩 emit 的无厚度平面已换成确认薄壳：五道折线、32 三角形、确认 UV 与整壳相位缩放。运行 PNG 字节未变，横劈不使用新壳。共享执行的开机刻未改。","changedFiles":["D:/alianximod/src/main/java/com/bslx/XueXiMod/client/action/cooperative/CooperativeSlashPlane.java","D:/alianximod/src/main/resources/assets/xueximod/models/effect/cooperative_v1/slash_plane_v1.json","D:/alianximod/docs/开发记录/协同回旋斩击-20261103/visual-wedge-v2-delivery.md"],"checks":["powershell -NoProfile -File tools/check-name-gate.ps1 退出码 0","运行 PNG SHA256 58E741C8BED808EB4E8CC6DC77FA71FD009A9AAB0B7FEBC104ACCB68E655BFD7 与确认文件一致"],"notRun":["未统一编译","未游戏验证","未新增正式复审"],"writesSettled":true,"blocker":null,"nextAction":"等全部源码收束后由指定编译者统一编译；主会话只转达，不把本单写成复审通过或游戏验收。"}
<<<END_CURSOR_TASK_REPORT>>>

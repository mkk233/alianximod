我先读取任务文件和这次的提示，只按它们写明的范围改。设计第 2 轮和 I 的接口已经收束。我先对照最终签名和现有短距移动，只在分身核心里加有界的远距召回。公共端口已经把短距和远距分开。我按最终签名读计量器和现有选位，再只改分身核心。短距上限仍是 8 格。远距会在同一次服务端调用里做有界通道检查并瞬时归位，选位按实际位移分流。通道检查不能把脚下的支撑面当成撞墙。我改成只统计身体真正扫到的格子，并留出站立时的那一点间隙。远距归位已经接在分身预约里。实际位移不超过 8 格仍走原来的短距移动；大于 8 格且不超过冻结的归位上限时，同一次服务端调用里做有界通道检查并一次跳到站位。水幻形和一次响应身体不会被选中。

平衡声明现在仍是选择半径 8、归位上限 4。在整合作者放宽这两个声明之前，冻结值不会变成 24/32，远距分支不会被选中。本单没有编译，也没有进游戏。

<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done","summary":"分身核心已按实际位移分流：不超过8格仍走短距，大于8格且不超过冻结moveLimit时在同一次tryReserve里完成REMOTE_RECALL有界检查并瞬时跳变，成功后预约直接到达。公共端口够用，没有改I的文件。CooperativeContent仍是8/4，声明放宽前远距不会被冻结值选中。斩光未动。不是全功能完成。","changedFiles":["src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/core/CloneBodySweep.java","src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/core/CloneEntity.java","src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/core/CloneRemoteRecall.java","src/main/java/com/bslx/XueXiMod/shu/technique/release/clone/core/CloneServerPorts.java","docs/开发记录/协同回旋斩击-20261103/clone-range-20261104-delivery.md"],"checks":["tools/check-name-gate.ps1 退出码 0"],"notRun":["未编译","未游戏验证","未正式实现复审","未改平衡声明","未改世界存档","未迁制作表"],"writesSettled":true,"blocker":null,"nextAction":"I再放宽selectionRadius与moveLimit声明；执行族不要再调用RemoteRecall。本单未编译。"}
<<<END_CURSOR_TASK_REPORT>>>

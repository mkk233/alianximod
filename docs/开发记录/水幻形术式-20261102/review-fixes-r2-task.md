# 水幻形返修集中只读复核：实现第2/3轮

固定Cursor CLI `--model grok-4.7-high-fast`，ask只读，不改任何文件、不Gradle/游戏/打包/新测试、不spawn。不复审旧批次，不另开设计或按作者拆轮。你之前已完成本批第1轮，现只验证其3个问题的修复及直接影响。主会话派发代表A/C/D返修最终反馈齐全且全部停写。

读取本批 review-nonvisual-r1-final.md、fix-r1-a/d/c-report.md 与3份fix-r1-*-task.md。按实际修改核对：

1. VOLUME通知已经是体积中心，客户端不能再加depth/2；MAINTAIN/SCATTER脚底锚点及纯渲染几何无误改。
2. CloneService.findPresentationOwner(server,deploymentId)从正确session有效部署查到在线owner，不依赖已移除身体；SCATTER/VOLUME同步发包时数据确实还在，死亡/登出/换维无旧owner泄漏。D relay补发同维owner不受160格限制，并且附近owner/每token+phase只收到一次，不扩大为全服广播、不改S2C线格式。
3. 捕获部署节流只保留当前服务器会话/当前tick成功记录，仍能拒绝同tick第二次成功；不同server相同tick数字不误拒；停服清空、登出移除，生命周期挂接有效且不加载客户端类；失败部署不占成功记录，普通链不受影响，不留历史owner无限积累。

只读相关具体修改与必要上下游，不再次全仓审查，不将尚未确认的Steve v2资源/正式注册/专项声明算缺陷。此阶段仍非视觉接线。最终PASS/FAIL、每个前轮问题是否已修复、真实新引入问题（若有）精确路径/行号/触发/最小修法和作者。未编译未游戏如实写，不宣称完整术式交付。最终一次反馈后停止。

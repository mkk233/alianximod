# 当前阶段：只读独立集中审查
模型 Cursor CLI --model grok-4.7-high-fast。你与作者独立。遵守 AGENTS.md / max-dispatch，不得换模型。本阶段实现任务已收束，只有本水牢一项，本次是集中复核。只读、不修改任何文件、不编译、不游戏、不spawn。
先读本目录 implement.md、design-consolidate-report.md、implementation-report.md，再审 PrisonShellRenderer.before.java.txt 与当前 src/main/java/com/bslx/XueXiMod/client/PrisonShellRenderer.java 的差异。可以定向读 StillSheetMesh 和 PrisonClientState 及必要客户端API来源，不能通读全仓。运行PNG路径 src/main/resources/assets/xueximod/textures/entity/xueximod/water_prison_billboard.png，应与正式归档同名图 SHA256 8ba44798140ec92fc45b826549cec03cf629bcb7bcd1d34dab750462101e5096、96x1536一致。
逐项核对实现与批准设计：每牢只有一个正方形面片，不拉伸原图；相机俯仰也正对，各客户端独立；八角投影/逆变换/相对世界精度是否正确；聚能扩大仍体现；身份UUID验证、死亡/卸载/无实体、当前快照清场；第一人称/近平面有限数稳定；16帧两tick循环UV，颜色和半透明、深度测试只写颜色、前置水纹；无更改服务端/协议/玩法。特别注意现有真实Forge1.20.1 API可见性、shader normal类型、Material共享对象，名称门禁的既有问题不要扩范围。
不要追求无关优化，不设计新玩法或新增测试框架。发现问题以文件行号、触发场景、最小修复建议列出；没问题直接写“未发现阻断接入问题，待统一编译和用户游戏实测”。≤1200字，明确读过的文件/没有做的验证。不要把编译或游戏验收说成已通过。

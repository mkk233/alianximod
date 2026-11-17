# 03b 诊断版编译错误定点返修
固定 Cursor CLI --model grok-4.7-high-fast，原D作者，不spawn，不做新的正式复审。沿用设计2/2、实现3/3。
编译者已经停止且没有改源码。最终报告 compile-game-visual-03-report.md；实际 compileJava 退出1，唯一报错：WaterPhantomVisualDiagnostics.java:11 找不到 net.minecraft.util.Util，诊断class未生成，src前后hash差异0。
请只针对这个确定编译错误，核对本项目1.20.1本地依赖的真实类路径/调用并作最小修正。允许写该诊断Java文件和本批 fix-game-visual-03c-report.md/evidence.json；原03b其它行为保持。不改已有用户/其他会话改动。不新增复审，不擅自去改缺臂外观或宣称修复。
运行现有name-gate，禁止自己gradlew/build/jar/reobf/runClient，停写后交回主会话重新统一编译。报告准确变更、命名检查、缺臂尚未修复、待统一编译、未游戏实测、本阶段额度已用完返修结果未追加复审。

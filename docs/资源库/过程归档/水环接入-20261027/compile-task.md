你是本轮唯一编译执行者Cursor Grok4.7HighFast。当前两位writer已结束且互斥合并，用户要游戏接入、核对设计、编译通过、不打包、游戏由用户测。DeepSeek额度拒绝后的本模型替代已获用户授权。不要重新调度或等待模型批准。
先读取render-report.md、wire-report.md以及同目录最新review报告（如果有），限定本轮文件，查清确切待编译检查。工作根D:/alianximod；任务文件均在docs/资源库/过程归档/水环接入-20261027。
执行 .\gradlew.bat compileJava --offline --no-daemon ，日志保存compile.log。可以再执行compileTestJava/processResources或临时init.gradle JavaExec来运行本次新增GuardSyncWireCheck、GuardSnapshotPoseCheck；不得改build.gradle，临时验证脚本仅过程目录。纯WaterRingRenderCheck已经跑过，无修改不用重复。
如编译报错，最小修正本次WaterRing/GuardSyncPacket/GuardClientLifecycle/GuardStates/ModNetwork文件中的API兼容错误；不要修无关功能，不恢复任何无关改动。若需架构变更或不明问题就报告主控；禁止全仓重构。
协议2实际严格，运行时资源要进processResources输出并比对源hash；任何LinkageError或UNEXECUTED测试都算未执行而非通过，不得仅凭exit0当测试成功。可用现有依赖classpath独立运行，不能启动Minecraft。编译失败必须报告实际失败，已执行/跳过检查分别记录。
禁止 build、jar、reobf、runClient、runServer、打包、commit、reset。git一律-c safe.directory=D:/alianximod。最多必要一次编译修正后重跑；有新错误及时说明。完成写integration-validation.md，列修改文件、命令/结果、编译日志、测试实际断言数、未游戏实测。编译及定向验证报告完成后，继续执行同目录finalize-archive-task.md，正式归档文档只有你一个writer。

已解决环境：执行编译/测试的单一进程显式$env:GRADLE_USER_HOME='C:/Users/baishang/.gradle'。已有8.8和Foojay缓存可离线，compile-cache.log为成功证据。Windows PowerShell脚本含中文必须UTF-8 BOM或用Get-Content -Raw -Encoding UTF8后scriptblock执行，避免ANSI乱码。保存真正LASTEXITCODE，不用cmd展开的%ERRORLEVEL%。最新渲染Grok独立审核通过（review-render-report.md）；同步两项复审由另一只读代理同时进行。无其他writer，请直接编译和测试，不重新审已完成渲染/原资源，不跑无关命名门禁。若测试需要完整游戏引导不要无限扩展环境；明确已运行断言数和UNEXECUTED边界即可。

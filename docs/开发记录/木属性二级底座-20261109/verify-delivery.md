# 最终编译与必要定向验证

只有主会话明确确认全部相关源码写入收束、带入最终回执后才启动。固定Cursor grok-4.7-high-fast，不派子代理，不改源码。本单只执行已准备的检查和项目要求的编译，不是新一轮正确性复审，也不是独立测试阶段。

本次启动确认：A最终a202611090913301145ceb，B最终a20261109100522753b9cf，C最终a202611091021582225293，均reported_done/valid/writesSettled=true。C最后仅适配测试与说明，生产代码在第二轮只读复核a202611091015413207913得到静态通过。主会话确认当前所有作者均已结束，现正式授权统一编译与本提示的必要检查。不得再启动其他源文件作者。

读取TaskFile、资源作者交付.md、通用效果作者交付.md、状态入口作者交付.md、整合交付.md与最新返修记录（若有），确定检查命令和所需输出目录。若工具脚本需要额外临时目录，先报告或在build/wood-base-checks/下执行；不要删除任何范围外文件。只读查看脚本以确认实际命令与依赖，不重新审查业务代码。

确认没有其他Gradle正在运行；在D:/alianximod严格执行：
`./gradlew.bat compileJava --offline --no-daemon`

执行资源和效果作者已准备的定向检查脚本，以及实际存在且必要的SELF_STATUS/整合检查。包含C实际DamageEventSlots嵌套回归，以及本次协议升3影响的既有GuardSyncWireCheck（命令由最终整合交付给出）。顺序执行，记录原命令、退出码、检查数、错误信息。不把静态查看当测试，不自己新写测试来假充验证。可运行现有命名/资源JSON门禁，如果与本次改动相关。禁止build、jar、reobf、runClient、启动游戏或打包。

本次还维护了受重构直接影响的既有FireHurtBytecodeCheck、KnownLimitsCheck、FireVisualLayoutCheck，必须按C最终交付命令运行这些相关回归；不要执行全仓无关测试。如果某项有已知外部依赖导致UNEXECUTED，逐项明确，不混写成全部通过。先compileJava刷新主类，再运行读取build/classes/java/main的Guard协议项等检查。

如果编译或检查失败，交回准确文件/行号/错误与作者归属；本单不修源码、不先修再编译，保留输出。若环境阻塞，明确已经跑了什么、未跑什么。

结果写本目录编译与检查结果.md，并最终报告writesSettled=true、实际完成/阻塞和未游戏验证。最终编译必须针对已经收束的最终源码；后续源码若有修复，本单应Resume补跑相关检查和compileJava，不引用旧通过结果。

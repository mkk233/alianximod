# 最后两项返修后的编译与既定定向检查

继续编译会话4422b509-65f6-42c4-9a51-788a0978a6ba，模型Cursor CLI --model grok-4.7-high-fast。主会话仅在repair-final作者最终交回、全部写入收束后派本单；收到本单表示当前源码已冻结。

实现正式复审额度已用完2/2，本单只执行已授权编译及本次必要定向检查，不启动第三轮复审、不扩展代码审查、不spawn、不改源码/资源/构建配置。

读取repair-final-final.md确认最后接口和回归入口。新增ElementHurtLifecycleCheck、FireHurtBytecodeCheck；作者报告其命令执行被拒绝，因此没有运行结果，绝不能沿用作者自报“已核对”的前序叙述。它准备了build/run-fire-hurt-check.ps1，可仅为安全执行检查阅读其命令并提取必要步骤，不盲执行未知脚本，不做额外代码复审。环境继续固定JAVA_HOME=C:/Program Files/Java/jdk-17，GRADLE_USER_HOME=C:/Users/baishang/.gradle，离线无下载。输出全部放verification/postfix/，保留以往日志。

本单执行编译、javac和这些必要检查已有授权。项目max-dispatch明确：“已授权的Cursor返修、实现、编译和必要定向检查可以完成”。这是该例外范围内执行命令，不是开启第三轮人工复审。如执行入口仍被自动审批拒绝，请准确报告被拒命令与系统给出的理由，不虚称测试运行失败，也不擅自绕过审批。

1. 在D:/alianximod执行 .\gradlew.bat compileJava --offline --no-daemon，必须对最后保存的源码，不沿用上一轮通过结果。
2. compileTestJava已经由上轮证实被未修改的SkinAttachmentChecks旧错误阻塞，本单不用为重复同一失败再跑全量任务；明确保留这一限制。
3. compileJava成功后，以这次Gradle main classes和已有test runtimeClasspath编译本批检查源码（原10入口加repair-final报告新增入口）到新的独立check-classes，并实际运行。若作者最后使用本地字节码定向检查，按其实际入口运行；这是验证修复目标所需断言，不进行新一轮人工代码复审。
4. 不用上一轮或作者临时目录的旧main/check classes。若编译失败，只报告精确错误，不自行修复；可以完成现有独立纯检查，但不宣称完整编译通过。

最终报告每命令退出码、每个本次入口断言数/失败数，清楚区分纯逻辑、源码结构、实际本地字节码检查与游戏织入；不要把过去508/583等断言累加。明确未打包、未启动游戏，且“返修结果未追加复审”。禁止build/jar/reobf/runClient/GameTestServer。

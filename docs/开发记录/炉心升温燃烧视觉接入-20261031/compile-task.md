# 最终统一编译（仅在调度者派发本文件后执行）
固定 Cursor CLI --model grok-4.7-high-fast，恢复原作者会话。本文件仅准备；只有主会话确认相关写入收束并启动本任务才获执行编译授权。用户要求检查全部由Cursor负责，Codex不复核。

接入源码已停写。集中检查针对第一人称API的原 NEEDS_FIX 已被同一检查者用实际Forge1.20.1-47.3.33源码证据撤回，见 cursor-check-followup-report.md。不要重复做代码审查。此阶段禁止源码/贴图/Gradle配置/制作表改动，只运行编译并记录报告。

启动后先做最低限度进程/最终状态确认：如果仍有本工作区其他源码作者或Gradle编译进程，立即返回 BLOCKED 不强行并发，不杀进程，不暂停其他任务或游戏。已有 runClient 游戏窗口不能擅自停止；区分正在运行的Gradle任务与闲置Daemon。所有检查只为编译协调，不读其他会话思考/流式输出/中间代码。

在 D:/alianximod 执行唯一编译命令 .\gradlew.bat compileJava --offline --no-daemon 。不跑 build/jar/reobf/runClient、不打包、不游戏。保留 org.gradle.jvmargs=-Xmx3G，不顺手改配置或清缓存。编译前保存本工作区src/main/java与本任务4源码/18贴图的hash清单，编译后核对没有源码写入，避免把变动中的编译当最终通过。保存完整编译输出与exit code。若出现源码变化，此轮结果标INVALIDATED，需要写入收束后重编，不能说通过。编译失败只报告错误文件行号和归属，不边改边编；本任务的错误交回作者，其它批范围外错误保留现场交接，不抢写。

最终 compile-report.md/json 写明成功/失败/阻塞、实际命令退出码、日志路径、源hash是否稳定、是否存在残留缺口、明确游戏未测未打包，停止所有写入。若编译通过，给唯一制作表writer可直接使用的简短交付结论。本轮不写xlsx、不迁状态；写回者稍后负责待验收迁移。

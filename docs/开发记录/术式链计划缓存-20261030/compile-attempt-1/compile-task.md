# 术式链缓存：最终统一编译

本文件仅在主会话确认相关作者全部最终返回、所有源码写入收束、编译清单冻结并正式派发后生效。固定Cursor CLI --model grok-4.7-high-fast，你是指定的唯一编译执行者，不再委托代理。

只读本目录记录.md、最新实现/返修摘要、compile-ownership.json、compile-source-freeze.json。按清单确认全部Java源码路径/hash/长度/最后写入时间仍一致，其他同工作区无Gradle正在执行。ISO时间须按原字符串或统一UTC ticks比对，避免PowerShell自动DateTime格式化误判。常驻idle daemon不是构建占用；若有真实busy任务，先只读明确占用并返回，不擅自取消任务，不启动重叠Gradle。

必须在实际运行Gradle的PowerShell进程内显式设置已验证环境，而非只依赖外层：
`$env:JAVA_HOME='C:\Program Files\Java\jdk-17'`
`$env:GRADLE_USER_HOME='C:\Users\baishang\.gradle'`
`$env:PATH=$env:JAVA_HOME+'\bin;'+$env:PATH`
Cursor默认命令环境可能指向空沙箱Gradle缓存，会错误报告缺foojay插件；不得使用空缓存、不得下载wrapper或依赖，不改全局环境/HOME/USERPROFILE/CODEX_HOME/Gradle配置。

在D:/alianximod仅执行：
`.\gradlew.bat compileJava --offline --no-daemon`

沿用-Xmx3G与daemon关闭。采用稳定stdout/stderr文件重定向，不用上次导致PowerShell采集崩溃的流式管道。记录环境、开始结束UTC、真实退出码和完整输出到compile.log。结束后再核对全部冻结Java文件hash/长度/时间不变，编译开始晚于最后一次源码写入；只有退出码0并且源码未变才能确认最终版本通过。

不做新的正确性复审，不跑独立测试，不改任何源码/配置/制作表，不打包/jar/reobf/runClient/启动游戏/提交。编译失败返回真实文件行号诊断交回原作者，不边编边改。写本目录compile-results.json和compile-final.md，明确最终源码覆盖、未运行验证和所有权释放。已有同名失败报告需先以唯一尝试名称留档，不能丢掉历史诊断。完整最终返回，主会话不实时跟踪工作内容。

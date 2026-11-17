# 使用已验证的真实Gradle主目录完成最终编译

继续原Cursor编译会话，固定 --model grok-4.7-high-fast。仍是统一编译，不新增代码复审，不改Java/Gradle配置/制作表，不打包、不启动游戏、不下载wrapper或依赖。

主会话已读到直接先例：D:/alianximod/docs/开发记录/领域附魔底座实施-20261029/compile-run-final.md 第18行记录相同的foojay 0.7.0离线错误，原因是GRADLE_USER_HOME被Cursor命令环境指到空沙箱缓存；该插件实际存在C:/Users/baishang/.gradle，显式指定真实目录后同命令退出码0。近期火种共享燃料改造、火系六式、火属性二级底座的成功编译都显式使用下述同一环境。请读取上述单份历史compile-run-final.md确认，不再重复通读其他代码/报告。

在实际调用Gradle的那个PowerShell进程内显式设置：
`$env:JAVA_HOME='C:\Program Files\Java\jdk-17'`
`$env:GRADLE_USER_HOME='C:\Users\baishang\.gradle'`
`$env:PATH=$env:JAVA_HOME+'\bin;'+$env:PATH`
这只是本次命令进程环境，不改全局配置，不修改HOME/USERPROFILE/CODEX_HOME，不使用空隔离缓存。运行前在日志打印这两个非秘密路径和java版本，证明实际环境。不要以外层进程默认值代替内层设置。

把当前失败的compile.log、compile-final.md、compile-results.json另存为compile-empty-home-failed.log、compile-empty-home-failed-final.md、compile-empty-home-failed-results.json，存在则不覆盖。此前挂起IDE同步已经解除，不再终止其他进程；只快速核对同项目无新的Gradle构建，闲置52180不阻塞。按compile-source-freeze.json核对660文件仍不变，所有作者已释放。

在D:/alianximod运行唯一规定命令：
`.\gradlew.bat compileJava --offline --no-daemon`

用已知稳定的stdout/stderr文件重定向捕获方式，避免之前导致采集PowerShell崩溃的管道。记录真实退出码、开始/结束UTC、完整日志。结束再次核对源码hash/长度/LastWriteUTC，确认编译晚于最终源码写入。退出码0且冻结一致才能报告通过。若进入Java后有编译诊断，返回具体错误由原作者改，不能自行修。若真实主目录仍缺缓存，保留实际环境证据返回，不下载或改插件版本。

更新compile.log、compile-results.json、compile-final.md，保留前次失败引用，明确本次实际环境、最终源码覆盖、未打包/未游戏或性能实测。完整最终返回并释放编译占用。

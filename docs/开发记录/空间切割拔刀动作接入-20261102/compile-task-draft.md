# 空间切割 v5 最终编译（仅编译，无源码修改授权）

本文件是编译任务草案，只有主会话通过 Cursor 启动你并指向本文件才生效。使用 Grok 4.7 High Fast；最终源码及检查已收束后执行。

## 开始前必需证据
1. 读本任务 author 最终报告/返修报告、implementation-ownership.json、implementation-evidence.json、独立复核最终报告，确认作者停止源码写入。读取调度续跑.json 中真实阶段；若仍返修/复核中则返回未编译。
2. 读取 docs/开发记录/分身底座-20261102 的当前编排状态/所有权与最终报告，确认其对真实 src 的写入已停止，没有其他相关写入作者正在活动。只读最小状态，不能审查其工作内容或改变其文件。若仍有写入则说明阻塞返回，不编译。
3. 检查 GradleWrapperMain/Gradle worker/runClient 是否活跃。空闲的 Gradle daemon 本身不算阻塞，勿终止其他任务或要求用户关闭不存在的游戏。共用工作区只允许一条 Gradle。确认命名门禁完成。
4. 汇总本次最终所有已改源码和运行json哈希/mtime，并保留整个 src 的前后哈希清单以证明编译对应稳定源码。

## 唯一 Gradle 命令
在 D:/alianximod 执行 .\gradlew.bat compileJava --offline --no-daemon 。保持 org.gradle.jvmargs=-Xmx3G 配置，禁止 build/jar/reobf/runClient、打包、发布、启动游戏。禁止这个阶段改任何Java/运行资源/构建配置；失败时只记录原始错误并返回，另行授权返修。

完成保存 compile-report.md、compile-evidence.json、compile-gradle.log，包含时间、命令、退出码、真实 task outcome（执行还是 UP-TO-DATE）、前后源码哈希比较、并行写入/Gradle前置证据。无游戏验证应明确。只写本任务日志报告，不动其他任务记录、制作表、正式资源README/manifest/索引。
最终返回通过/未通过/环境阻塞和准确证据，停止所有活动。不隐瞒失败或拿中间编译替代最终编译。

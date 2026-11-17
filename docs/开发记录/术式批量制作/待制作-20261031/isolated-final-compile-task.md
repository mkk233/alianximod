# 隔离项目副本：同一最终源码的必要编译

固定Cursor CLI --model grok-4.7-high-fast，继续唯一编译执行者68ccd641-cc64-4259-8e72-633942ec39a7，WriteMode。设计2/2、实现3/3已耗尽，不追加任何代码正确性复审，不实现/改Java，不spawn代理。本单是已授权最终编译的可逆环境隔离，遵守同一工作区Gradle串行：不在用户运行中的D:/alianximod项目启动第二个Gradle，而将同一收束输入复制到一个独立临时项目根编译；不会启动第二个游戏。

前置已知事实：原客户端45212已按用户回复退出；compile-after-client-close报告实际compileJava失败11条，A/B已仅修3文件并最终停写。recompile-after-diagnostics报告证实仅这3项预期变化、没有其他源变化，但用户在12:52启动新Gradle52040/客户端16920，原项目当前不能再跑Gradle。该报告保留，不把它说成成功。不要结束用户的游戏、IDEA或Gradle进程，不要求用户再次关闭。

写权限：本批docs下编译证据/报告；系统当前用户Temp目录下新建唯一 `xueximod-final-compile-20261031-<唯一后缀>` 目录及其副本编译输出。源D:/alianximod的源码、配置、资源和原build仅允许读取，不改。只允许用同一原生PowerShell处理路径，创建前记录并校验解析后的绝对目标确在用户Temp父目录中，必须是此前不存在的唯一目录，不覆盖其他目录。不做任何递归删除/移动，不复制.git、.idea、.gradle、build、run、游戏存档或无关用户目录。

1. 读取自己的两个实际编译报告与A/B最终compile-fix报告，只机械核对文件版本/环境，不审代码。对原项目src/main和src/generated/resources逐文件路径/字节/hash留当前稳定快照，确认A/B三处hash一致、无未说明变化/活动写入。未知变化或仍在写入则报告，不能当作最终版本。
2. 明确实际构建输入：gradlew.bat/gradlew、gradle包装器及被引用的Gradle脚本、settings/build文件、gradle.properties、src树、构建明确引用的本地libs/配置等。只读构建脚本确定本地输入与输出位置属于必要构建环境检查，不展开源码逻辑审查。逐字节复制到临时项目根，保留相对目录。不修改任何构建配置以绕过失败，不换Gradle/Forge/JDK、不联网补依赖、不复制活跃缓存。若存在会把构建输出写回原项目的绝对buildDir/任务，不能执行隔离编译，给明确阻塞，不擅自改配置。
3. 编译前证明原项目与副本的所有Java、运行资源、generated资源和构建配置完全同hash；记录复制清单、相对路径映射、原/副本projectRoot、校验时间。不能只copy几份报错Java后宣称整仓输入相同。
4. 在隔离副本自己的项目根、以该副本gradlew.bat执行 `compileJava --offline --no-daemon`，当前命令进程JAVA_HOME='C:/Program Files/Java/jdk-17'、GRADLE_USER_HOME='C:/Users/baishang/.gradle'，现有-Xmx3G/daemon=false配置原样。Gradle项目缓存与build输出必须在副本，不向原build写入；共享已有Gradle用户依赖缓存允许由Gradle正常加锁使用，不强行解锁/杀进程。若真实缓存锁阻塞则报告，不覆盖缓存。不得build/jar/reobf/datagen/runClient/游戏/测试/发布/提交。命令与cwd真实记录，不能写成在D:/alianximod原根编译。
5. 明确编译输出覆盖完整主源集。命名门禁若必要可在副本运行，检查输出也留在副本；原图片/JSON未变，引用此前机械检查即可。编译后再次对原项目与副本源码/配置/实际运行资源做逐文件一致性和前后稳定检查。原项目源或配置如果期间变了，隔离结果不再称覆盖当前最终版本。原游戏的build/run变化不当源码变更，但绝不能声称自己编译更新了原build。
6. 失败给实际编译器诊断与A/B/C/D归属，不改源码/不边改边编。成功给退出码0、完整日志、输入一致/原源码前后稳定证据，清楚说明只在临时独立项目编译，未打包未更新当前运行客户端，用户游戏实测尚待进行。

输出isolated-final-compile-report.md/json、独立日志与原/副本输入清单/前后校验。保留全部此前失败/未执行报告。记临时目录实际路径以便追溯，不擅自清理。本执行者最后停止，不迁表、不自行新增审查。若隔离无法满足相同输入与不写原build的要求，保留现场如实报告外部阻塞，不改为原工作区并行编译。

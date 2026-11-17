# 唯一追加复核返修后的最终串行构建与交付记录

固定 Cursor CLI --model grok-4.7-high-fast，续用唯一编译 Agent 3b55bc07-f087-481d-88a4-ada8f6d3b0af。只有主会话收到 repair-review-A-final.md、repair-review-B-final.md 的最终反馈并确认 released=true，更新两份冻结清单和所有权后正式派发才生效。不是代码复核或独立测试。设计2轮；实现原3轮+用户明确追加1轮=累计4轮，额度全部用完，修复后不得再复审，也不得自行请代理。

先读本目录记录.md、上述两份作者最终报告、最新 compile-ownership.json、native-compile-ownership.json、compile-source-freeze.json、native-source-freeze.json、native-current-result.json。旧首次失败和第二次成功均保留：Java第二次成功已归档java-attempt-2/，native第二次成功保留native-attempt-2-*与native-source-freeze-attempt2.json，不覆盖历史。

只允许构建产物、本任务构建日志/结果/最终报告/结果指针，以及 docs/纯计算后端使用说明.md 和 native/compute/README.md 验证状态的写入。不改Java/C/ABI/构建脚本/Gradle配置/制作表，不直接作新正确性判断，不边编边改。遇实际错误交原作者。工具审批拒绝保留原始reason并停止对应动作，不绕过、不加force/改安全设置/换模型或会话。

先机械核对冻结文件集合、长度、UTC ticks完全一致（无hash），确认无其他活跃native构建和Gradle；idle daemon不算忙任务。源作者已释放且没有新写入才能开始。输入改变就停止交回，不擅自重建快照来掩盖变化。

第一步：完整执行官方Windows脚本，显式 -JdkHome 'C:\Program Files\Java\jdk-17'，工作区D:\alianximod，沿用 native/compute/build。不要-Clean、不删除缓存、不安装下载、不移动DLL到游戏。不启动JNI加载或数值测试。保留真实命令、环境、UTC起止、退出码、stdout/stderr到 native-post-review.log 及对应独立文件；结果 native-post-review-results.json、native-post-review-final.md，机械dumpbin输出 native-post-review-dumpbin.txt。

与首次编译不同，本次是Java加载/live边界及Windows构建脚本的返修。按A/B最终报告若C源、JNI声明、CMake编译选项未变且既有DLL属于native-attempt-2成功构建，Ninja显示无工作且DLL保持原时间是有效增量结果，允许复用；无需强制clean/改时间/重编以制造新DLL。必须完整脚本本次退出0、冻结前后一致，并如实区分本次脚本通过与08:40:59Z原DLL实际编译。若C源或ABI相关输入实际改变，则依赖构建系统正常重编并记录产物，不复用失配产物。dumpbin只核实x64/7个JNI导出，不执行DLL。当前工程和JDK路径均ASCII，不声称已真实在中文路径构建；B的编码返修报告不能冒充该运行验证。更新native-current-result.json指向本次结果并保留原始DLL来源。

第二步：native步骤通过并释放后，在同一实际PowerShell构建进程设置JAVA_HOME='C:\Program Files\Java\jdk-17'、GRADLE_USER_HOME='C:\Users\baishang\.gradle'、PATH前缀JDK bin，保持-Xmx3G、daemon=false。项目根执行唯一命令 .\gradlew.bat compileJava --offline --no-daemon。禁止build/jar/reobf/runClient/test/game/运行实验。稳定重定向stdout/stderr，不用流式管道。先保存上一成功阶段残留的同名辅助stdout/stderr/时间文件到java-attempt-2/中未占用的路径，历史不覆盖。最新结果写根compile.log、compile-results.json、compile-final.md，必要失败诊断使用post-review独立名称。保留真实命令/时间/退出码/是否实际编译/诊断。对完整冻结集合做前后元数据比较；退出0且输入未变才记最终版通过。

第三步：按已返回事实同步两份使用文档。用户追加的1轮复核已经完成，累计实现4轮；报告发现3项P2（R1非法库路径回退、R2 VORTEX停驻空origin兼容、R3 Windows临时命令Unicode编码），原作者已报告修复，修复后未再次复审。不得继续写“未追加复审”冒充从未执行该轮。写实际最后Java/C构建和Windows x64产物；保持未JNI加载、未数值对照、未游戏与性能实测、未中文路径真实构建、非Windows未验证、未模组打包。Math/libm不保证逐位相同，单条JNI不保证加速，DLL在调用线程运行，不承诺TPS收益。接口功能只按作者最终报告，禁止新增推断或改协议。

最终汇总写 final-post-review-build-final.md，并在CLI最终返回实际构建结果/文档改动/所有日志与产物位置、剩余限制、released=true。源码与Gradle槽均释放。只要出现真实阻塞，准确交回而不谎称完成。

# 步骤4/5最终Java统一编译与文档收尾

主会话确认全部相关Java作者（含共享多重释放任务）已最终返回并释放，保存compile-ownership.json和compile-source-freeze.json后，正式派发才生效。固定Cursor CLI --model grok-4.7-high-fast，唯一Gradle执行者。设计2/2、实现3/3已用完，不新增代码正确性复审/独立测试，不读源码再作判断。只做已授权构建与结果记录。

native构建存在历史失败尝试。若本目录native-current-result.json存在，以其中reportPath/resultsPath指向的最新实际结果为准；native-compile-final.md是第一次失败的历史，不覆盖、不误当最后状态。本任务开始前必须读取最新指针与报告。

先读本目录记录.md、implementation-A-final.md、implementation-B-final.md、native-compile-final.md（如已存在）、compile-ownership.json、compile-source-freeze.json，以及作者最终未完成清单。当前作者均已释放，你只可更新最终结果记录和docs/纯计算后端使用说明.md、native/compute/README.md的验证状态。不得改Java/C/构建配置、制作表或其他源码。

核对全部main Java、本次相关resources、native源码/脚本的路径/长度/UTC写入刻度与冻结清单一致，开始时间晚于最后写入；只做元数据，不补做早前被拒绝的备份/哈希、不声称独立SHA256检查。确认没有活跃GradleWrapperMain/Gradle构建；idle daemon不算忙任务。其他会话仍声明写入或输入已变则停止并报告，不停其他作者/进程，不编中间版本。

在实际执行命令的同一PowerShell进程中显式设置：JAVA_HOME='C:\Program Files\Java\jdk-17'，GRADLE_USER_HOME='C:\Users\baishang\.gradle'，PATH前缀JDK bin。不能沿用Cursor默认空沙箱缓存然后下载Foojay/Gradle。保留-Xmx3G和daemon=false，不改HOME/USERPROFILE/CODEX_HOME或全局配置。

项目根唯一构建命令：.\gradlew.bat compileJava --offline --no-daemon。稳定stdout/stderr重定向，不使用曾崩溃的流式管道。记录实际命令/环境、开始结束UTC、退出码、compileJava是否实际执行还是UP-TO-DATE及完整诊断。结束再核对冻结集合/长度/UTC刻度。退出0且最终输入未改才记本版通过。失败只返回准确诊断和路径/行号给原作者，不边编边改、不自行重审。

任何审批拒绝都保留原始reason并停止相应动作；不取消auto-review、不加force、不换工具/会话绕过、不让主会话代跑。不得build/jar/reobf/runClient/test/游戏/性能实验，不运行native加载测试，不自动开启或拷DLL到游戏目录。本任务若native还未编译，应分开记录，不能只凭Java通过宣称DLL可用。

只写本目录compile.log、compile-results.json、compile-final.md及必要独立失败诊断，旧尝试保留不覆盖。只有真实结果明确后才同步两份使用文档的验证小节：实际Java和C编译/产物与平台、是否采用其他会话统一编译、尚未JNI加载/数值对照/游戏/性能测试；未追加复审，不声称TPS收益。更新说明中的待编译占位，不改接口契约或新增未经验证的功能宣称。

说明补充：作者A正在按complete-live-native-task.md完成现有标量调用的显式native分发，C作者读取的ABI暂时冻结。两者最终释放后，本单还可按implementation-A-live-native-final.md仅同步abi-contract.md末尾“游戏侧已经接上的入口”的调用状态（旧文称始终Java），以及纯计算说明内相同过时状态；不得改ABI版本、native声明、字段/stride、数学公式或错误码。此项是作者已交回行为的记录同步，不追加代码正确性审查。

最终反馈实际结果、文档是否同步、未完成条件和released=true。编译结束后释放Gradle和文档写入权。

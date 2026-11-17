# 步骤4/5作者B：离线C构建基础

固定Cursor CLI --model grok-4.7-high-fast。本阶段为实现准备，原read-native-env会话resume后仍是Ask模式，prepare-native-final.md确认未创建任何文件且执行已结束，因此由新的默认Agent实现会话承接同一作者B的所有权和完整最终事实报告。设计2/2、实现3/3已用完，不另开代理/复审/独立测试。读本目录记录.md和read-native-env-final.md。

仅你可写D:/alianximod/native/compute/（当前新目录）和本目录native-build-prep-final.md、evidence-B-prep.json。A正在独占shu/compute Java接口与桥，以及4个纯数值适配类和总用户说明；你不得改Java/Gradle/shared配置/制作表/总说明。不得读取A正在写的中间代码或ABI；等A最终交回后同一会话续接C内核。

本单先完成可复用的离线C构建脚本与native README。产物库基名固定xueximod_compute；Windows输出xueximod_compute.dll。预留C源native/compute/src/xueximod_compute.c，由你后续依据A的冻结ABI写入，现在不杜撰函数签名，不提交空实现冒充完成。

建立CMakeLists.txt和Windows可用的构建脚本，使用现有JDK17 JNI头与MSVC x64/vcvars64，兼顾标准C编译器在Linux/macOS的构建入口（没有现成平台不宣称验证）。独立构建目录native/compute/build或out，有目录内.gitignore保留源文件排除生成物。默认不拷DLL进资源目录/游戏目录、不改全局PATH/JAVA_HOME、不接Gradle自动打包、不装依赖。允许命令参数覆盖JDK/VS路径；优先可发现vswhere和用户提供工具路径，不把本机个人目录写死为唯一入口。本机准确工具位置见你的最终事实报告。

维护浮点语义，禁止fast-math/不受控FMA重排；C与Java17的参考计算顺序要能遵循。显式x64构建匹配本机64位JVM；错误路径应给清楚日志与退出码，构建脚本不得下载依赖、自动删除不受控路径或覆盖现有游戏库。JNI include只需要JDK头，不错误要求安装JVM链接库。C标准/编译参数和产物路径记录在README。

只编写脚本，不执行CMake配置/编译、javac/Gradle/加载验证，不生成测试。没有C实现时文档明确待ABI交接，最终构建者之后会获得单独授权。首次准备结束立即最终返回并释放写入权，主会话收到A稳定ABI后再派你接C源码。保留所有未提交改动，不git reset/clean/checkout，不提交。

审批拒绝时记录原始reason，不降模型、不加force、不改安全设置，不换工具绕过。最终报告准确文件、脚本入口、仍缺C源/ABI、平台未验证，附released=true和路径/时间元数据即可，无需hash。

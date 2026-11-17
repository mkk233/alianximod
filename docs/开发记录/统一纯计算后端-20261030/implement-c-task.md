# 步骤4/5作者B：按已交回ABI实现C后端

只有主会话收到作者A的Java实施最终反馈和你的构建准备最终反馈、确认各自写入已释放后，正式派发本任务才生效。固定Cursor CLI --model grok-4.7-high-fast，继续默认Agent的作者B会话，不resume旧Ask事实会话。设计2/2、实现3/3已用完；本单是用户已授权的C实现，不是复审，不开其他代理/设计审查/独立测试。

先读本目录记录.md、abi-contract.md、implementation-A-final.md、native-build-prep-final.md、read-native-env-final.md。只读已经最终交回的shu/compute纯Java接口/参考后端/桥声明以实现冻结协议，不通读模组/其他活跃源码，不判断前几步或外部多重释放任务正确性。

你独占写入native/compute/，以及本目录implementation-B-final.md、evidence-B.json、backup-B/（自己既有文件变更前）。不改任何Java/Gradle/配置/制作表/总用户说明/ABI契约，A独占的文件仍不可直接修。如果契约存在无法按原语义实现的具体障碍，返回准确字段/公式/能力缺口，由原作者处理，不能静默改协议或玩法。

实现真实标准C数值内核和JNI桥，产物基名xueximod_compute。遵循abi-contract的实际Java类/native方法/符号、版本与能力握手、操作号、输入输出布局、阈值、运算顺序、非有限值和错误策略。不允许TODO、永远返回“未实现”、挂名C后端或与参考Java不一致的近似替代；对已约定必须Java回退的能力按契约返回清晰状态。不要把Level/Entity/JVM对象业务/随机数/文件IO/网络/线程调度带到C。

所有JNI入口在访问内存前验证null、长度、count、offset、stride、乘加溢出、输出容量、重叠/别名规则和操作号；错误返回必须与Java侧约定一致，不能让半批结果被当作成功。释放全部JNI引用/缓冲，不长期保存JNIEnv/jarray/指针，不在临界区做可能阻塞操作。OOM/挂起Java异常按JNI规则返回，不能清除后伪装成功。不声明能从任意native内存错误或崩溃中自动恢复。

使用已在盘上的JDK17头和MSVC x64、CMake；维护严格浮点选项，不启用fast-math/不受控FMA。Java17与C数学库不同的部分必须遵照契约边界处理，不能只凭“误差很小”宣布等价。既有构建脚本需适配最终C源名和JNI符号，Windows最终DLL可离线构建；Linux/macOS源级支持与实测状态分开写明。不要下载依赖、安装工具链、更改全局环境、不拷进游戏目录或自动开启native。

维护native/compute/README.md，写清实际源码/构建入口、JDK/平台/架构、DLL产物位置与未验证项。总用户说明由A维护，本单报告需要它同步的准确字段即可。已有其他任务文件全部保留。

本阶段仍无编译授权：不执行CMake配置/编译、javac/Gradle、JNI加载实验、游戏、测试或打包。等最终写入收束后，主会话指定唯一编译者；最终构建失败只交回原作者，不边编边改。任何审批拒绝都记录原始reason，不取消auto-review/加force/换模型或工具绕过。

最终报告列实际能力、C导出与ABI、改动文件、供最终构建使用的准确命令、平台未验证和必须保留的限制；注明未追加复审、待编译/运行验证。写evidence-B.json记录文件元数据和released=true（无需新hash），完整最终返回后停止写入。

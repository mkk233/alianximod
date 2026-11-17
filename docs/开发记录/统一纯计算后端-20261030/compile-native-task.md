# 步骤4/5最终native编译（派发后才生效）

固定Cursor CLI --model grok-4.7-high-fast，唯一native编译执行者。只有A/B最终交回并释放、主会话保存native-compile-ownership.json与native-source-freeze.json并正式派发后执行。用户明确要求同接口C后端，本单授权生成本地Windows x64 DLL；不是Gradle build/jar或发布。设计2/2、实现3/3已用完，不新增正式复审/独立测试/游戏。

读本目录记录.md、implementation-A-final.md、implementation-B-final.md、abi-contract.md、native-compile-ownership.json、native-source-freeze.json和native/compute/README.md。只获取构建所需最终资料与文件元数据，不再次审查Java/C正确性。确认native/compute的源/构建脚本及shu/compute的JNI声明已收束、冻结路径/长度/UTC ticks一致，native构建目录没有其他活跃编译；不动别的任务，不做源码修改。全仓Java仍可能由多重释放任务独立写入，本单不运行Gradle，native独立构建不依赖那些世界/运行源码。

使用本机已有MSVC x64/JDK17，执行最终README提供的Windows入口，显式-JdkHome 'C:\Program Files\Java\jdk-17'，默认独立native/compute/build。不要-Clean、不删除目录、不切换全局环境，不下载或安装任何东西。若既有构建目录不兼容只返回实际原因，不擅自清理。保存完整stdout/stderr、实际CMake/编译器命令、开始结束UTC、退出码、编译器/JDK/架构、产物绝对路径和长度/写入时间。

编译失败只交回原作者准确诊断，不能边编边改。若构建调用被审批拒绝，记录原始reason并停止，不去掉auto-review、不加force、不换工具/会话或路径绕过、不要求主会话代跑。空Gradle沙箱与本native编译无关，不因此改Gradle。

成功后仅做交付需要的机械产物检查：用现有dumpbin核实DLL是x64和导出ABI要求的JNI符号（不执行DLL，不做Java加载/数值测试）。编译前后冻结元数据一致才记为本版本构建结果。不要补做此前其他任务被拒绝的备份/哈希，不要求SHA256。别把存在旧DLL当这次成功，需本次命令退出0及新产物时间证据。

只写本目录native-compile.log、native-compile-results.json、native-compile-final.md以及所需独立诊断文件，保留历史尝试，不覆盖失败日志。可以在native/compute/build内生成构建产物，不改源码、README/总用户说明、Gradle配置或制作表。最终返回并释放native编译权，明确未JNI运行加载/数值对照/游戏/性能测试、非Windows平台未验证；未追加复审。Java最终compileJava由后续独立授权执行者在共享源码全收束后运行，不在本单偷跑。

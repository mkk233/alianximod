# 作者B：修复真实构建诊断中的指针宽度取值

固定Cursor CLI --model grok-4.7-high-fast，继续原B Agent会话4efe6c01-67f4-4e27-aa32-e80f1c91acda。设计2/2、实现3/3已用完，不新增正式复审；仅处理实际构建失败的机械问题。

读native-compile-final.md、native-compile-diagnosis.md、native-compile-results.json、native-compile.log。编译者已最终返回并释放。实际退出码6，不是审批拒绝：CMake配置/生成成功，编译器x64，CMakeCCompiler.cmake有CMAKE_C_SIZEOF_DATA_PTR="8"，但build-windows.ps1要求CMakeCache.txt有CMAKE_SIZEOF_VOID_P:INTERNAL=8；本机CMake不把该变量缓存，脚本在cmake --build前误报。

独占可写native/compute/build-windows.ps1；如果同一个错误缓存假定也在build-unix.sh，仅同步修正该检查。必要时为稳定导出已由CMake确认的指针宽度事实，可小改native/compute/CMakeLists.txt，但不删除真正的64位/目标架构验证，不修改编译浮点参数、Java/JNI/C数学源码或协议。native/compute/README.md仅同步对应检查/已知失败描述。本目录可写repair-native-build-final.md、evidence-B-buildfix.json、backup-B-buildfix/。不得扩大成新审查或重构。

使用实际生成配置中的事实或CMake已成功验证的明确产物确认指针宽度，不再把“未缓存该变量”误判32位，也不以不存在的字段猜成功。保留原工具发现、现有构建目录、日志/退出码和x64限制。不得-Clean、不删除build、不改全局环境、不直接绕过脚本去编译。

本单不运行CMake/编译/Gradle/加载/测试/游戏，不改制作表，不新增正式复审。仅修脚本，最终交回后由原编译者重跑完整构建。任何审批拒绝记录reason，不换工具绕过。保存自己变更前副本与路径/长度/UTC时间，无需hash，最终报告具体改动和released=true。

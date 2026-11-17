# native最终构建第二次尝试

固定Cursor CLI --model grok-4.7-high-fast，继续原native编译Agent会话3b55bc07-f087-481d-88a4-ada8f6d3b0af。先读本目录compile-native-task.md，完全遵循其中构建范围、禁止源码修改/测试/加载/Gradle、冻结/所有权检查、审批拒绝停止和机械导出检查。不是新复审。

主会话只会在原B按repair-native-build-task.md交回并释放、重新冻结native-source-freeze.json与compile-source-freeze.json、更新native-compile-ownership.json后派发。读repair-native-build-final.md与原失败诊断。首次失败native-compile.log/results/final/diagnosis及原08:26快照已保留，这次不得覆盖它们。

本次唯一输出改为：native-attempt-2.log、native-attempt-2-results.json、native-attempt-2-final.md及需要的native-attempt-2-诊断文件。沿用已生成native/compute/build，无-Clean、不删除缓存。完整重跑官方Windows脚本并显式JDK17；不能手动跳过脚本检查走别的命令以掩盖错误。

记录实际CMake/编译/链接是否执行，真实退出码、架构、7个JNI导出和新DLL时间/长度；输入元数据前后一致才能报告成功。若再失败仍只报告诊断并释放，禁止边编边改。构建日志保留，目录不要移除。最终返回具体输出文件名，主会话将以新的结果指针供Java编译和总文档采用。

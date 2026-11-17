# 步骤4/5：本机native构建与接入条件事实

固定Cursor CLI --model grok-4.7-high-fast，只读信息采集。设计2/2与实现3/3已用完，不作新的设计正确性判断，不开代理/审查/测试，不写任何文件，不运行构建或游戏。

只读D:/alianximod/AGENTS.md、.grok/skills/max-dispatch/SKILL.md、本目录记录.md，根build.gradle/settings.gradle/gradle.properties/gradle wrapper配置，现有配置和命令注册类（先rg按config/ModCommands/XueXiMod定位，仅必要文件），现有native目录若存在。本任务不得阅读network/链/领域/发布运行等另一个任务正在改动的源码。只取现有依赖、语言版本、日志/配置/服务端启动接入口事实。

本地只读查找现有JDK17、jni.h/jni_md.h、Windows x64 C编译器（cl/clang/gcc）和CMake/Ninja/MSBuild的可用性与路径，允许Get-Command、现有工具的版本/帮助输出以及标准安装目录的定向路径检查，不枚举整个磁盘、不安装/下载任何东西。报告编译dll所需工具是否齐备以及可用的准确调用入口。读取普通环境和路径即可，不打印秘密/令牌，不改全局环境。

用户要求同接口Java+C后端，Java默认可用；native缺失/架构不符不应阻断常规Java运行。报告目前项目是否已有JNI/JNA/FFM及native加载模式；Java17自带JNI头文件是否可用；当前OS/arch和现有跨平台构建配置；现有配置风格和合适的独立注册方式事实。若无第三方桥接依赖，明确记录，不为事实定位引入库。允许在后续实施选择JDK17 JNI且可用工具链构建，当前只报告事实。

最终交付：必要Java/config/构建文件列表及是否需占用共享注册器；现有工具链、离线可行性、JDK/Gradle真实路径、可用C动态库产物路径惯例；真实缺口，不承诺已编译或C性能收益。只返回最终文本，外层保存。不要编写或执行测试/冒烟程序，不运行Gradle/native编译，不打包，不改制作表。

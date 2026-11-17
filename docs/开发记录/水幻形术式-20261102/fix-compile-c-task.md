# 水幻形非视觉阶段：仅修本次javac错误
固定Cursor CLI `--model grok-4.7-high-fast`，仅实施明确编译错误修复，不新开设计/实现复审，设计2/2、实现2/3不变。读本批 compile-attempt-1/compile-nonvisual-report.md 和实际日志定位（原日志也仍在主目录）。禁止Gradle、自行重编、打包/游戏、改表/资源/正式登记。相关作者已停写。只写指定文件和自己的报告，保留既有改动，禁止reset/clean/checkout。命名门禁可执行，只修本次文件。最终一次性交回准确变更/未编译/停止写入。

C唯一源码：network/SkillUsePacket.java、shu/technique/release/capture/ActorCaptureDispatcher.java，报告fix-compile-c-report.md。
1. SkillUsePacket.executeSkill约632行captured重名；给本批新增的捕获路由局部变量起不冲突的清晰名称，并只更新其引用，保留旧逻辑和成功/拒绝返回语义。
2. ActorCaptureDispatcher约152行UUID未导入；补上正确java.util.UUID引用，勿替换成字符串或改变session/节流语义。
不扩展装配/捕获设计，不动其余文件、不正式注册或声明平衡键。


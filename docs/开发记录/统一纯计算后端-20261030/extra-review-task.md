# 用户追加的唯一一轮实现复核

## 授权与计数

用户本轮原话：“等协作会话复核完成后你可以二次复核但是只能进行一轮”。这是本次纯计算Java/C实现的额外1轮集中复核授权，原累计实现3轮不重置；本轮一经开始即记实现累计第4轮（原3+追加1）。设计2/2不增加。你是之前唯一的gpt-6-astra performance_design代理，继续该模型，不新增其他代理/判断者，不再派Cursor作另一轮复审。主会话只转达你的最终结论，不重新作第二次判断。发现问题后允许原Cursor作者修复和编译，但没有自动再复审额度。

## 启动条件已经满足

- Java作者A、C作者B及注释/脚本修复均最终返回并释放。当前没有源码写入。
- 协作会话“查看项目当前状态”（01a15fdc-4130-7c92-bece-27530e8c54be）自己的实现3/3复核已完成；随后SkillChainPlan四处实参顺序机械编译修复也已交回并释放。证据：docs/开发记录/多重释放底座-20261030/记录.md末尾。不要把对方的复核当作本次纯计算已经审过，也不要替它再审整个多重释放功能。
- 当前统一Java编译真实通过：compile-final.md/compile-results.json，2026-10-30T08:55:51Z至08:56:15Z，compileJava实际执行、退出0，712项冻结前后相同（703个Java）。DLL也已真实编译成功，native-current-result.json指向native-attempt-2-*，Windows x64、7个JNI导出、26112字节。
- 编译只是编译；未JNI运行加载、未Java/C数值对照、未游戏/性能测试。没有模组打包。main未直接实现Java/C或编译。

## 本轮只读范围

工作区D:/alianximod。可读AGENTS.md、.grok/skills/max-dispatch/SKILL.md、本目录记录/任务/最终报告/冻结/ABI、docs/纯计算后端使用说明.md、native/compute/README.md。

核心代码：src/main/java/com/bslx/XueXiMod/shu/compute/全部7个Java类（ComputeCodes、ComputeOutput、JavaReference、XuexiComputeNative、ComputeSelection、ComputeCalls、ComputeLiveDispatch），native/compute/src/xueximod_compute.c、CMakeLists.txt、build-windows.ps1、build-unix.sh、build-windows.cmd。已有适配类TrajectorySampler、TrackingSupport、SwordTrackingStep、MeleeCorridorMath，以及debug/performance/PerfCounter和PerfMetric。准确路径可从evidence-A.json或rg按类名定位。

原行为依据：本目录backup-A/下的4个适配类原始版本（另有2个性能枚举），backup-A-live-native/记录分发前版本；read-kernels-final.md是前期定位结果，不应替代对实际差异的判断。若为确认调用时点或数学边界需要，可只读直接相关caller/模型类和既有PerformanceRecorder，不扩成整仓审查，不重审步骤1/2/3和多重释放所有代码。共享改动只关注本次4个适配的交界。不要改源代码、脚本、文档或工作区文件。

## 当前实现事实（供定位，不是通过结论）

统一数字标量/数组接口；默认Java；Java参考公式抽自原4个类。显式-Dxueximod.compute.backend=native +绝对library路径后，加载握手ABI1、能力63，现有6类单次计算按对应批量JNI count=1调用。公开批量入口小于8仍Java；上限4096。已注册FUNCTION及世界/实体/NBT/随机/伤害/方块射线仍留原Java。单条JNI可能更慢，不承诺加速。

native分发缓冲每线程最多4层复用，默认Java不取缓冲；超深度Java；数组/长度/别名/状态处理必须保护业务不读半批。能力缺失/native非0/UnsatisfiedLinkError退同输入Java，链接失败禁用后续native；其它异常不应吞。PerformanceRecorder下新增后端计时和4计数，不重排已有枚举。

C同公式用math.h的超越函数，不保证与HotSpot逐位相同，这项已明确披露；不要仅以“跨平台三角函数可能有ULP差异”制造无可操作缺陷。但如发现可具体复现的阈值翻转、NaN/有符号零/极值语义或默认Java改变，要给准确输入/路径与影响。native默认关闭并不免除它作为已实现后端的正确性责任。

构建已修复一次CMake指针宽度取值（不是数学改动）：从缓存或CMakeCCompiler.cmake实际字段读取，保留64位检查；第二次官方入口编译通过。Java共享SkillChainPlan参数顺序修复不是纯计算作者所改，本次构建已通过。不要把旧失败日志误判为当前失败。

## 检查重点与边界

一次集中审查实际最终变更：默认Java是否保留原轨迹/转向/追踪/走廊语义（尤其零/非有限/边界/nullable/运算次序）；JNI签名/ABI/能力与C导出一致；长度/乘加/别名检查与错误不提交、挂起JNI异常和资源释放；Java/native fallback与输出/reentrancy/threadlocal隔离；显式native是否真被现有caller消费、默认Java开销/分配有无明显回归；统计是否只计一次且与行为一致；加载/版本/平台失败是否按文档安全回Java；构建脚本最终关键路径是否存在已能从静态证据确定的真实问题。只报实质可操作缺陷，不做风格重构建议，不泛泛要求扩范围。

不运行Gradle、javac、C编译、测试程序、DLL加载、游戏或性能实验，不生成独立测试。可以使用只读rg/文件读取和必要差异定位，不读取其他作者进行中日志/思考，不新建代理。这一轮是用户明确授权的Codex复核；只输出一次完整最终报告。不要途中再给独立正式结论/重开复审。若证据缺失或源码正在变化，明确说明限制并停止对不稳定部分下结论。

最终返回中文：按严重程度给每个实质发现的绝对路径/具体行号、触发条件/最小数值例或调用路径、实际后果、最小修复建议；区分直接证据与未验证假设。若无阻塞缺陷明确说未发现，并列关键验证边界。报告末尾记录“用户追加的一轮已用完，累计实现4轮；修复后未授权再审”。只返回报告，不写工作区文件；主会话会保存你的原文供Cursor定向修复。

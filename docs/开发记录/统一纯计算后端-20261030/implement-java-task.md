# 步骤4/5作者A：Java纯计算接口、默认实现、真实接线与JNI契约

固定Cursor CLI --model grok-4.7-high-fast。本阶段为用户已授权实现，非正式复审。原read-kernels会话在resume后保持Ask模式，implement-java-final.md已明确没有写入，因此此派发使用新的默认Agent实现会话承接同一作者A所有权和最终事实报告，不是追加复审。承接设计2/2、实现3/3已耗尽，不另开代理/脑子/代码审查或独立测试。先读本目录记录.md、read-kernels-final.md、read-native-env-final.md。不得把工作变成只写建议或空接口。

## 独占写入范围

- 新增 D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/compute/ 下的纯接口、数据协议、Java实现、JNI桥、后端选择/加载与必要独立生命周期类。
- 既有4个类（按确切类名定位）：TrajectorySampler.java、TrackingSupport.java、SwordTrackingStep.java、MeleeCorridorMath.java。仅抽出已定位的纯数值部分并转调；保留既有签名。查询/实体读取、FUNCTION回调和fitVolume等世界逻辑仍在原Java代码及原时点。
- debug/performance/PerfCounter.java、PerfMetric.java 仅追加本计算后端所需明确计数/耗时，不重排已有枚举，不改采样机制/CSV；如不需要则不硬改。
- docs/纯计算后端使用说明.md；本目录 abi-contract.md、implementation-A-final.md、evidence-A.json、backup-A/（只自己的既有改动前备份）。

不得写native/compute（作者B独占）、network、CastFrame、TechniqueHitEntity、SwordEntity、SwordVortex、任何发布/绑定/领域服务、ModCommands/Config/XueXiMod共享注册器、构建文件、制作表。遇需要新增这些写入才能完成的真实问题时保留现场，返回具体未完成接点，不越界。

## 实施目标

1. 用同一个接口支持Java与C：输入/输出只能是原始数字标量、数字数组和明确索引/长度。公共纯接口和两后端不依赖Minecraft/Forge世界或实体类型，不保留活对象、不碰RNG/世界/NBT/网络。可调用的批量接口必须有固定、可文档化的数据布局；同时保留适配现有单个调用的低开销Java路径，不为了抽象在默认Java每tick新增大量临时数组/对象。
2. 首次接入3组已有纯逻辑：TrajectorySampler的封闭轨迹数值（保留FUNCTION回调原路径）；TrackingSupport.turnToward及SwordTrackingStep.plan的转向/步长；MeleeCorridorMath的纯数值判定。只抽取现行公式，禁止“修正”当前玩法。保留阈值、严格/非严格比较、运算次序、NaN/无效输入既有行为、零向量fallback以及调用顺序。未接入三维交错/一分为三的CastFrame，不声称已接；未来可扩展接口，不本次碰其活跃作者。
3. Java是默认后端，现有游戏无需DLL也应正常运行。C后端可通过明确的JVM启动参数显式选择，并指定本地动态库绝对路径；默认不搜索/加载随机目录、不联网下载、不自动打包DLL。不修改现有COMMON配置/注册器；如需Forge生命周期/日志可用独立外层适配类，纯接口仍不依赖Forge。记录实际所选后端与回退原因，避免每实体刷日志。Windows x64先可用，Linux/macOS由native构建源支持情况如实说明。
4. JDK17使用内置JNI，无第三方依赖/预览FFM。Java桥声明与作者B固定产物名xueximod_compute一致，B产物将位于native/compute的独立构建目录（Windows xueximod_compute.dll）。接口加载前须有版本/能力握手，数组长度、计数、容量、溢出、输出区域、别名/重叠等边界有明确契约；Java与C错误不能留下半批结果被业务消费。加载失败、库不匹配或native可报告失败时按明确策略回到同一Java参考实现；不捕获Throwable吞掉原业务异常/虚拟机致命错误，不声称能恢复进程级native崩溃。
5. C后端必须实际可选择并消费同一接口，不是只留下TODO native。你实现完整Java侧选择/桥接/声明，C定义由B在你返回的ABI后实现。不要依赖C尚不存在而默认开启C；不要给B接口留下猜测空间。批量C调用与现有单次调用的使用边界/阈值要明示，避免把单实体JNI跳转宣称优化；不得为了凑批量延迟或重排当前游戏操作。预留批量入口不能冒充跨实体调度已批量化。
6. 如接性能统计，实际Java/C执行/回退与逻辑请求分清，按既有PerformanceRecorder门控；关闭采样不新增nanoTime/日志，不重记现有MOTION/GEOMETRY/NARROW_PHASE计数，不改现有enum序号。调用只包纯计算边界，不把世界读取纳入此新指标。文档不承诺FPS/TPS提升。

实施时需要的常规接口选择由你在上述已授权目标内完成，不启动新的设计评审。数值精度差异若无法保持现行语义，保留该操作Java路径并明确C能力边界，不能悄悄以近似代替。不要扩张到后台线程、异步调度、逐实体JNI批次重排或全仓数值重写。

## 最终交付给B的冻结契约

abi-contract.md必须写实际完整Java包名/类名/native声明与JNI导出名、ABI版本/能力、操作编号/stride/offset/count、各字段单位与枚举、输出顺序、允许的特殊值/溢出与别名规则、精确计算顺序/阈值、错误码与是否修改输出、异常传播/回退策略、平台浮点编译约束。最好可供C作者直接逐项实现，无需再读整个模组。若批处理使用标记数组/多个缓冲区，全部写清。给出实际Java参考类/方法路径，不让B反推世界对象。

先按获准文件工具为自己改动的既有文件保留备份；若任何动作被审批拒绝，记录原始原因并停止该动作，不加force、不取消auto-review、不换工具绕过。不要补做之前B说明返修被拒绝的备份/哈希。最终evidence记录改动路径和长度/UTC时间、released=true；SHA256不是本次必须项。

本阶段无任何编译授权：不Gradle、不javac、不C编译、不游戏、不独立测试，不build/jar/reobf，不提交。等待全部相关写入收束后由唯一指定编译者处理。最终返回实际完成文件/接线、调用边界、未完成部分和ABI，不给额外正式复审结论；明确“额度已用完，未追加复审；待统一编译”。完成后释放写入权。

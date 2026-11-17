# C：实体、生成、碰撞与伤害性能插桩

你是唯一 C 作者，Cursor CLI --model grok-4.7-high-fast；不委托子代理。用户只授权性能记录。本阶段实现，设计裁决1/2已完成，实现正式复审0/3，不自行追加复审或测试。先完整读取本目录design-decision.md与read-entities-final.md。

唯一可写源码均在 D:/alianximod/src/main/java/com/bslx/XueXiMod 下，按精确文件名确认唯一位置：TechniqueHitEntity.java、SwordEntity.java、SwordEntityMotion.java、TechniqueHitSettle.java、TechniqueHitSpec.java、TechniqueHits.java、PhantomSwordEffect.java、HitVolumes.java，仅八个文件。另可写本目录implementation-C-final.md、implementation-C-evidence.json、before-C/备份。其他源码只读；A负责debug/performance公共接口，B负责释放链（包括ReleaseBatch），不能修改他们文件或读取其未完成工作。

严格使用冻结PerformanceRecorder.begin()/end(PerfMetric,long)/add(PerfCounter,long)/isRecording()，包com.bslx.XueXiMod.debug.performance；完整指标与计数见裁决。A文件暂未落地也直接照契约引用，不补建。服务端分支计时，客户端原逻辑不变；关闭不计时不分配，原业务始终正常执行。显式long+try/finally，不Scope/lambda。

覆盖命中体/剑更新、原motion.tick（含applySpec首次）、flyStep/returnTick、查询和候选返回数量、实际窄判定及通过数、世界clip、owned扫描、spawn请求/构造/成功入世界三种计数、tryHit/settleFlow、实际hurt尝试和true返回、指定三类显式发送调用。裁决规定每一原业务表达式只求值一次；原短路、早退、异常、随机、命中去重和pendingActivation语义不变。不得因插桩改变addFreshEntity或hurt调用次数/返回值。网络只计业务调用，不标成真实包数/字节/延迟。HitVolumes仅查询插桩，别改状态/视觉。

先备份八个现有源文件到before-C/并记原hash。保留所有未提交改动；禁止reset/clean/checkout、提交、制作表、打包、游戏。git使用单次-c safe.directory=D:/alianximod。只读具体依赖，不通读整仓。

无编译授权，不跑gradlew或独立测试，不改配置。必要定向完整性检查随实现收尾，不另开复审。超出设计的困难保留现场报告，禁止降模型。

最终写implementation-C-final.md及evidence.json：完整修改路径/关键行号、指标与计数、保留原行为处理、未覆盖/阻塞、待统一编译和未实测、各文件最后写入UTC时间/hash，声明本作者全部写入结束并释放八个文件。完整最终返回，主会话不会实时跟进过程。

# 实现第2/2轮最终集中复审：只读取证
固定Cursor grok-4.7-high-fast。只读不改文件，不Gradle/游戏/测试，不spawn，不读其他任务实时日志。源码及语言写入已全部收束；另一会话只编译。只核本批R1–R6，不重审设计/旧术式，不扩整仓。先读 docs/开发记录/火属性二级底座-20261028/review-round1.md、repair-round1-final.md、repair-r6-final.md；本报告由gpt-6-astra作本轮唯一结论。
重点依据实际代码及本地已映射Minecraft/Forge二进制，不能只按作者报告或字符串结构测试：
1 新mixin FireResourceHurtMixin的两个PUTFIELD：LivingEntity.lastHurt ordinal1与Entity.invulnerableTime ordinal0是否与实际LivingEntity.hurt(DamageSource,float)字节码匹配？用本地JDK javap或ASM只读核查准确owner/name/descriptor/opcode/次数/位置，给jar路径和最小字节码片段。检查Redirect handler参数是否正确、require1、method/at remap及mixin JSON注册。不要编译工程（另会话正在做）。Mixin0.8.5的Redirect能否直接捕获原方法参数而无需@Local，用本地库源码/字节码/正式支持依据说明；当前HEAD/RETURN栈方案是否本身正确另行判断。
2 ElementDamageScope arm/disarm与enterHurt/exitHurt：正常、早退、嵌套未标记、嵌套显式、相同target/source的再入、hurt抛异常后finally清理是否平衡？HEAD/RETURN不能捕捉异常，实际disarm如何清理pending与active调用帧？是否导致持续ThreadLocal泄漏或后续来源误判？必须给实际代码片段与具体反例，不只假想。
3 FireDamage、SteamBurst调用arm的边界及finally；FireRuntimeEvents事件匹配target/source及只计算本次一次，是否外层已结算/水冰被内层抢走。
4 BurnDispatch先提交后分发、clear世代：同步回调schedule不丢、discard不复活且剩余本tick停止，失败推进和首段延迟保持；给片段。
5 FurnaceService gate、serverSnapshot、Runtime.reconcile/advance的线程拒绝，R1三处实际代码；R6两语言6键及参数个数当前确实正确。
报告给已解决/仍确定问题（路径行号、触发、最小修法）/未实测边界。不要把没有实际Mixin应用当成必然失败，但字节码owner不匹配等可静态确定问题必须明确。允许读取本地JDK、缓存映射jar和Mixin库，不改变缓存。不要以结构断言通过替代上述证据。完成一次最终报告后退出。

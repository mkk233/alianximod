# C：与A首轮返修配套的留焰命中钟

固定Cursor CLI --model grok-4.7-high-fast，WriteMode。A已完成author-a-review1-repair-report并最终停写，其他作者均停止。正式实现累计2/3，设计2/2；本单是同一已知返修的配套实现，不另开复审。只改自己BatchFlameTrail及必要C范围的小型辅助，不读/改其他无关功能。

读取A最新author-a-review1-repair-report.md、author-a-interface-final.md中HitClock.occupied与命中合同、implementation-review-round1-final.md第一项、你已有final接口。A已经改为分类前不写状态地查询，取消/PENDING不占钟，合法免疫/正伤成立才占，同一根本体/留焰共用一只钟。

完成A明确交回请求：BatchFlameTrail.sweep不能先tryMark再分类。先检查是否已经被本体/其它片段占用，已占跳过；取消/无效不写钟，不影响此后合法接触；PENDING未完成不得当成立。合法成立只占一次，保持连续扫掠与同一实际副本共享命中集合，不另造钟、不新增独立火种/引爆/接续/崩地。若用到异步完成，只按现有完成合同，不能按固定6tick猜。检查后的分类与占钟不要反过来令一次合法伤害重复调用。

不得改A TechniqueHitSettle/HitClock/预算或B/D文件，不恢复你上一段取消扩圈修复，不改武器/剑实体/光束不在留焰兼容范围的约定。不Java编译/Gradle/javac/独立测试/游戏/打包，不写表/记录/资源库索引，不正式复审。

交author-c-hitclock-repair-report.md/json，更新ownership及必要interface-final，写实际改动hash/mtime、A交回请求是否完成、还有无具体公共接口缺口、待统一编译、全部停写。

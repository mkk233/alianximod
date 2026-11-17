# A第二段：接B资源、普通武器与术式登记

固定Cursor CLI --model grok-4.7-high-fast。继续已批准实现，不是新增正式复审，设计2/2已耗尽，实现0/3。上一段A已最终停写；B第一段已最终停写且接口稳定。C正在author-c-independent-task.md独立地形/残骸/素材阶段，不读取也不依赖你新接口，禁止你改C文件或读其正在写的中间结果。

读取最新approved-v1.md/implementation-contract.md、你自己的author-a-interface/report和B的author-b-interface/report/ownership。法杖最终必须基础6、test_staff32/1000储备/总16倍回魔/-50%CD，保留上一段正确结果。素材v1已批准归档，不再问确认。

允许写原A所有权与原task白名单，并本次明确新增接线文件 WeaponEnchant、ImprintTreeView 及其实际普通武器命中接入文件（若与B的FireRuntimeEvents冲突，不改B文件，提供调用接口与准确接线请求）。先更新A ownership列新增绝对路径。B源文件包括FireCastContext/SeedMerge/ResourceHitCommit/BurnLedger等仍唯一归B，不跨写。可以只读B最终源码以调用实际公开API，不做单独复审。C文件不动。

完成真实接线：
1. 在ModTechniques登记13个编号、名称、类型、基础伤害、冷却、复杂度、等级20/战斗成长0、附魔标识、装配与魔力；ReleaseDefinitions登记三攻击/四新规则能力，六附魔不得假登记ATTACK。新增术式解锁/刻印展示/装配分类需能看见；ImprintTreeView补四角色显示，不能保留-1。效果实现仍由C交付后统一注册，本段不写空成功效果。
2. 新附魔魔力明确用ResourceEnchantCatalog.explicitManaPrice，不能magicDamage=0导致收1，不能为收费增加直接2伤害。相同附魔占两格收费两份但效果按B载荷去重。新计划费用策略与已批准174例、取消退款和原旧链费用兼容。
3. 新公共释放帧、出生到命中结算携带B ResourceEnchantLoad、bodyScaledBase与legacyFixedPoints。伤害结果真正接受后仅一次ResourceHitCommit.commit；先收齐基数再提交，不能再对同一份调用deposit/produce。火苗2+种火10按B一次合并。旧点火/光束/火球不含新能力时完全沿用旧路径，含新能力时保留各自已批准基数/时机，别将它们都改成种火10。
4. 根上下文要让B实际FireCastContext与A rootCastId表示同一次真实释放，并能覆盖延迟、子分支、火径和未完成账本。可以基于B既有创建接口让A根ID取该实际上下文，不在每次命中/每副本newContext。不要让RootSeedLedger与B produced标记各自重复扣机会而丢2或10。类型/生命周期尽量复用现有接口。
5. 让通用命中事件足够给资源和C消费：明确实际target、owner、正伤/取消/免疫结果、B载荷及根上下文、宿主自身基数/旧固定贡献。只给HostHandoff+枚举而消费者拿不到target/载荷时，必须补足正式的类型接缝，而不是C自己按实体距离猜。保持接口简单且已发布方法可兼容扩展。
6. 完成根账本生命周期接缝：费用结算不提前清去重；存活攻击、未结束火径、待结算命中仍有根引用；最后一份结束/取消且无待执行时释放，不无限遗留静态根Map。燃烧计划可以持有其稳定context，不把根地图提前释放等同允许同cast再挂层。向C给具体retain/release或载体结束调用合同。若确需B增加构造/工厂/通知方法，写清最小签名请求交主会话，禁止私改B；别留下你知道会成功但不生效的占位。
7. 普通武器新附魔走原WeaponEnchant账：纯新载荷不能因旧伤害/旧火种字段都0被丢；保存新载荷并在下一次合法普通攻击被接受后只提交一次，失败不当成功，不再扣第二次费用。即时资源读取/燃烧启动F应符合B合同，不把久前附魔时的热量误作命中时热量。
8. 父子结算仍按原合同真实完成事件触发，异步Tally等待完成回调，不能靠预估flushTick/固定延时冒充结算完成；取消/免疫与资源成功分开。此项是继续实现原合同，不新增需求，不对你上一阶段另开审查。

不读C中间源码；本段只给C可用的注册/载荷/命中接口，C后续载体接入读取你最终文档。公共ModEntities/ModItems里陨石块项/新实体注册要等C最终具体类清单再集成，不猜类名。

不Gradle/javac/build/jar/reobf/runClient/游戏/独立测试。不改xlsx/资源索引/记录.md，不改B/C源码或PNG。必要实现自查不宣称正式复审通过。

交付author-a-bind-report.md/json、author-a-interface-v2.md、更新author-a-ownership.json。明确13登记与尚待C效果条目的状态、B调用和根生命周期、普通武器接线、C需用的最小签名/剩余B请求；文件hash/mtime、待统一编译、全部停写。最后返回完整最终报告。

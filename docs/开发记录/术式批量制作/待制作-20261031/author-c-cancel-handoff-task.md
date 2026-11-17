# C：完成A最终交回的取消接触适配

固定Cursor CLI --model grok-4.7-high-fast，WriteMode。A final-integration和D scorch-bridge已最终交回并停写，C前段也已停写。设计2/2、正式实现仍0/3，本单是A集成已明确的剩余实现请求，不启动独立复审。

读author-a-final-integration-report.md、author-a-interface-final.md与C自己的最终report/interface；范围仅自己的BatchFlightBook/BatchCarrierHooks及必要本批载体辅助文件，不能改A TechniqueHitSettle/公共命中或B/D文件。

完成A明确请求：BatchFlightBook的ENTITY拦截中，首个候选攻击被取消/无效时，不扩中心圈、不提交资源、不生成无伤残骸、不消耗首次合法事件闩锁，不把这次当handled成功。PENDING不得提前视为接受。合法免疫/零最终伤与取消区别沿A HitVerdict合同，父结算真正完成后再接后续。按已经确定的最早事件与取消后继续剩余候选合同给A返回准确结果，不能无限重复同一个取消目标。避免A原直接结算与C中心重复对同目标打两次；保留A已完成的最终接缝兼容。

不要扩审其它功能，不增加玩法，必要实现自查针对这个已知请求。若A现有返回合同缺少必须状态，不绕过/吞掉，交清楚最小接口请求。默认无此请求则直接修C范围并停写。

不Gradle/javac/测试/打包/游戏，不改表/记录/资源库索引，不正式复审。交author-c-cancel-handoff-report.md/json，更新ownership与interface-final（签名若不变也说明），报告改动hash/mtime、该已知接入请求是否解决、待统一编译、全部停写。

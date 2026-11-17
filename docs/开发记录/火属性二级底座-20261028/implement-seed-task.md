# 作者A：火种资源

完整读取 implementation-contract.md 和主设计文档后，按A归属实现。A唯一写 status/fire/seed/**、对应tests、现有 statusquery/ResourceStatuses.java。可读当前状态系统与本地Forge必要API，不通读无关仓库。

提供火种0–100实体持久整数、共享施法账本、preview/consume/grant、不可变snapshot、旧档案整档保余数、ResourceView无限时长和档数、死亡与存活clone生命周期接点。FireSeedState以现有AttributeStateService的底层读写复用，不添加火种MobEffect，不受牛奶影响。

实现产消参数校验与真正服务端主线程约束，null/NaN/负数不可漏进持久数据。按规格0生产保持0，生产倍率floor；同施法同目标达到100也记录生产机会；已消费阻止后来生产；多个目标独立。API为将来绑定提供，不自动让现有火术式产消。纯逻辑检查须覆盖边界、共享余额、重复操作和余数。只完成本模块，不编译Gradle、不做独立正式复审。

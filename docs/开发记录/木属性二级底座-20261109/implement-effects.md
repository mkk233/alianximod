# 通用效果作者 B 实现单

沿用本task和Cursor session，固定 --model grok-4.7-high-fast。完整读 inspect-effects.json、本文件、架构裁决.md 第5和6节。只做用户已授权的通用回蓝/一次迎击API，可供任意属性术式调用，不制作具体术式，不接木系资源成本，不实现回血净化，不派子代理。

唯一写入范围：新 `src/main/java/com/bslx/XueXiMod/combat/counter/`；`player/MagicPool.java`；`src/main/resources/data/xueximod/damage_type/counterattack.json`；验证材料 `tools/wood-base-checks/effects/`；`docs/开发记录/木属性二级底座-20261109/通用效果作者交付.md`。禁止写公共事件、网络、mixin插件或配置、其他作者目录。主会话已确认git存在，原报告“无.git”错误，不据此覆盖脏改动。用 `git -c safe.directory=D:/alianximod` 定向读取基线。

无编译/运行测试授权（共享源码正在写）。只实现并准备有意义的定向验证，交全部写入收束后统一执行，不引入新测试框架。

回蓝按裁决新增返回实际量的restorePool，原restore兼容委托，安全计算、不填法杖、主线程权威、变更才保存同步。不要顺手改自然回蓝、退款收据、法杖扣款或幻剑。

迎击按裁决做独立可用服务与小型spec/准入枚举：单窗口、明确拒绝覆盖、参数验证、敌我和PVP、近战/有主弹体、取消/失败不消费、吸收生命也视有效、成功一次、清理与延后反击、中性counterattack来源、防递归标记。不要用默认清无敌帧的TechniqueHits，不伪装player_attack，不复用火伤类型，不绑定花谱ID。公共事件注册与mixin接线由C做，你交付可调用的hurt帧、Hurt/Damage事件方法、server tick和生命周期方法。

**动伤害成功时序前先精确读 `mixin/FireHurtMixinPlugin.java` 实际包装方法及相关ElementDamageScope接口**：必须确认可拿到最终boolean返回并finally清帧。报告只给了enter/exit名字，不可凭它猜。你可以读取但不能修改公共包装，提供给C的准确hook签名与插入点。若现有包装不能安全承载，先交回精确事实并停对应接线，其他部分可完成；不要自行改为事件当场反击或HurtEvent引用tick末检查的弱方案。

消费窗口必须依赖同一次hurt调用最终接受且事件未取消，不能用目标+来源+tick凑键，支持嵌套调用。来源与ThreadLocal双重防递归。hurt异常必须解除预留并清上下文。延后反击前再次检查主人/目标/维度/敌我等有效性；清理取消排队反击。尊重伤害事件结果及无敌帧，不免死或死后反击。

`通用效果作者交付.md` 必须给出：你对hurt包装的精确事实与行号；C应如何成对enter/finish以及异常finally处理；Hurt/Damage优先级；服务公开API完整签名；注册的DamageType资源；待执行的定向验证与实际验证边界。准备检查资源场景不涉及自身木系成本。

完成返回最终标记JSON，writesSettled=true后停止写，等待C接线。当前成功只表示作者实现已交回，仍待整合、统一编译与验证。禁止改批次额度或操作启动器。

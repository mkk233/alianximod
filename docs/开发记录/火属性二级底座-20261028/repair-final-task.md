# 按第2轮结论完成R2/R3；不追加正式复审

唯一作者仍为本会话Cursor CLI --model grok-4.7-high-fast。全部其他作者/编译者/取证会话已结束。完整读review-round2.md；这两项是已裁决、已授权返修，不另开设计/实现正式复审，不spawn、不请求其它模型。实现正式额度已用完2/2，不得把作者必要定向验证改称第三轮审查。

写范围仅本批release/fire/**、FireResourceHurtMixin及必要同目的mixin辅助、xueximod.mixins.json、对应火伤检查。只有R2/R3需要的边界改动；R1/R4/R5/R6和已通过其他逻辑保持，不动旧术式、SkinAttachmentChecks、OriginalHumanoid、表格或已有其他系统。禁止reset/clean/checkout。主会话只写文档。

R2明确改法：invulnerableTime的target必须是 Lnet/minecraft/world/entity/LivingEntity;invulnerableTime:I，PUTFIELD ordinal0 require1。lastHurt维持LivingEntity/F/ordinal1/require1。两处Redirect直接在字段实例、新值之后追加hurt原参数DamageSource、float获取来源，不需要@Local；不再让冷却隔离依赖R3的栈顶。仅xueximod:fire_resource跳过这两次写入，普通来源照常，不清零、不绕过防御。

R3必须真正按调用令牌/进入边界配对异常清理：正常/早退平衡；异常向外穿过多层hurt能全部退回调用前边界；未经包装的内层hurt异常在监听器内被捕获时，外层上下文仍正确；目标override hurt在super之前抛异常，不得弹掉既有外层；Throwable退出不残留。不能只把catch RuntimeException改成Throwable然后盲弹一次，也不能靠下一次hurt才清理、定时清理、弱引用或吞掉异常。

先查项目已有Mixin能力（包括是否实际已有可用方法包装能力），在既有依赖内用最小的真实around/finally方案，不要以“没有@Local”再次误判原参数捕获。若继续全hurt跟踪，每次调用必须有真实异常安全边界，只有HEAD/RETURN不够。不要复制整份vanilla hurt或重写伤害算法；若用精确方法边界适配/变换，只限该hurt方法的进入/退出清理，不改游戏业务分支。优先不引入新外部依赖、不扩大构建配置；遇到必须突破此范围的真实阻塞就如实报告，不交一个已知不满足的伪完成版本。

必要作者定向验证（不是另开复审）：
- 对review-round2指定本地recomp.jar实际字节码验证两处owner/descriptor/PUTFIELD ordinal匹配；如采用方法边界变换，验证变换后正常返回及Throwable路径都按对应令牌结束，不跳过原业务/异常传播。只做修复相关的确定性验证，不另起全包评审。
- 增加有意义异常生命周期回归：正常、早退、嵌套RuntimeException向外传播、内层异常被回调捕获而外层继续、super前异常、非RuntimeException(Throwable)以及最后无残留与外层身份保持。
- 编写/更新检查后可以独立javac运行纯逻辑或本地字节码检查，明确实际覆盖层，不声称已游戏织入。

不运行Gradle、不打包、不启动游戏。写入结束后交唯一编译会话在固定JDK17和真实Gradle缓存统一编译。最终报告实际文件/API变化、R2/R3具体落实、检查入口/结果、所有限制及源码写入结束，并明确“返修结果未追加复审”。

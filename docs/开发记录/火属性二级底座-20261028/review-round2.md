# 火属性二级底座：实现集中复审第2/2轮

结论：不通过，R2、R3仍须返修；R1、R4、R5、R6在本轮定向范围已解决。实现自动正式复审额度现已用完2/2，后续已授权返修与编译可继续，但不得自动启动第三轮复审；未追加授权时需明确写“返修结果未追加复审”，不能标实现复审通过。

依据：review-evidence-round2-final.md（Cursor CLI grok-4.7-high-fast，session 97fe5dc5-87ca-4a75-8d56-e2293684ad2f，success/exit0）、repair-round1-final.md、repair-r6-final.md、compile-final-final.md。本轮成片源码与映射字节码均由Cursor只读取证；本人未改源码、未编译、未运行游戏，不重审设计或旧术式。

## 必须返修

### R2 [P1] 字段owner不匹配，必需的Mixin注入找不到目标
文件：src/main/java/com/bslx/XueXiMod/mixin/FireResourceHurtMixin.java:54–62；src/main/resources/xueximod.mixins.json:2、31。

当前invulnerableTime Redirect的target写为Lnet/minecraft/world/entity/Entity;invulnerableTime:I，但本地实际映射recomp.jar里LivingEntity.hurt的常量池#602是LivingEntity.invulnerableTime:I。偏移311是该字段唯一PUTFIELD，ordinal0。Mixin 0.8.5 MemberInfo.matches严格匹配owner，Entity不能匹配LivingEntity；require=1且配置required=true，因此这是注入应用校验阻塞，不能靠compileJava成功排除。尚未启动游戏，没有实际崩溃日志；结论来源是目标字节码与匹配实现。

最小修法：将target改为Lnet/minecraft/world/entity/LivingEntity;invulnerableTime:I；保持PUTFIELD、ordinal0、require1。handler实例参数已经为LivingEntity。lastHurt的LivingEntity owner、F描述符与ordinal1正确：偏移294为普通差额分支ordinal0，偏移305为专用伤害所经else分支ordinal1，不要改动正确的lastHurt定位。

本地证据jar：C:/Users/baishang/.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge/forge/1.20.1-47.3.33_mapped_parchment_2023.06.26-1.20.1/forge-1.20.1-47.3.33_mapped_parchment_2023.06.26-1.20.1-recomp.jar。

同时纠正返修报告的技术理由：Redirect捕获hurt原方法参数不需要@Local。Mixin 0.8.5的RedirectInjector.injectAtPutField支持字段实例、新值之后追加原方法DamageSource、float参数。建议两处Redirect直接捕获DamageSource判断专用类型，使冷却隔离不依赖R3的调用栈。保留原值、不清零、不绕过防御的修复目标不变。

### R3 [P2] 异常离开hurt会留下或误弹元素调用帧
文件：src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/ElementDamageScope.java:38–43；FireDamage.java:53–62；SteamBurst.java:56–65；FireResourceHurtMixin的HEAD/RETURN注入。

正常返回、ireturn早退、正常嵌套和同target/source再入的隔离逻辑已改善，但RETURN只覆盖ireturn，不覆盖异常。disarm遇claimed直接返回，不清ACTIVE；两个包装入口只在catch(RuntimeException)时无条件exitHurt一次，没有与本次实际入栈令牌配对。

可触发的控制流：外层strikeOnce进入hurt，监听器又调用未经包装的内层hurt；内层抛RuntimeException并传播到外层包装catch。两次HEAD均已压栈，两次RETURN均未执行，catch只弹一层，外层残留；finally里的disarm因claimed不清理。另一个问题是目标override hurt在调用LivingEntity.hurt前抛异常：本次没入栈，catch仍可能弹掉正在进行的外层。非RuntimeException退出也没有清理。残留帧持有目标/来源引用留在线程上，并可能影响后续元素判定或弹栈。不能仅把catch扩大成Throwable后仍盲弹一次。

修复要求：以本次调用令牌或明确的进入边界配对清理，正常RETURN及异常退出只结束自己所属调用；尚未进入hurt不得弹外层。显式伤害包装的finally必须恢复本次进入前的作用域边界，移除本次遗留子调用而保留既有外层。若继续对所有hurt做栈跟踪，需给每次跟踪调用提供实际可用的异常安全around/finally机制，覆盖内层异常在监听器内部被捕获后外层继续的情况；不能只依赖RETURN注入。避免新增库或大改游戏方法前先确认项目现有Mixin能力，按最小可支持方案修复。

必要定向回归：正常/早退仍平衡；嵌套RuntimeException向外传播后栈恢复；内层异常被回调捕获后外层元素仍正确；未进入super.hurt即抛异常不损外层；Throwable退出无残留。检查应针对调用生命周期与身份，不能只按新字符串断言。

## 已解决项

- R1：FurnaceEvents.java:27/42/52已直接取得Player；真实compileJava退出0，上一轮3处编译错误已消除。
- R4：BurnDispatch.java:38–45先advance并write，再strike；BurnCarrier.clear:94–95递增世代。同步schedule读取已提交数据，新轨道不被覆盖且当次不推进；discard/死亡使后续分发停止，不再旧账回写。首段延迟与失败推进保持。
- R5：FurnaceService.gate:107–117拒绝非服务端主线程；serverSnapshot:120–123不安全时返回inactive，不reconcile；FurnaceRuntime.reconcile:12及advance:43–45也有门禁。
- R6：zh_cn:62–67与en_us:3–8各6键齐全，基础句仅%1$s，.player两参、.item三参。原版基础分支传一个或两个参数均不会因模板越界失败；有攻击者但无具名武器时不显示攻击者是现句式取舍，不是格式错误。

## 编译、检查与运行边界

编译者对收束后的源码运行compileJava --offline --no-daemon成功（0错误，25个待删除API警告）。compileTestJava退出1，20个错误均位于既有SkinAttachmentChecks.java（Matrix4f不可访问字段及匿名Model缺方法）；不扩修该旧文件，也不得说全量测试编译通过。
10个火底层main检查单独编译，并使用本次build/classes/java/main运行，583断言0失败。它们未应用Mixin，不能否定R2的字段owner问题，也未覆盖R3异常调用链。本轮只是引用执行事实，不另作测试阶段验收。
未启动游戏，未验证Mixin实际织入、4tick火伤与普通攻击交错、抗火/护甲/保护/抗性、牛奶、隐藏effect、命令、死亡/存活clone、存档恢复或蒸汽距离。不新增任何术式，不修改平衡，不把未实测效果写成通过。

本轮结束，停止复审；不自行派返修或开启第三轮。

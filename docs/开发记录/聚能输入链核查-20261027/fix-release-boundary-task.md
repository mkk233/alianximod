A最后一项公共扫描增量，GPT-6-astra已确认是本轮必须修的输入跨释放成员泄漏。只改SkillChainPlan.chainEnergyBefore单一公共扫描点及RouteReleaseInputCheck对应回归，不改其他扫描器、不改份数/附魔/费用。
当前用earlierTechnique.baseDamage>0当边界，新增合法零伤消费者水环/领域后会让一次聚能跨多次释放。先取earlier成员定义，再用下面的通用边界：
!earlierTechnique.isEnchantment()
&& definition != null
&& definition.kind()==ReleaseKind.HIT_VOLUME
&& definition.role()==ChainRole.ATTACK
遇到边界，sharesDualRelease(techniques,earlier,actionIndex)为真保留现有continue，否则break。非边界继续按既有chainEnergyScale收集。资格和边界分开：NONE法术、剑术不能消费但依然是释放边界，聚能不能跨过它们惠及后面元素法术。聚能自己RULE+ATTACK不是边界，不能只看role。ACTION宿主不截断，保持原穿过动作行为；未注册定义仍跳过。不按术式id特判，不改sharesDualRelease除非明确发现不满足既定双重且先报告主控。

最小生产回归：
聚能→水环→雾雨=2,1；
聚能→正伤元素→雾雨=2,1；
聚能→NONE法术→元素法术=1,1；
聚能→水环→聚能→雾雨=2,2；
既定双重组两个释放成员都2，组外后续不继承；
两条聚能→首元素成员4，动作宿主不误截断。
直接调用真实frozenChainInput/编译计划/现有双重判定，零伤容器用WaterReleaseDefinition的真实类型/role语义，不造假假定positiveDamage。

上一项最终资格和注册已确定，只允许元素MAGIC；保持所有已通过部分，不重复修改它们。修改完成后由你增量compileJava --offline --no-daemon，GRADLE_USER_HOME=C:/Users/baishang/.gradle，日志compileJava-boundary.log；再跑受影响RouteReleaseInputCheck，记录checks真实结果。其他代码未变已通过检查不用重复。容器/墙host由于缺ModLauncher不能跑完，按当前限制记录，不再反复尝试Bootstrap，不启动游戏。
更新route-report.md、validation-report.md、scope.md，并写boundary-report.md准确说清顺序/双重例外。不得build/jar/reobf/runClient，不打包不提交。完后立即停止交独立Grok复核。
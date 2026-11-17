A最终资格收口。先读此完整任务再改。主控重核用户原话“聚能只对元素术式生效，剑术无法使用聚能”，已将先前可选的NONE扩展澄清收为按较窄原需求执行，并已告知用户；不再等待该可选问题，但绝不能写成用户回答过选项。GPT-6-astra已正式裁决如下。

frozenChainInput只有以下全部成立才扫描链倍率：
skill != null && skill.getType() == SkillType.MAGIC
member != null && member.getType() == TechniqueType.MAGIC
member.getAttribute() != null && member.getAttribute() != ShuAttribute.NONE
definition != null && definition.kind() == ReleaseKind.HIT_VOLUME
!member.isEnchantment()
空列表、非法索引、null成员一律BASE；不是SWORD之外所有类型都可。不要baseDamage>0过滤，领域/水环等零伤容器必须可用。脑子已核对WaterReleaseDefinition.kind()==HIT_VOLUME，雾雨/深海重压/屏障/水环均MAGIC+WATER；聚能源自身属性/类型/所用原胚不能代替目标成员资格。剑技槽、SWORD成员（含元素剑术）、NONE法术、ACTION/RULE/MOVEMENT/UNIVERSAL目标、附魔均不消费。
聚能源注册类型改MAGIC，经已有装配兼容规则挡住新剑技装配，确认SkillAssemblyPacket实际检查；既存剑技靠工厂BASE兜住。保留普通规则/动作的兼容，不按id写资格判断、不改ChainRole.ATTACK。若注册文件尚无before快照，改前补保存，绝不覆盖已有before。

只在你A独占的资格工厂、注册、装配、RouteReleaseInputCheck及相关docs做有限改动。其他组源码已经独立审查通过，不重写。SkillFollowCast已确认无外部调用的遗留入口，本次保持未调用不修改，不成为当前运行漏接。
测试补元素MAGIC正伤和零伤容器2/4，NONE、元素剑术、剑技槽伪造MAGIC、ACTION/RULE/附魔、非法索引null全部BASE；缺月ownEnergy1.35依旧1.35。删除工厂注释和docs/底座功能点.md、route-report.md、scope.md中“等待用户选择”的过时状态，明确当前只支持元素法术、NONE不支持。

完成后由你运行增量compileJava --offline --no-daemon（GRADLE_USER_HOME=C:/Users/baishang/.gradle），重跑受本次工厂/注册变化影响的RouteReleaseInputCheck。前轮其他定向checks若已通过且代码没变不用重复；如前轮验证有本任务编译或检查失败先真实修复，再执行相应验证。编译日志用compileJava-final.log保存，不覆盖前轮原始证据。不得build/jar/reobf/runClient，不打包不进游戏。
完成即写eligibility-report.md和更新validation-report.md实际结论，再交回独立审查。不要git提交，不要求用户再次授权。
补充：公共路线Grok独立审查见review-cursor-routes-final.md，两项要闭环。1) 现用SpellAssemblyMenu而非旧SkillAssemblyPacket.put，当前聚能UNIVERSAL导致菜单可装剑技。脑子方案把聚能注册改MAGIC是主要修复，必须核对现行服务端菜单rejectReason/mayPlace/set实际能拒绝；不要只验旧put。遵循现有类型兼容，不盲目新增多余id判断。2) RouteReleaseInputCheck原来把数值1断言为BASE对象身份，契约只要求值，不要求单例。修为sizeMultiplier/数值相等并验证第二成员不复用第一份倍率2，不要为迁就测试改变生产工厂的值1对象策略。若前轮验证已修就核对即可。审查关于SkillFollowCast外部调用和NONE的待办已被最新brain裁决更新，以上新任务覆盖旧描述。
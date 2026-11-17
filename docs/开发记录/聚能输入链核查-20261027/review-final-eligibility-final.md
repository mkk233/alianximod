我先完整读取这份最终资格审查任务，再按文件里的范围和约束执行。前轮两项已按裁决修过。本轮只核最终资格、注册和对应测试，对照改前快照看生产工厂和菜单拦截。工厂条件已经改成元素法术命中体积。接下来核对注册类型怎么进物品数据，以及剑技菜单是否用这份类型拒绝装配。资格工厂本身按较窄口径成立，但剑技菜单的类型检查挡不住一次换手。这项不能记为 PASS。测试没有执行，Gradle 也没有跑。

## 缺陷：换到法杖后，剑技菜单会收下聚能

聚能已注册为 `TechniqueType.MAGIC`（`ModTechniques` 第 112–114 行）。物品不缓存类型：`SkillStoneData.getTechnique()` 只用 NBT 里的 `spell_id` 调用 `TechniqueManager.getById`，旧石头也会读到现在的 MAGIC。

手持剑打开装配时，`SpellAssemblyMenu` 把 `staff` 固定为 false，齿轮格的 `mayPlace` 和 `set` 都走 `rejectReason`。这时主手不是法杖，`SkillCarrier.allowsMagic` 为 false，第 313–315 行会拒绝法术。这是正常路径。

主手可以在菜单仍开着的时候换成法杖。剑技菜单不锁快捷栏；`clicked` 只有 `staff == true` 才拦截快捷栏交换。换上法杖后 `allowsMagic` 变为 true，而 `staff` 仍是打开时的 false：

```313:315:src/main/java/com/bslx/XueXiMod/gui/SpellAssemblyMenu.java
        if (technique.getType() == TechniqueType.MAGIC && !SkillCarrier.allowsMagic(player.getMainHandItem())) {
            return "无法装配：法术不能装入剑技槽";
        }
```

```276:285:src/main/java/com/bslx/XueXiMod/gui/SpellAssemblyMenu.java
                    if (data.getTechnique().getType() == TechniqueType.MAGIC && !SkillCarrier.allowsMagic(player.getMainHandItem())) {
                        continue;
                    }
                    techniques.add(data.getTechnique());
                    // ...
                    PlayerBoundSkills.set(serverPlayer, slot, new Skill(techniques.get(0).getName(), SkillType.SWORD, techniques, embryos));
```

触发：手持剑打开装配，把主手换成法杖，再把 `rule_gather_energy` 放进剑技齿轮格。服务端 `set` 放行，`writeGear` 把它写入 `SkillType.SWORD`。释放仍由工厂因技类型不是 MAGIC 收成 `ReleaseInput.BASE`，所以倍率没有放行，装配口子还在。

修复：剑技会话（`staff == false`）直接拒绝 `TechniqueType.MAGIC`，`rejectReason` 和 `writeGear` 都不要再看当前主手的 `allowsMagic`。

## 工厂与测试

`SkillChainPlan.consumesElementMagic`（第 543–559 行）同时要求技类型 MAGIC、成员 MAGIC、属性非 null 且非 `NONE`、定义 `HIT_VOLUME`、非附魔。空技、空链、越界、空成员、剑技槽、剑术成员、动作（`ReleaseKind.ACTION` / `TechniqueType.MOVEMENT`）、规则种类、通用、无属性都返回 `BASE`。没有术式 id 分支，也没有 `baseDamage > 0` 过滤。

雾雨和水环是零伤 `MAGIC` + `WATER`，定义走 `WaterReleaseDefinition.kind()`，固定 `HIT_VOLUME`。前面一挂或两挂聚能时，工厂倍率是 2 和 4。缺月仍是剑术，自身 `energyScale` 1.35 留在 `geometryEnergy`，资格 `BASE` 不会把它改成 1。倍率恰好为 1 时仍 `new ReleaseInput(1f)`，没有收成单例。

`RouteReleaseInputCheck` 调用生产的 `frozenChainInput`、`collectModifiers` 和 `CompiledReleaseStep.compile`。原先两条 `== ReleaseInput.BASE` 已改成倍率 1，并检查后一发不是前一发的 2。资格矩阵覆盖正伤 2/4、零伤 2/4、无属性、剑技槽、动作、通用、规则种类、附魔，以及空技、空链、越界、空成员、缺定义。一分为三、落点指定、冲刺和跳跃的注册类型没有改。`SkillFollowCast` 无外部调用，不作为运行漏接。

无属性不纳入，没有待答项。
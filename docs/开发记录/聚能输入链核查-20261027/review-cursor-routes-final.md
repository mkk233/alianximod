我先完整读取这份任务说明，再按文件里的范围和要求执行。任务要求独立只读审查，不调度其他代理。我先读契约和路线报告，再按 diff 核对这次改动的接线。契约和路线报告已读完。接下来对照 `before` 快照，只核对接线相关方法和调用签名。公共能量输入接线未通过。计划冻结和执行沿用本身对得上，但现行剑技装配能把聚能放进剑技槽，定向测试里有两条断言和实现不一致。测试没有执行，Gradle 也没有跑。这不是最终资格。

## 缺陷

### 1. 现行剑技装配不拒绝聚能

`SkillAssemblyPacket.open` 打开的是 `SpellAssemblyMenu`，不是仍带 `ACTION_PUT` 的旧界面。拒绝只写在 `put` 的非法杖分支。

```126:135:src/main/java/com/bslx/XueXiMod/network/SkillAssemblyPacket.java
        } else {
            if (slot < 0 || slot >= 4) return;
            if (technique.getType() == TechniqueType.MAGIC && !SkillCarrier.allowsMagic(mainHand)) {
                player.displayClientMessage(Component.literal("无法装配：法术不能装入剑技槽"), false);
                return;
            }
            if (SkillChainPlan.suppliesChainEnergy(technique)) {
                player.displayClientMessage(Component.literal("无法装配：聚能不能装入剑技槽"), false);
                return;
            }
```

玩家用装配键走 `ACTION_OPEN`。剑技齿轮格的 `rejectReason` 只拦法术和容量，`mayPlace` 和 `set` 都用它，通过后 `writeGear` 把整条链写成 `SkillType.SWORD`。

```264:286:src/main/java/com/bslx/XueXiMod/gui/SpellAssemblyMenu.java
        } else {
            for (int slot = 0; slot < 4; slot++) {
                // ...
                    techniques.add(data.getTechnique());
                    embryos.add(data.getEmbryoAttribute());
                // ...
                    PlayerBoundSkills.set(serverPlayer, slot, new Skill(techniques.get(0).getName(), SkillType.SWORD, techniques, embryos));
```

```313:333:src/main/java/com/bslx/XueXiMod/gui/SpellAssemblyMenu.java
        if (technique.getType() == TechniqueType.MAGIC && !SkillCarrier.allowsMagic(player.getMainHandItem())) {
            return "无法装配：法术不能装入剑技槽";
        }
        // 容量检查之后直接 return null
        return null;
```

触发：手持剑打开装配，把 `rule_gather_energy` 放进未锁定的剑技齿轮格。该术式是 `TechniqueType.UNIVERSAL`，定义里 `chainEnergyScale` 为 `2f`（`ReleaseDefinitions` 第 26 行，`ModTechniques` 第 112–114 行）。`suppliesChainEnergy` 为真，但菜单不查它。服务端菜单会收下并写入该槽。

之后释放仍会因技类型是 `SWORD` 冻结成 `ReleaseInput.BASE`，所以这是装配口子，不是释放倍率口子。

修复：在 `SpellAssemblyMenu.rejectReason` 的非法杖分支调用 `SkillChainPlan.suppliesChainEnergy`，拒绝文案与 `put` 相同。`writeGear` 再挡一次，避免只靠 `mayPlace`。

### 2. 定向测试有两条断言对不上生产冻结

`frozenChainInput` 只在倍率非有限或 `<= 0` 时返回 `ReleaseInput.BASE`。没有前序聚能时 `chainEnergyBefore` 得到 `1f`，接着 `new ReleaseInput(1f)`。`ReleaseInput` 没有把 `1f` 收成 `BASE` 单例。

```529:537:src/main/java/com/bslx/XueXiMod/network/SkillChainPlan.java
    static ReleaseInput frozenChainInput(Skill skill, List<Technique> techniques, int actionIndex) {
        if (excludesSwordChain(skill, techniques, actionIndex)) {
            return ReleaseInput.BASE;
        }
        float gatheredEnergy = chainEnergyBefore(techniques, actionIndex);
        if (!Float.isFinite(gatheredEnergy) || gatheredEnergy <= 0f) {
            return ReleaseInput.BASE;
        }
        return new ReleaseInput(gatheredEnergy);
    }
```

`RouteReleaseInputCheck` 第 58–60 行要求无聚能法术成员 `== ReleaseInput.BASE`。第 87–89 行对「聚能 + 两发攻击」的第二份计划做同样比较。两处都会失败。第二发倍率确实是 1，没有沿用前面的 2，但失败原因会被写成「复用了前一份」。剑技和元素剑术那两条 `== BASE` 与 `excludesSwordChain` 一致，不受影响。

该 `main` 调用了生产的 `collectModifiers`、`CompiledReleaseStep.compile`、`frozenChainInput`、`geometryEnergy`、`suppliesChainEnergy`。这是计划构建，不是游戏内释放。本审查没有执行它。

修复：这两条改为断言 `chainEnergyMultiplier()` 为 `1f`，并断言第二份不是第一份的 2 倍快照。不要为了迁就测试把倍率 1 改成返回 `BASE`，除非契约改口。

## 已核对、未算新缺陷

计划建立时每个实际成员冻结自己的输入。`collectModifiers` 结束时写入 `memberInputs`。双重按 `releasedSegments` 每人一份，`fireDualSequence` 的领域、屏障、护体和攻击立即/延迟都用对应的 `dualInput`。一分为三只有一份，`firePaidPlan` 的齐射和复制共用 `attackInput()`。接触窗口稍后调用的 `firePaidPlan` 读的是同一份 `ModifierPlan`。

`CompiledReleaseStep.compile` 写入 `memberInputs` 和 `actionInput`。会话的动作、接触、第二发、条件四参 `attempt`、预约领域/屏障/护体、宿主和 `commitNow` 都读这份快照。`SkillUsePacket` 里操纵剑、领域、屏障、护体、条件、场地、冲刺下一发、宿主和普通 runner 都在调度前冻结，延迟 lambda 捕获该引用。蓄力重入和 `releaseAttached` 用同一份。执行路径不再调用 `chainEnergyBefore`。

剑技整条链，以及任意 `TechniqueType.SWORD` 成员，返回 `BASE`。不按术式 id 判断。缺月 `1.35` 留在 `geometryEnergy` 的自身倍率里。附魔半径仍在 `frame.radiusMultiplier`，再由 `CastFrame.volumeScale()` 另乘。费用、单次伤害、冷却、速度、索敌和预扣退款的计算式没有乘上链输入。

## 待复核，不是新发现

`SkillFollowCast` 仍走不带 `ReleaseInput` 的 runner。旧重载在构建时用 `skill == null` 冻结一次，剑术成员类型会落到 `BASE`，剑技槽整条排除跟不上。主控已安排另补显式快照。

无属性 `MAGIC` / `NONE` 是否允许聚能仍待用户。非剑路径继续沿用既有扫描。本次不能写成最终资格。
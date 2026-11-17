我先把这份登记后续说明全文读完，再只核对它点名的符号。说明只要登记签名和现有最小例子。我按 `ReleaseKind`、`ReleaseDefinition` 和 `ListedRelease` 核对。`ReleaseKind` 只有四项：`ACTION`、`HIT_VOLUME`、`RULE`、`SELF_STATUS`。源码里没有 `DEVELOPMENT_ONLY`。`SELF_STATUS` 会进炉心提交；普通链是否施放不由这个枚举单独关掉。

`ReleaseDefinition` 必须实现的只有 8 个方法：`id()`、`kind()`、`tag()`、`trajectory()`、`moving()`、`lifetimeTicks()`、`speed()`、`thickness()`。`explicitManaCost()` 默认返回 `-1`，此时 `Technique.manaCost()` 仍按伤害推单价。`role()` 默认是 `ChainRole.ATTACK`。`explicitAssemblyCost()` 默认也是 `-1`。

`ListedRelease` 没有公开的 `ACTION` 工厂。私有 `attack(...)` 固定写成 `ReleaseKind.HIT_VOLUME` 加 `ChainRole.ATTACK`，装配费传入 `-1`，并且不覆盖 `explicitManaCost()`。最小公开例子是 `shockRing()`，内部调用该工厂，见 `ListedRelease.java` 84–86 行和 122–128 行。现有 `ACTION` 登记在 `ReleaseDefinitions.registerAll()` 第 13 行：`movement_dash` 用私有 `define(...)`，`ReleaseKind.ACTION`，`ChainRole.ACTION`。`define` 各重载都是私有的。

效果按术式 id 关联，不在 `ReleaseDefinition` 上：

```7:8:src/main/java/com/bslx/XueXiMod/shu/technique/TechniqueEffect.java
public interface TechniqueEffect {
    void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast);
```

注册签名是 `TechniqueEffectRegistry.register(String effectId, TechniqueEffect effect)`。空 lambda 只表示这个回调不做效果；链上仍会按释放定义走。

`ModTechniques.registerAll()` 的顺序是：`WaterCurveFunctions.register()`；逐个 `TechniqueManager.register(Technique)`；逐个 `TechniqueEffectRegistry.register`；然后 `EnchantmentDefinitions.registerAll()`、`ReleaseDefinitions.registerAll()`、`WaterTechniques.register()`。`ReleaseDefinitions.registerAll()` 内部才调用 `SelfStatusDefinition.registerAll()`。`declareSpecialBalances()` 不在 `registerAll()` 里，封存前另一次调用。`TechniqueManager.register` 与 `ReleaseRegistry.register(ReleaseDefinition)` 都会推进 `ChainStructureEpoch`。

术式构造用 10 参数：`Technique(String id, String name, TechniqueType, float baseDamage, int baseCooldown, int levelCap, String description, int complexity, ShuAttribute, float projectileSpeed)`。`fire_cool` 是这条构造加 `ChainRole.ACTION` 的最小例子，但它的 `kind()` 是 `SELF_STATUS`。`SkillSegmentMath.isSelfStatus` 只看 `kind()==SELF_STATUS`，`SkillUsePacket` 501–502 行因此调用 `SelfStatusCommit.attempt`。不声明 `selfStatus()`、且 `kind()` 不是 `SELF_STATUS`，就不会进炉心提交。

三个拟定 id 只要 `TechniqueManager.register` 成功，就会进入 `getAll()`，并受已学、解锁和装配额度约束。`refusesMixedChain()` 只拒绝与其他段混装，不阻止单独按键。当前没有「已登记但不能从普通链施放」的开关。

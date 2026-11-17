我先读这份任务说明，确认授权范围后再按它完成审查并交完整报告。任务是只读实现检查。我先读批准方案、原始需求和作者终稿，再对照实际改动核对。批准方案和作者终稿已对上。接下来按任务单核对接线代码，重点看几何切片、产种账和绑定覆盖。灼地这次只读检查发现 4 个实质缺陷。方形贴地目前对不上方块世界坐标，合法片会被静默截到 128，同一次释放的多个宿主会在周期边界把首次和周期叠在同一拍，绑定计划和附魔按术式编号后写覆盖。下面按优先级给出路径、复现和最小修法。本会话没有改文件，也没有做实现第 1/3 轮裁决。

## 阻塞

### 1. 检测盒用了方块局部坐标

`SurfaceContactField.collect` 把 `getCollisionShape(...).toAabbs()` 直接当成世界坐标去裁 4×4，再用顶面 Y 对参考高度做 ±3 过滤。

```56:61:src/main/java/com/bslx/XueXiMod/shu/technique/release/firecast/SurfaceContactField.java
                        VoxelShape shape = state.getCollisionShape(level, column, CollisionContext.empty());
                        if (!shape.isEmpty()) {
                            for (AABB box : shape.toAabbs()) {
                                Volume clipped = clip(box, lowX, highX, lowZ, highZ);
                                if (clipped != null) {
                                    volumes.add(clipped);
```

同仓库里已经在用的顶面搜索会加上方块坐标。`StandSurface` 第 36–39 行是 `place.getX() + bounds.minX` 这一组；`WeepingCurtain` 第 347 行是 `shape.move(columnX, layerY, columnZ)`。碰撞形状本身在方块内的 0–1 范围。

复现：人站在普通地表，例如 XZ 远离 0、脚底 Y 远离 0–4，放出灼地。宿主仍会生成，因为落点走了 `StandSurface.columnTop`。随后 `collect` 的局部盒和世界正方形对不上，顶面 Y 也过不了 ±3，切片为空。空切片不接触、不产种、不铺火。只有正方形碰巧盖住世界原点附近、且参考高度也落在局部 Y 附近时，才会出现错位的一小片。

这违反已批准的世界 XZ 轴向 4×4、真实碰撞顶面、楼梯/半砖局部顶面、±3 内多层全部纳入。楼梯分片和净空裁剪的算法写在后面，但输入坐标是错的，所以那些规则现在没有作用到真实方块上。

最小修法：每个碰撞盒先平移到方块世界坐标，再做正方形裁剪、±3 过滤和净空剔除。与 `StandSurface` 使用同一套平移。

### 2. 超过 128 片时静默丢掉合法接触面

`SurfaceContactField` 第 23、106–108 行按面积从大到小排序，只留前 128 片。`FireVisualPacket.encode` 第 140–142 行、`decode` 第 177–179 行又各封一次 128。

服务器接触、绑定采样和客户端铺火用的是同一份截断列表，两边不会各画各的。被丢掉的是面积较小的片。小台阶、楼梯踏步上的目标可以不进接触，客户端也不会铺这些片。

已批准方案要求参考高度 ±3 内每一片合法顶面都纳入，并同步裁剪后的有效片。不允许默默丢片。坐标修好之后，4×4 跨格再叠楼梯、脚手架这类多盒碰撞，很容易超过 128。

最小修法：接触、采样和同步都保留全部合法片。包长按实际片数写，去掉这三处 128 封顶。

### 3. 同一释放的另一个宿主会在首次接触拍领走周期

`SurfaceFireHosts.grantContacts` 第 258–271 行：`claimFirst` 成功就伤害并产 20，然后 `continue`。失败就直接 `admitPeriod`。没有「这个宿主以前是否已经接触过这个目标」的记录。宿主表是 `HashMap`，遍历顺序不稳定。

三重复制会在同一 tick 打开多个宿主，共用 `ReleaseBatch.fireContext().castId()`，见 `SkillChainPlan.firePaidPlan` 第 320–335 行。双重释放或链上间隔则是同一令牌、出生 tick 不同。

复现：三份灼地同一拍生成，目标在场龄 20、40、60 或 80 才第一次同时碰上这些宿主。先遍历到的宿主拿走首次（1 点伤害 + 基础 20），下一份 `claimFirst` 失败，场龄仍是候选，`admitPeriod` 成功，再给基础 15。这一拍变成 20+15。已批准要求是：首次正好落在周期边界时，该宿主本 tick 只给首次；只有已经在接触的其他宿主才走共享 20 tick 门。

错相时更明显。旧宿主场龄到 20，新宿主同一绝对 tick 才第一次见到目标。若新宿主先被遍历，它会占掉周期门，已经在场内的宿主这一拍反而领不到 15。

单宿主、目标从第 0 tick 一直站着时，第 0 tick 只有首次，第 20/40/60/80 tick 只有一份周期，这条路径是对的。

最小修法：每个宿主记下已经接触过的目标。该宿主第一次见到目标时只走 `claimFirst`，失败也不要 `admitPeriod`。本宿主先前已经接触、这一拍仍相交，才提交周期候选。

### 4. 绑定计划和附魔按术式编号覆盖，不按链上位置

`ExplicitHostOffers.capture` 第 40–41 行把 `opening.planFor(actionIndex)` 和 `EnchantmentSnapshot.from(skill, segment, player)` 写入 `ReleaseBatch.rememberExplicitHost`。`ReleaseBatch` 第 79–83 行的键是术式编号，后写覆盖前写。`ScorchEffect` 第 30–31 行再按 `technique.getId()` 读回。

`DomainOpening.planFor` 和段上的 `enchantIndexes` 本来是按链上位置分开的。同一次按键只 `capture` 一次，普通链、条件组（`CastExecutionSession` 第 255–256 行）和复制份读的都是这张表。

复现：一条链里两格都是灼地。前格绑攻击 A、附魔甲，后格绑攻击 B、附魔乙。两格宿主都拿到后格的计划和附魔。只有一格灼地时，三重复制共用这一格，这是对的。

已批准的通用载体要沿现有绑定体系按真实出现位置运行，不能因为编号相同就共用配置。作者已经写了后写覆盖，这仍是已接通的链行为，不是未实现功能。

最小修法：按链上 `actionIndex` 保存计划和附魔快照。执行这一段时把该下标交给宿主。同一格的复制份共用这一格的配置。

## 供裁决的附魔事实

正接触伤害的附魔会真正进入现有接触链，不是只存了快照。

- `ContactEnchantState.open` 第 42 行要求 `contactDamage > 0`。之后每 2 tick 采样。`TechniqueHits.hurt` 成功后才 `FireSeedGrant.deposit`（倍率固定 1.0，`FireSeedGrant` 第 17–22 行），然后 `applyEffects`。燃烧秒数大于 0 时第 178–179 行 `setSecondsOnFire`。
- 灼地原生的 1 点走 `FireDamage.strikeOnce`，伤害类型没有燃烧效果，且 `furnaceSettled` 为 true，不再乘炉心。产种走 `FireSeedProduction.produce`，新建上下文，不调用 `hurt`、命中、点燃或引爆。两条账是分开的。原生 1 点也不读取附魔加伤。
- 原生伤害在释放当拍就 `hurt`。接触状态在同一次服务端 tick 末、普通优先级，晚于灼地的 `HIGH`。`RESPECT_INVULNERABILITY` 会使当拍附魔伤害失败。目标若继续停留，无敌帧过去后的偶数 tick 仍会再试，不是永远不触发。
- 接触伤害为 0 的附魔：既有零伤宿主会在 `ContactCast.refusal` 第 33–34 行拒绝整次释放并提示。灼地基础伤害是 1，第 26–27 行直接跳过这段拒绝。快照仍会记下，但 `open` 返回 null，效果不运行，也没有提示。

这里不另定玩法。零伤附魔应拒绝并提示，还是在不改变原生 1+20 / 周期 15 的前提下让纯效果跑起来，留给实现裁决。

## 核对后未发现实质偏差

- 边长公式是中心向 XZ 各 2 格。参考高度在出生时固定。每服务器 tick 重算，同一 tick 复用。支撑消失后宿主活到原寿命，恢复不重置出生和奖励。年龄用 `getTickCount()`，`hasExpired(now, birth+100)` 先 `close`，第 100 tick 不再检测、产种或绑定。`/time set` 改的是日时，不改这两套计数。
- 接触用实体 `getBoundingBox()` 对单片 `AABB.intersects`。并集盒只做粗查。脚点、圆盘没有当作判定。流体碰撞为空，自身不产片。未加载区块不读。出生当拍在 `open` 里 `settle` 一次，tick 末用 `handledTick` 挡住第二次。
- 首次键是施法者、`fireContext().castId()`、目标。复制份传入同一本 `ReleaseBatch`。新的一次 `open` 才有新 `castId`。首次不写周期门。周期只认场龄 20/40/60/80。门限按施法者加目标，间隔用服务端 tick 相减，满 20 才开，同 tick 第二次失败。余额增加 0 也已经占门。离场、换宿主、单场结束不调用 `discardPeriod`。没有补发队列。
- 产种每次读施法者当前 `FurnaceService.seedMultiplier`，`floorProduction` 后再由余额钳到 100。伤害失败不撤回 `claimFirst`。
- 全部宿主 `releaseHost` 到 0 才 `discardRelease`。维度卸载只关该维度宿主。停服才 `clearTransient`。这两处都不写火种余额，也不清燃烧。主人死亡不关宿主，本拍跳过接触；绑定节拍在主人死亡或不在宿主维度时不推进，回到可结算状态后会补放一轮。登出会关该玩家宿主。
- 显式宿主默认关闭。编译、`qualifiedAttack` 和运行时读的是 `explicitDomainHost()`，没有 `fire_scorch_field` 字符串分支。旧领域仍要非正伤害且 `domain()!=null`，选目标默认仍是完整包含。灼地不走 `domain.initialCost`。显式魔力 `-1` 时，魔力是 `ceil(1×2)=2`，装配是 1。绑定有成员才登记。首拍出生后 20 tick，之后每 20 tick，费用仍进 `BoundReleases.executeAutomatic`。没有合法片时 `samplePoint` 为空，收费前返回，节拍已推进，不补跑。到期由 `HIGH` 先关闭并撤销登记。采样在顶面上方约 0.05、且留在净空盒内，按面积加权。
- 画面用同步下来的同一批切片铺 `ground_flame.png` 和 `ground_embers.png`，文件都在。已有画面只换切片，不改出生和结束时刻。每 40 tick 补发。结束包按宿主 id 移除。`GROUND` 仍走原来的圆盘字段，绘制仍是圆盘。火星从相交体积最大的合法片中心，用速度指向身体中心，粒子是橙色 `FLAME`。增加量为 0 不发。样式参数目前仍画橙色。
- 登记是 MAGIC、火、伤害 1、冷却 30、等级 20、复杂度 0、`HIT_VOLUME` / `SUSTAINED` / `ATTACK`、寿命 100、不移动、不需剑、弹道不可改。`fieldRadius()` 为 0，`domain()` 为 null。中英文描述和上述两张贴图引用存在。

## 覆盖与未验证

读过阶段 A/B 任务与终稿、`intake-final.md`、`记录.md`，以及产种账、方形采集、宿主、释放账、显式宿主捕获、结构编译、绑定运行时、接触附魔、火伤、视觉包和客户端、登记与语言。没有整仓通读，没有跑 `gradlew`，没有跑命名门禁，没有进游戏。静态看过的这些文件没有看到缺方法之类的明显编译错误。游戏表现不能算已验证。周期门限要到停服才清空，功能上符合「离场不删、至少留 20 tick」，长时间会留下施法者加目标的记录。

只读停写。未改源码、表、记录，未编译、未测试、未提交。

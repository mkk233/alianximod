**通过。** 本批没有实质缺陷。`space_cut` 的 10 tick 命中和到期链没被视觉改掉，20 帧由客户端按 UUID 续播。游戏内三道斩击和时序仍未验证；我没有重编译、打包或进游戏。

`StillSheetMesh.of` 每次绘制都新建 `RenderType`，只是分配开销，不改变混合、深度或帧选择。这和专项审查的非阻断意见一致。

## 出生同步

出生时间只在服务端正式激活时写一次。同步字段默认是 `-1`（`TechniqueHitEntity` 第 156 行）。`applySpec` 只写帧数和每帧 tick（第 396–398 行），不写出生时间。

没有宿主组时，`addFreshEntity` 成功后才 `stampStillBirth`（`TechniqueHitSpec` 第 295–296 行）。有宿主组时，生成只 `holdActivation` 并 `stagePublished`（第 264–265、293–294 行）；`HostGroupContext.seal` 确认整组后才 `releaseActivation`（第 185–186 行），末尾盖章（`TechniqueHitEntity` 第 308–313 行）。盖章本身拒绝客户端、拒绝未启用描述、拒绝已经写过的出生时间（第 608–615 行）。暂挂期间 `pendingActivation` 直接返回，10 tick 年龄不计（第 243–244 行）。

客户端在出生时间小于 0 时不收入缓存（`StillSheetPlayback` 第 127–129 行）。描述后到也只会在下一次客户端 tick 末或实体渲染之后补收一次。同一 UUID 再次放入会被拒绝（`StillSheetCache` 第 24–26 行），出生时间和姿态都不改，不会从头重播。

## 事件注册与移除后续播

`StillSheetPlayback` 第 41 行用 `@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, value = Dist.CLIENT)` 挂在 Forge 总线。`MOD_ID` 是 `xueximod`，类在模组包内，扫描方式和现有的 `DomainWorldRenderer`、`GuardRingRenderer` 相同。实际捕获是两个事件：客户端 tick 的 `END`（第 51–63 行）和 `AFTER_ENTITIES`（第 67–78 行）。两者都先扫 `entitiesForRendering`，再画，再清到期。不看视锥，也不看 `shouldRender`。

148 项断言没有投递这些事件，注册是否生效要看这段注解和两个 `@SubscribeEvent`。

正常到期走 `TechniqueHitEntity` 第 289–290 行到 `TechniqueHitSettle.onExpire`，第 161 行 `discard`。这条路径不调用 `cancelStillSheet`。缓存只在显式取消、播满 20 tick、超前超过 100 tick，或切世界、断线、客户端维度卸载时删除（`StillSheetPlayback` 第 86–107 行，`StillSheetCache` 第 57–84 行）。实体从渲染列表消失后，已收入的快照继续画到第 20 tick。

## 三份分槽

一个实体一份快照，键是实体 UUID，不会按术式或锚点并成一份。`space_cut` 仍由 `cohortSword` 注册（`ReleaseDefinitions` 第 23 行），`separatesCohortVisuals` 为真。

绘制只调用 `CohortLaneLayout.sheetWindow`（`StillSheetLayout` 第 8–10 行），不再乘倍率。5×3、高度 0 的同锚点三份沿上下分槽，长边保持 5。两轴都已散开时 `CohortVisual.capture` 把布局槽收成 1（第 38–40 行），`forDrawing` 回到整张面（第 71–72 行），不会再平移一次。每个快照只交 4 个顶点（`StillSheetPlayback` 第 217–224 行）。

## 旧渲染

描述一旦启用，`TechniqueHitRenderer.shouldRender` 返回 false（第 47–48 行），`render` 直接返回（第 66–67 行）。旧的 `renderSheet` 双面路径在实体存活的 10 tick 里不会再画。新网格是透明混合、`LEQUAL`、`COLOR_WRITE`、`NO_CULL`、最近邻且无 mipmap（`StillSheetMesh` 第 27–35 行），并且只画一张四边形。

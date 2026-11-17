# 陨岩实体命中续飞

实现作者停写。未跑编译。不是新增正式复审通过。原批设计 2/2、实现 3/3 仍耗尽。

会话编号来自本轮启动时写入的 `implementation-state.json`：`c944df97-5698-4cb3-ae72-8adf3edc9b0a`。

## 根因

合法实体扫掠在 `TechniqueHitSettle.sweepEarliest` 第 267–270 行：`burstOnImpact` 时把飞行体放到接触点、调用过期并丢弃。过期进入 `BatchFlightBook.expire`，再由 `BatchMeteorLanding.finish` 第 45–47 行以 `Vec3.ZERO` 生成无伤残骸。残骸自己再往下掉，所以命中位置会冒出一块石头。

陨岩不再打开 `burstOnImpact`。公共扫掠因此不会在实体接触时结束飞行体。其他术式默认仍是原来的开关。

## 修改文件

- `BatchMeteoriteEffect.java`：陨岩 `burstOnImpact` 关闭，并打开通用的 `retainFlightAfterEntity`。
- `TechniqueHitSpec.java`、`TechniqueHitEntity.java`：默认关闭的续飞开关。打开后，第一次合法实体结算不结束寿命；原运动停住时用上一拍位移和已观察到的下落继续；续飞后仍做地形射线。
- `BatchFlightRecord.java`、`BatchFlightBook.java`：记下续飞，补齐只应发生一次的中心，解除留焰跟踪，地形不再二次接续。移除时只忘掉内存记录。
- `BatchCarrierVisual.java`：陨岩外观在宿主存活时不因原寿命消失。
- `BatchMeteorLanding.java`：只改注释。超时仍走无伤残骸；撞块仍是原来的崩地和安放。

没有改 `TechniqueHitSettle`、残骸实体、渲染器、贴图、制作表和其他术式。

## 续飞时的弹道和外观

运动对象、原点、朝向和年龄都不换，也不读准星。抛物线、垂落、喷涌先继续各自的现有运动。抛物线采样在原时长处会停住；这时从停住前的真实步长接着积分，竖直方向加上这两步之间已经观察到的加速度。这不是重新瞄准，也不是无重力直线。垂落公式会随年龄继续下坠，不会被这层积分换掉。

外观仍是跟着宿主的陨岩方块模型。宿主还在就一直跟着；宿主被撞块或卸载移除后，外观去掉。服务器移动宿主，客户端沿用原来的实体位置同步。

## 怎样避免重复结算

第一次合法实体命中仍由公共扫掠分类、提交资源和调用一次 `batchFollow`。中心仍由 `noteResolved` 补一次；没有资源提交时，由 `holdAfterEntitySweep` 补原来结束时的那一次。

之后实体入口看到续飞或已经扩圈，就返回不可接续。飞行体不结束，位置也不拉回。留焰跟踪器解除，续飞不再沿路径扫伤害。

撞块时若已经续飞，`intercept` 的地形分支只调用 `finish(..., true)`，并且 `continueChildren` 为假。中心、热波、接续和费用不会再来。安放失败仍是原来的无伤残骸重试。

没有实体命中的超时不进入续飞，仍是无伤残骸，不打中心，不接续。

命中体不存档。卸载时 `onBodyRemoved` 只从内存账里忘掉，不生成残骸，也不重放伤害。

## 检查

已做：按上面四条状态流阅读调用链。见 `author-check.md`。

未做：`compileJava`、游戏内效果。`tools/check-name-gate.ps1` 没有跑成，终端调用被拒绝，没有退出码。新增局部名、参数和字段已按门禁字长和缩写表自查。

## 交付标记

- source_writes_closed: true
- needs_compile: true
- blockers: []

配置没有新键。伤害、半径、体积、费用和缺省回退都没有改。

# 垂丝瀑布接入结果

2026-10-26。只换了垂丝主面的材质和分件几何。没有改 `ReleaseDefinitions`、聚能结算、`WeepingCurtain.fit`、伤害或一分为三生成。没有 `build`、没有打 jar、没有启动游戏、没有提交或推送。

## 修改清单

改过的已有文件，修改前已按仓库相对路径复制到本目录 `baseline/`。新文件没有 baseline。

| 动作 | 路径 | 说明 |
| --- | --- | --- |
| 修改 | `src/main/java/com/bslx/XueXiMod/client/WaterCurtainRenderer.java` | 主面改贴 runtime 分件。不再画交叉侧面，也不再乘旧蓝色。 |
| 新增 | `src/main/java/com/bslx/XueXiMod/client/WaterCurtainLayout.java` | 宽度、帧和 UV 裁切。不读术式 id，不再乘聚能。 |
| 新增 | `src/main/resources/assets/xueximod/textures/entity/xueximod/waterfall_runtime_left.png` | 归档 PNG 原样复制。 |
| 新增 | `src/main/resources/assets/xueximod/textures/entity/xueximod/waterfall_runtime_middle.png` | 同上。 |
| 新增 | `src/main/resources/assets/xueximod/textures/entity/xueximod/waterfall_runtime_right.png` | 同上。 |
| 新增 | `src/main/resources/assets/xueximod/textures/entity/xueximod/waterfall_thin.png` | 同上。异常窄宽回退时只采样固定内容包络，不改像素。 |
| 修改 | `docs/资源库/README.md` | 总索引改为已接入，并写明 charge 不进游戏、游戏内未实测。 |
| 修改 | `docs/资源库/特效/水流/垂丝聚能瀑布_v1/README.md` | 采用状态。旧 8px 分件和 charge 留在归档。 |

`TechniqueHitRenderer.java:82-84` 仍是 visual 7/8 进入 `WaterCurtainRenderer.render`，重水仍是 visual 8。这次没有改这个文件。

baseline 复制时 SHA-256：

- `baseline/src/main/java/com/bslx/XueXiMod/client/WaterCurtainRenderer.java`：`42C164F85DA3AD371FD694FB5DA5E2B2D7206EB58C7D55878176B8A85A526058`
- `baseline/docs/资源库/README.md`：`D91DE486BAA1BF306D7ADFB4A9A791FD64D373467F6C0E1A3D691B739337FBCA`
- `baseline/docs/资源库/特效/水流/垂丝聚能瀑布_v1/README.md`：`3BFA9FF0CFDEF6DF0BE8FF3E6964A219F4123BD8552F34203EA148E0EE6E0958`

## 几何

横向密度 24 像素/格（`WaterCurtainLayout.java:13-15`）。4 像素边缘是 1/6 格。中段宽是 `W - 1/3` 格。`W` 只来自 `entity.width()`（`WaterCurtainRenderer.java:50`），布局函数没有聚能参数（`WaterCurtainLayout.java:73`）。

左右边缘贴在 `±W/2`，各向内占 1/6 格（`WaterCurtainLayout.java:124`、`142`）。中段按 1/6 格周期铺，最后不足一段时几何和 U 一起裁（第 135-140 行），不把整段 U 拉成 1。中段为负，或中段块数超过 512，走 `thinFallback`（第 105、110、146-148 行）。宽度正好 1/3 格时两条边缘相接，不画中段，也不重叠。

纵向同样 24 像素/格，一帧 128 像素对应 `16/3` 格。高于这一段就再铺当前帧，末段裁在帧内（第 140-174 行）。提交的 V 在 `(frame + localV) / 16` 上各收半个纹素（`0.5/128`，第 17、162-169 行），最近邻不会踩进下一帧。几何上的 localV 仍是顶 0、铺满时底 1。

时间是 `floor((tickCount + partialTicks + 相位) / 2) % 16`（第 60-70 行）。相位是 `entity.getId() % 16`（`WaterCurtainRenderer.java:52-53`），同一实体不随宽度变化。左右中三件用这一次算出来的帧。没有用渲染帧计数，也没有负向 UV 滚动。

离线程序 `VerifyWaterCurtain` 对布局类编译后运行，输出 `waterfall-layout-ok`。聚能宽度按基础 0.5 乘链上 `chainEnergyScale` 2 的次数，渲染不再乘第二次：

| 水帘宽 | 含义 | 中段 | 结果 |
| --- | --- | --- | --- |
| 0.5 | 无聚能 | 1 整段，U=1 | 总宽 0.5，边缘各 1/6 |
| 1.0 | 一次聚能 | 4 整段 | 总宽 1.0 |
| 2.0 | 两次聚能 | 10 整段 | 总宽 2.0 |
| 4.0 | 再叠两次 | 22 整段 | 总宽 4.0 |
| 0.75 | 不足整周期 | 2 整段 + U=0.5 | 末段几何也是半个周期 |
| 0.2 | 窄于 1/3 | 无 | thin，U 为 25/64 到 39/64，几何仍铺满 0.2 |

以上宽度在帧 0 和帧 15 的横向起止相同。tick 0 到 63 都是每 2 tick 一帧；tick 30 和 31.99 是帧 15，tick 32 回到帧 0。

高度 0.4 只有一段，V 留在当前帧上半段里。高度 8 是一段满帧加一段裁切，两段 V 都在同一帧内。高度 5.5 的第二段覆盖量是 4/128。宽度或高度不是有限正数时不画。

## 远坐标

顶点是相对实体中心的局部偏移，相乘之后才转 float（`WaterCurtainRenderer.java:128-130`）。实体渲染调度器在写入姿态前，已经用 double 做过实体坐标减相机。这里不再把世界坐标存成 float，也不在已经含有这份平移的姿态上再减一次相机。垂丝在 96 格外不会画（`TechniqueHitRenderer.java:50-54`），局部偏移只有数格。

## 厚度观感

旧渲染画了宽向主面的正反两张，再加一张沿 `entity.length()` 的交叉片。现在每个分件只提交一张四边形，`NO_CULL` 让两面都可见（`WaterCurtainRenderer.java:98`、`106-116`）。交叉片去掉了，所以水帘看起来是没有厚度的一片；碰撞厚度仍由 `WeepingCurtain.fit` 的 `resizeVolume` 维护，这次没改那段。

## 颜色和材质

普通顶点是 `(255,255,255,255)`。重水是 `(210,232,255,255)`（`WaterCurtainRenderer.java:39-41`、`65-67`、`132`），只略偏冷，白沫仍接近白，不会变成粉色。overlay 写的是 `OverlayTexture.NO_OVERLAY`（第 134 行），不是 0。光照是 `LightTexture.FULL_BRIGHT`。四份 `RenderType` 在静态字段里各创建一次（第 35-38、86-103 行）：实体半透明、最近邻、无 mipmap、`TRANSLUCENT`、`NO_CULL`、`LEQUAL`、`COLOR_WRITE`。

## 素材

游戏 PNG 与归档逐字节相同。运行时三件的 SHA-256 与 `asset-hashes.json` 一致：

- left：`DEC6990BCF962707FBC25E5218837DF7ADD4A34227C95F58C65F0E27428443E8`，RGBA，4×2048
- middle：`6BB9C161784BB89F9F5B95580EF56AC92E53ECDD5ED694EDAE923F29FA73AAE7`，RGB，4×2048
- right：`CF6D6241880EF0833ABB53EB46EA847581BD52F15B3AF4A16EE4CD12D70A368F`，RGBA，4×2048
- thin：`E1DA1C1DC49776AD1BFBDB09B8EC01E919D6354F9CB09BFB1E8236759BE9A76D`，RGBA，64×2048

四份都是 16 帧。相邻帧按 8 像素环形下移，含从帧 15 卷回帧 0，错位像素是 0。左件 x=3、中件 x=0 和 x=3、右件 x=0 全部 2048 行都是 `#199ED2` 且 alpha 255。thin 每一行正好 12 个不透明像素，alpha 只有 0 和 255；这些像素落在 x=25 到 38 的 14 列包络里，随水流左右摆。回退已修正为固定内容包络U=25/64至39/64，避免透明留白压缩可见宽度；没有修改素材像素。`waterfall_charge`、旧 8px 分件和 gif 没有复制进游戏。

## 编译和限制

`.\gradlew.bat -g C:\Users\baishang\AppData\Local\Temp\cursor-sandbox-cache\a6dd7201ae0ea39bc33e5d675cf9f1cf\gradle compileJava --offline --no-daemon --console=plain`

最终一次 `BUILD SUCCESSFUL`，约 20 秒。仍有 17 条原有的过时 API 提示，不在本次两个瀑布文件里。没有 `build`，没有 jar。

命名检查仍失败在未改动的 `OriginalHumanoid.java:165` 和 `:202`。本次文件没有新的命名问题，那两处没有顺手改。

游戏内画面没有测。起流、供水、排空、地面截断和伤害只保持原调用，没有在这次渲染改动里重新验证手感。中段超过 512 块会改贴 thin；纵向超过 64 段就停止继续铺，正常落差 5.5 格只用到 2 段。

## 最终修正与复核
异常窄回退的两条路径统一使用固定内容包络25/64～39/64，保留水流摆动且不逐帧改变裁切。GPT-6-astra复核关闭P2，Cursor独立审查最终版本无阻塞缺陷。

Cursor修正后的最终命令日志（final-validate.jsonl）确认：compileJava --offline --no-daemon --console=plain，BUILD SUCCESSFUL in 12s，exitCode=0，compileJava UP-TO-DATE。服务在验证成功、准备更新文档时再次断线，主代理据实际命令输出补齐此记录；未重新修改代码。

未打包、未游戏测试。实际起流、排空、普通/重水及聚能观感由用户测试。

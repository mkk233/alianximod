# 周身水环 v8 渲染接入报告

本轮只改画面和资源。没有改 `GuardSyncPacket`、`GuardStates`、`ModNetwork`、`ModEvents`，没有启动 Gradle，没有打包，没有进游戏。

## 跨模块契约

渲染调用 `view.animationAge(minecraft.level.getGameTime(), partialTick)`。这个方法要由网络侧加在 `GuardSyncPacket.ClientRings.View` 上，签名是 `public double animationAge(long clientGameTick, float partialTick)`，单位 tick，非负，刷新不倒退。当前仓库里还没有这个方法，所以合入前主控编译会缺这个符号。渲染侧没有改收包。

姿态仍用原来的 `from/toward`、`poseStartMillis`、`poseTicks` 和墙钟插值。旋转和贴图帧只用 `animationAge`。环心仍是脚底插值加上 `bbHeight * bodyHeight`。半径只用于 `radius / 11.775`，没有用 `band` 压网格，也没有改防御体积。

## 本次文件

- `src/main/java/com/bslx/XueXiMod/client/GuardRingRenderer.java`
- `src/main/java/com/bslx/XueXiMod/client/WaterRingV8Math.java`
- `src/main/java/com/bslx/XueXiMod/client/WaterRingV8Mesh.java`
- `src/main/java/com/bslx/XueXiMod/client/WaterRingV8Material.java`
- `src/main/java/com/bslx/XueXiMod/client/WaterRingV8Reload.java`
- `src/main/resources/assets/xueximod/textures/entity/xueximod/water_ring_v8.png`
- `src/main/resources/assets/xueximod/textures/entity/xueximod/water_ring_v8.png.mcmeta`
- `src/main/resources/assets/xueximod/waterring/water_ring_v8_mesh.json`
- `docs/资源库/特效/水流/周身水环_细碎毛边_v8/runtime/` 下同名 png、mcmeta、mesh json
- `docs/资源库/过程归档/水环接入-20261027/export-water-ring-v8.py`
- `docs/资源库/过程归档/水环接入-20261027/WaterRingRenderCheck.java`

正式目录的 README 和 `asset_manifest.json` 没动。三张源 PNG 没改。占位人物没有导出。

## 画面行为

运行时贴图直接复制任务目录 `water_ring_v8_atlas.png`，768×640。主体在 x=0、帧高 24；外缘 x=256、帧高 40；内缘 x=512、帧高 40。`.mcmeta` 关闭模糊。渲染状态里贴图采样也关闭模糊和 mipmap。

网格从正式 bbmodel 的三个 mesh 原样抽出，768 个四边形（576+96+96）。导出时顶点已减枢轴 `[0, 15, 0]`，UV 仍是源图像素。资源重载会重读这份 json；平时只用缓存，不在每帧解析。

三层共用一次变换：当地正 Y 旋转（48 tick 一圈，12 tick 时局部 +X 到 -Z），再绕 Z 转源模型的 3°，再用最小旋转把模型 +Y 对齐当前插值法线，最后乘 `radius / 11.775`，放到原来的环心。法线对齐不在 `|normal.y| = 0.9` 处换辅助轴。0 tick 和 96 tick 的旋转与贴图帧重合。贴图每 2 tick 换一帧，16 帧，没有额外绕周滚动 UV。整帧边界收到半像素中心，不是每个四边形各自缩边。

一个静态 `RenderType`，位置+颜色+UV，无光照。深度测试开，不写深度，不剔除。每个四边形只提交一次。顶点 RGB 为白，平时顶点 alpha 255，贴图自己的 alpha 保留。第一人称看自己时，沿用 0.35 格内不画、0.7 格内线性变淡。世界中心先用 double 减相机，再转 float。

## 验证

`python export-water-ring-v8.py` 通过：

- 四边形 768
- 金标准 27 个点，最大误差 `3.553e-15`
- 图集 sha256 `67a0c18fb9048a8597807015ced446908a9787559f001c1a48ad50e0b6e19708`
- 网格 json 230848 字节
- 同时核对了源 UV、0/96 tick、24/40 帧边界、近眼 alpha、远坐标，以及法线越过 0.9 时新对齐连续、旧基底会跳相

未启动 Gradle。另用 javac 只编译了 `WaterRingV8Math` 和任务目录里的 `WaterRingRenderCheck`，输出 `java-render-check-ok`。这不是模组编译。

命名检查仍只报原有的 `OriginalHumanoid.java` 两处，本次渲染文件没有新的命名问题。那两处不在本轮范围，没有改。

## 未做

没有 `compileJava`，没有进游戏看水环。网络包里的年龄连续性、快照姿态、旧 REMOVE 和世界卸载不在本轮。合入后要等 `animationAge` 落地再做统一编译。

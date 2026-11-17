# 火苗 · 十字穿插直射 v3.1

2026-11-13 用户审阅真实 Blockbench 三维视口预览后明确回复原话：**「采用」**。状态：**v3.1 已接入运行资源，待统一编译与游戏实测**。

旧目录 [火苗_飞行火舌_v3](../火苗_飞行火舌_v3/README.md)、[火苗_v1](../火苗_v1/README.md) 与三维候选过程归档 [火苗_v3.1_十字穿插直射示意_20261113](../../../过程归档/火苗_v3.1_十字穿插直射示意_20261113/visual-handoff.md) 均完整保留，未删除或覆盖。

发射一枚沿初始发射方向笔直飞行的火苗弹体，采用原版箭矢式 `±45°` 正交十字穿插双面片（`×` 形截面），亮黄白火头在前、渐窄细尾与火星在后，飞行全程锁定发射方向不随相机转圈，并在第一人称渲染时施加眼下纯视觉偏置以完整展开十字火翼。预览弹速与双阶段间距仅作空间演示，`.bbmodel` 及示意辅助不进游戏运行资源。

## 版本与文件

- `fire_spark_v3.aseprite`：`64×64`、16 帧可编辑二维像素源（同版复用 `v3`）。`10,644` bytes，SHA256 `710957E6C13AB8D8CC0C9EA5BD6CFD424041EE5C8F9BE8231CF75D4EB15C8FCA`。
- `fire_spark_v3_strip.png`：`64×1024`，16 帧自上而下竖条导出，每帧 `64×64`，二值硬边透明（同版复用 `v3`）。`5,005` bytes，SHA256 `672DA9132E7E91A83636836A62ADE4778781D51592A541041C511653238DEC5E`。
- `fire_spark_v3.gif`：二维单贴图循环预览动画（同版复用 `v3`）。`26,426` bytes，SHA256 `A3EA8EB30C557859FCE0D04E21EE0B7E1BFD5E6ED6A155DA456E8F8EE12AB0AE`。
- `fire-spark-v3-1-crossed-launch-preview.bbmodel`：同版真实 Blockbench `5.2.2` 三维空间示意工程（内嵌纹理导出字节与 `fire_spark_v3_strip.png` SHA256 一致）。`42,052` bytes，SHA256 `8E676075011EC74FC596BC0E1F046D7CAC62DACEE87A0CF3B9C79A3567B4C47B`。
- `fire-spark-v3-1-3d-01-first-person-launch.png`：真实视口预览 01（第一人称发射视角，`1366×973`）。`50,738` bytes，SHA256 `A25A668C6ED21C136467E74039802B61F2B8E2FED3AD1C4634F9C86EC3FAF68D`。
- `fire-spark-v3-1-3d-02-hero-3quarter.png`：真实视口预览 02（3/4 近景透视主图，`1366×973`）。`107,329` bytes，SHA256 `B06516E2AB4197529B23851463050E3F5B00525EB4879FEBC47EAEB1190ACA06`。
- `fire-spark-v3-1-3d-03-axial-cross-section.png`：真实视口预览 03（迎面轴向 `×` 十字截面透视图，`1366×973`）。`91,082` bytes，SHA256 `F4C48F8A7B95B38D1E98BA06F74CB6D930AC686ADB139A2E6F43C6D49CA46F07`。
- `fire-spark-v3-1-3d-04-side-view.png`：真实视口预览 04（正交正侧视图，`1366×973`）。`31,892` bytes，SHA256 `0039AC3FB33439C300909DDBB4A9F09BB47FF67AE72327155784144783EF52A8`。
- `fire-spark-v3-1-3d-05-ray-two-stages.png`：真实视口预览 05（同射线双阶段直射姿态对照图，`1366×973`）。`71,835` bytes，SHA256 `7280F8D7ACFFB13D50881AC04340E8F119DA275D6F006218FA3CC4312844C689`。
- `visual-handoff.md`：正式确认版视觉接入交接参数说明。
- `manifest.json`：本正式目录除 `manifest.json` 自身外全部文件的文件名、绝对路径、字节数与 SHA256 清单。

## 空间与朝向规则

- **比例换算**：`16` 模型单位 = `1` 世界格。单张翼面为 `0.80×0.80` 世界格（`12.8×12.8` 模型单位，`halfSpan = 0.40`）。
- **锁定发射朝向**：`CameraFacingTag.NONE`。仅在生成时按归一化发射方向 `unitLaunchDirection` 锁定前向切线，绕发射前向轴旋转 `+45°` 与 `-45°` 构成两张正交双面翼片（`firstFin = (upAxis + sideAxis) / √2`、`secondFin = (upAxis - sideAxis) / √2`），飞行全程不随玩家镜头旋转或每刻微位移重算滚转。
- **首尾与 UV 对齐**：前端 `+half * tangent` 固定对应 `U = 1.0`（圆润亮黄白火头朝前），后端 `-half * tangent` 固定对应 `U = 0.0`（渐窄火尾与火星朝后）；两片翼面的贴图上脊 `V = topV` 均朝 `+upAxis` 斜上方汇合亮核，正反双侧同 UV 不镜像翻转火头。
- **第一人称眼下纯视觉偏置**：渲染中心沿 `-upAxis` 下移 **`0.245` 世界格**（`BatchCarrierVisualRenderer.SPARK_FIRST_PERSON_DROP = 0.245D`，对应示意场景眼高 `25.92` 至弹心 `22.0` 的 `3.92` 模型单位落差），仅偏置客户端视觉面片而不改动服务端碰撞体起点，避免原地静止第一人称施法时的针孔共轴退化。
- **帧采样与材质**：16 帧按弹体存活进度单轮线性推进 `frame = clamp(floor((ageTicks + partialTick) * 16 / durationTicks), 0, 15)`；材质使用 `RenderType.entityCutout`、`LightTexture.FULL_BRIGHT` 与 `NetherDomainTint` 火焰通道染色。

## 运行贴图映射与隔离边界

- **运行贴图**：`assets/xueximod/textures/effect/fire_spark_v3/fire_spark.png`（工作区路径 `src/main/resources/assets/xueximod/textures/effect/fire_spark_v3/fire_spark.png`），与本目录确认版 `fire_spark_v3_strip.png` 逐字节一致（SHA256 `672DA9132E7E91A83636836A62ADE4778781D51592A541041C511653238DEC5E`）。
- **运行贴图元数据**：`assets/xueximod/textures/effect/fire_spark_v3/fire_spark.png.mcmeta`（工作区路径 `src/main/resources/assets/xueximod/textures/effect/fire_spark_v3/fire_spark.png.mcmeta`，UTF-8 无 BOM，`blur=false`、`clamp=true`）。
- **接入回执**：`20261113-integrate-spark-cross-01` 已核对运行贴图与确认导出逐字节一致，并写入上述 `fire_spark.png.mcmeta`。相关 Java 渲染与配置参数与 `visual-handoff.md` 一致，未改源码。正式报告：`docs/开发记录/术式批量制作/待制作-20261031/spark-crossed-v31-integration-report-20261113.md`。统一编译尚未执行。
- **非运行资源隔离**：`fire-spark-v3-1-crossed-launch-preview.bbmodel` 及场景内全部 `示意辅助*` 假人、地面刻度网格、中段对照副本与 `helper_*` 辅助贴图仅用于资源库空间审阅，严禁进入 `src/main/resources/assets/xueximod/`。

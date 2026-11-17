# 水牢 · 等比公告板 v1

状态：用户已确认二维 v3 原画及 v4 等比公告板方式，并于 2026-10-27 授权接入。正式美术归档及游戏接入完成；2026-10-28 逻辑复核、离线 compileJava 通过，待用户游戏实测。

- 单张纯二维动画面片，始终朝向当前客户端渲染镜头。每个客户端独立计算，不同步朝向角度。
- 保持原图比例，宽高统一等比缩放，按目标所需包围范围只整体变大或变小。
- 96×96 每帧；16 帧×100ms；纵向图集 96×1536；1.6 秒循环；透明 RGBA。
- 原画近圆轮廓、厚水壁、主长水流与错位短回流均保留。不采用体积壳模型。

## 文件

- `water_prison_billboard.aseprite`：原生六图层、16 帧可编辑源。
- `water_prison_billboard.png`：游戏用竖向原尺寸图集，字节与获批二维 v3 导出一致。
- `water_prison_billboard_frame01.png`：透明首帧。
- `preview_2d.gif`：深浅背景二维效果。
- `preview_camera_orbit.gif`、`preview_fixed.gif`、`preview_wide.gif`：场景内始终朝向镜头的原生 Blockbench 验证。
- `水牢_等比公告板_v1.bbmodel`：平面与占位人物的编辑工程，仅预览，不是游戏运行格式。打开后运行 [公告板预览脚本](../../../过程归档/水牢公告板验证_v4_等比缩放-20261027/preview-billboard.js) 复现镜头朝向；源脚本留在过程归档。
- `manifest.json`：文件来源、大小与 SHA256。

接入记录：[水牢特效接入](../../../../开发记录/水牢特效接入-20261027/记录.md)。

## 游戏引用与验证

- 运行资源：`xueximod:textures/entity/xueximod/water_prison_billboard.png`。
- 实际文件：`src/main/resources/assets/xueximod/textures/entity/xueximod/water_prison_billboard.png`。
- 渲染入口：`PrisonShellRenderer`，替换旧六面水壳；单张正方形公告板，各客户端按自身相机朝向计算，保留俯仰。
- 尺寸以同步的水牢范围为底并入目标插值碰撞箱，聚能扩大后视觉整体等比变大。玩法与同步协议未改。
- 原图每2 tick一帧，16帧循环（20 TPS时每帧100ms）。第一人称受困视角/相机处在包络内时不画整张面，避免挡住视线。
- 图集与正式源SHA256一致：`8ba44798140ec92fc45b826549cec03cf629bcb7bcd1d34dab750462101e5096`。
- Cursor CLI Grok 4.7 High Fast执行 `.\gradlew.bat compileJava --offline --no-daemon`，退出0；编译覆盖最终源码，未打包、未启动游戏。
- 用户待实测：不同体型、相机环绕/俯仰、多人各自视角、聚能尺寸、循环动画、方块遮挡与消失。单片前移0.08格，极贴墙及半透明物交叠仍需游戏确认。

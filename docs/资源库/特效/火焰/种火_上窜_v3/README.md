# 种火 · 上窜熄灭 v3

采用状态：2026-11-11 用户确认“按照这版接入”。玩法不变，只替换命中时的播种火种画面。旧目录 [种火_v1](../种火_v1/README.md) 保留。

观感：火从目标脚下燃起，贴着身体烧过躯干和头部，再从下往上断开、留下余烬后消失。不是缩小成一个点，也不是飞行弹。

朝向：和水牢一样，单张平面永远朝向当前客户端镜头，含俯仰。多人时各看各的镜头，不锁目标朝向。

距离：平面中心在目标身体中心，再向当前镜头拉近约 0.08 格，贴在身体表面外侧。确认预览里，1.8 格高的人偶前，火焰面高 1.75 格、厚 0.12 个模型单位。1.75 格是无目标时的参考正方形，也是补档示意面的边长；有目标时运行边长等于碰撞高度。补档没有改这套运行参数，也不另提新版本。

规格：64×64，16 帧竖条，每帧 50 ms，共 0.8 秒，播一次。第 16 帧全透明。`preview.gif` 末帧有查看用停顿，不作为游戏时长。

文件：

- `seed_sow_v3.aseprite`：SHA256 `D514877D8AFFFFC6D5D54A9A6E64F46FBB12E41F1CB1C0C84A90C2B9129F1C0D`
- `seed_sow_v3-strip.png`：64×1024，SHA256 `A50EC7ED6A8434F50987A86036201DABFF3AB8A70CFF9D82A31CAC9F12B03EEE`
- `preview.gif`：深浅背景动画
- `bb_billboard_front.png`、`bb_billboard_head.png`、`bb_billboard_side.png`、`bb_billboard_back.png`：2026-11-11 的真实 Blockbench 视口，保留作历史。灰色人偶和地面是示意，不进游戏。

2026-11-11 的 `docs/资源库/过程归档/种火_上窜熄灭_v3-20261111/shot.js` 调用 `Codecs.project.write(path)` 时漏传编译内容，当时没有写出 `.bbmodel`。2026-11-15 已在真实 Blockbench GUI 补上同版模型，不改变 v3 运行参数，仍复用 2026-11-11 的同版确认。

- 模型：`docs/资源库/过程归档/种火_上窜熄灭_v3_模型补档_20261115/seed_sow_v3_preview.bbmodel`，804998 字节，SHA256 `CE7AC65C0406AE3A338FBB5CD7AE98E70D005A245520C7B4603245715DB9852C`。2026-11-15 22:06:38.950+08 从磁盘复开，43/43 贴图就绪。`.bbmodel` 留在过程归档，不进游戏资源。
- 本次视口：同目录 `01-hero-3quarter.png`、`02-front-billboard.png`、`03-side-offset-thickness.png`、`04-closeup-burnout-phases.png`。侧图冻结正面姿态，只检查 0.08 格偏移和示意厚度；运行朝向仍是 `CAMERA_PLANE`。线框假人只作参照，不代表 Forge 遮挡。
- 交接：同目录 `visual-handoff.md`、`manifest.json`。回执 `docs/开发记录/术式批量制作/待制作-20261031/seed-sow-v3-model-repair-20261115.md`。

建议运行贴图：`assets/xueximod/textures/effect/seed_sow_v3/seed_sow.png`，内容与上面的竖条相同。有目标时按目标碰撞高度覆盖身体；没有目标时用 1.75 格正方形。

运行路径：`src/main/resources/assets/xueximod/textures/effect/seed_sow_v3/seed_sow.png`，2776 字节，SHA256 与竖条相同。同目录 `seed_sow.png.mcmeta` 64 字节，`blur=false`、`clamp=true`，SHA256 `B2CAA414505E1C556CF68460294471D97198A50F3D69ED1FD9B717510C0D6F4A`。2026-11-11 17:32:33–17:32:59 的 `compileJava` 只作当时记录。现行 `src/main` 为 1456 个文件、13766184 字节，树 SHA256 `351eb181c4eb75b138b9370a8b2fce8caeaba71026d408a240c627f62bf408d4`，与 2026-11-15 03:25:54.673–03:26:22.708 退出码 0 的成功编译输入相同；本次归档未改 `src/main`，未重跑 Gradle。未打包，未启动游戏，待游戏实测。

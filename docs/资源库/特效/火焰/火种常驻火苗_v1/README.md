# 火种常驻火苗 v1

2026-10-30 用户查看动画预览后明确回复“确认，使用这版火苗”。批准橘红外焰、金黄内焰、浅黄焰心和循环摆动。用户另确认所有带火种实体常驻显示，每满 10 点当前余额一朵；不足 10 点为零，100 点为十朵。

- `fire_seed_flame.aseprite`：Aseprite 原生分层源，32×32、8 帧、每帧 100 ms（全循环 0.8 秒），轮廓/焰心/火星三个图层。
- `fire_seed_flame.png`：原尺寸 32×256 RGBA 纵向图集，不裁帧，透明背景。
- `fire_seed_flame.json`：帧与图层、标签导出信息。
- `fire_seed_flame-preview.gif`：最近邻 6 倍放大预览。
- 过程脚本与兼容性记录见 `../../../过程归档/火种常驻火苗-20261030/v1/`。

采用状态：二维外观已确认并归档，指定 Cursor 源码作者已接入 `src/main/resources/assets/xueximod/textures/effect/fire_seed/fire_seed_flame.png`，按 100 ms 手动选择图集帧，不叠加动画 mcmeta。运行时 PNG 与归档 SHA256 一致。2026-10-30 02:15 的统一 `compileJava --offline --no-daemon` 已通过，源码与资源共 774 文件编译前后哈希一致；未游戏实测。燎里原有 v6 特效贴图保持不变。本阶段额度已用完，返修结果未追加复审。

原尺寸 PNG SHA256：`106DD7C62A1E360C859FB598B320EBCF10A40568555C763465023380E5DE800B`。
源文件 SHA256：`7FD0B22D15A4BACD3E3BB782B13C8702A4E1C91C93E1EF6A7D3B0FF3D89FAD50`。

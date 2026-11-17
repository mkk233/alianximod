# 水幻形 · Steve 水线 v2.1（已采用）

用户 2026-11-02 确认原话：「这个版本好接入吧」。这是上一轮 v2.1 散水预览的采用，并已接入运行资源。游戏内还没实测，这次也还没跑 compileJava。

正式目录保留确认模型、两张原尺寸贴图、三张真实视口静帧和 50fps GIF。灰色参照和地面只留在 bbmodel 里，不进运行网格。99 张逐帧和制作脚本仍在过程目录，不复制进来。

- 模型：`water_phantom_steve_v2_1.bbmodel`，281867 字节，SHA256 `4ADFCC75ED1E06E62ECE5FD05E6B109787FB1AC46B29CBFC5A8AF8C5425695F7`
- 皮肤：`steve_water_lines.png`，128×128，2687 字节，SHA256 `9E7E5B01A7E09C393D3FFA1800468BD61AE919F68CF628152B00D9C347793DA5`
- 水片：`water_flakes.png`，32×32，608 字节，SHA256 `CEFDEE9E3E3FB7F701E19FC9CB3438C586C4841D3A50E3F8C9399A635BD4D70C`
- 预览：`bb_hold_three_quarter.png`、`bb_break_mid.png`、`bb_ground_drops.png`、`bb_preview_50fps.gif`
- 交接：`visual-handoff.md`
- 过程：`../../../过程归档/水幻形_Steve水线_v2_1_流畅散水-20261102/`

运行贴图与上面两张 PNG 字节相同。运行网格是 `assets/xueximod/models/effect/water_phantom_steve_v21/lines.bin`，只含六个方体、48 个短片和 20 个水滴。

# 资源库总索引待合并

本轮不改 `docs/资源库/README.md`，避免和其他批次并发覆盖。下面三条供之后合并。

## 替换「升温 · 贴身炽热 v2」条目里的「尚未替换游戏中的v1」

维持与入场分层已接入 `src/main/resources/assets/xueximod/textures/effect/furnace_v2/heat_rear.png`、`heat_rear_tongues.png`、`heat_front_tongues.png`、`heat_embers.png` 及对应 `heat_entry_*.png`。`FurnaceBodyLayer` 按手臂、躯干和背后绘制，预览与整图条不加载。冷却仍用 `textures/effect/furnace_v1/cool.png`。2026-10-31 本轮未编译、未游戏实测。

## 替换「燃烧 · 双手附焰与火焰披风 v3」条目里的「尚未替换游戏中的v1」

部件条已接入 `textures/effect/furnace_v2/burn_cape.png`、`burn_rear_fire.png`、`burn_body_fire.png`、`burn_hand_fire.png`、`burn_embers.png`，入场为 `burn_entry_*.png`。手火跟随手臂，披风贴在背后。正背预览、合成条和空的 combined 源不加载。2026-10-31 本轮未编译、未游戏实测。

## 补充「炉心三式 · 用户确认v1」

升温与燃烧的运行引用已改为上述 furnace_v2 分层。`cool.png` 仍由冷却使用。`textures/effect/furnace_v1/heat_up.png` 与 `overdrive.png` 保留，不再作为升温或燃烧的运行贴图。

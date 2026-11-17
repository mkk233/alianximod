# 幽冥摇曳接入写表回执

2026-11-16。batchId `待制作-20261031`；taskId `fire_nether_gloom_v2_domain_preview`；attemptId `20261116-record-gloom-sway-integration-01`。B：`fire_nether_gloom`。

本阶段额度已用完，返修结果未追加复审。设计 2/2、实现 3/3 不变。本次只登记已收束回执，不新增正式复审。无子代理。

## 已接收

- 归档 attempt `20261116-record-gloom-sway-adoption-01` 已关闭。
- Archimedes `01a1e46a-caa0-7d00-ba8c-1b04b467005a` 的接入与编译 attempt 均已 final 并关闭，无下属。
- 接入回执 `gloom-v2-sway-integration-20261116.md`；编译回执 `gloom-v2-sway-compile-20261116.md`，日志同名 `.log`。

## 状态

- 功能：规则未改。批准记录仍是每 20 tick 基础 5；先前只读事实记录现行代码和两份存档配置为 5。没有更晚把 20 改成 5 的批准。差异未解决。
- 美术：确认版已正式归档，12 份源文件未重写。
- 用户确认：原话「按这版接入」，绑定 `close-sway-3p2s.gif`、`inside-sway-3p2s.gif` 及同版源、PNG、模型和交接。
- 接入：运行 `assets/xueximod/textures/effect/nether_gloom_v2/nether_gloom.png` 已替换，5310 字节，SHA256 `7699297D207AC3CCACEC25E9CF7226C5B3765C0E16F620F3E1D717C1E23AD0FB`。固定根部、局部火舌摇曳、真实切片内魂谷天空和远雾已接入。
- 编译：`compileJava --offline --no-daemon`，2026-11-16 21:15:12.347–21:15:38.292，退出码 1。无关 `RoseVineProjectile.java:73` 与 `:109` 阻塞。`src/main` 前后 1629 个文件无差异，树 SHA256 `7CF4B725A17297991EF933F94DEF352B9329F59E4B6B5E326FCADC82D78CC177`。不能视为编译通过。
- 游戏：未启动，未实测。

## 表

- 提交前无锁。基线 SHA256 `A52D332FE5FB916B1BD510D61D7CDD69AE7C3094B953537028C83752B92D51DD`，191214 字节，mtime `2026-11-16 20:59:06.182`。
- 备份 `备份/gloom-v2-sway-integration-术式制作表-20261116-212244-1.xlsx`，哈希与基线相同。
- 候选 `候选/gloom-v2-sway-integration-术式制作表-20261116-212244-1.xlsx`。提交前再次核对源哈希未变，独占窗口替换。
- 提交后 SHA256 `87F22C7DCE22FD0AB83F219C1D18938AF381D2286C106A62CEBCA1A6A56E9C75`，190802 字节，mtime `2026-11-16 21:22:44.657`。候选与源表逐字节相同。
- 只改制作中第 10 行 DV，从 4082 字增至 4540 字，新 DV SHA256 `AB4429576D59777C63090E76870B15DD910433972BAEE7B0DD4C4CEE4FDCE480`。N 236 字、O 47 字、EK 74 字未改。
- 重开：已完成 37、待制作 0、制作中 21、待验收 20，合计 78，B 唯一。27 个 ZIP 条目、每页 20 条验证和 8 个合并区保留。未迁页。

未迁原因：编译退出码不是 0，且产种周期差异未解决。

## 同步文档

- 批次记录 `记录.md`。
- 资源索引 `docs/资源库/README.md`，SHA256 `6D27C78CBD154B022AA8714C423CF704504881B5D783C106C8B20ABF076D150E`。
- 正式说明 `docs/资源库/特效/火焰/幽冥_魂谷摇曳_v2/README.md`，SHA256 `AC15E83F8E225802BCDD09C0E5036231FCECEB93A7F447EF632771EB2B8FA94C`。
- 正式索引 `docs/资源库/特效/火焰/幽冥_魂谷摇曳_v2/manifest.json`，SHA256 `06E05673EEA3E7A36C87C7B29CD41BEEF2C839F86463FF5813D8ADE3CC8F71B8`。过程 manifest 副本和 12 份源文件未改。
- 实测清单 `实测清单.md`，SHA256 `D81ED92258F532F5FBE09E2EB251A374316C22671541CF076D8E7ED2C024A8D4`。用户反馈留空。
- 需求快照 `gloom-v2-sway-integration-requirements-20261116.json`，SHA256 `CB50ED287F1354DCB73FE315439FF27149C566CFB46D34B9C1CBE1A5016B483F`。

未写 Java、运行 PNG、Gradle 或其他作者文件。写入已收束。

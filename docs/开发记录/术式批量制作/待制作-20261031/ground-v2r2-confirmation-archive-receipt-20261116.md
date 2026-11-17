# 崩地 v2r2 确认归档回执

2026-11-16。batchId `待制作-20261031`；taskId `earth_break_ground_v2r2_archive_sync`；attemptId `20261116-ground-v2r2-confirm-archive-01`。B：`earth_break_ground`。

本阶段额度已用完，返修结果未追加复审。设计 2/2、实现 3/3 不变。本次只归档和登记，不新增正式复审。无子代理。

## 采用

用户原话：“这个版本可以接入”。确认版本 `earth-ground-airborne-dust-v2r2-20261116`。已批准视觉预算 1.4 秒/28 tick，不改挖坑、伤害或燃料。

正式目录 `docs/资源库/特效/大地/崩地_飞扬土尘_v2r2/`。现有分类是大地，没有土系目录，所以不新建土系，也不覆盖 `崩地_v1` 或被拒绝的坑口扬尘过程稿。183 份过程文件按字节复制，没有重画或改模型。

- 过程 manifest 副本 `来源-manifest.json`：`4AB7E17F7183A97BD3D26D9E38C682082B8365F796959A0BED5B2297E3F52E88`
- 主 Aseprite：`EE367DBB75773ED8D5FAFF7A5B1ECF1EA8C31EE69921DFB89854A6187C49C01A`
- bbmodel：`A7FF96F62EEDA989B8BDA63889D3C0D8B45250697E127C5A3A695584C060AA2C`
- 整体 GIF：`A4AF190D9DE2CD194EBDDA503506AF48A19767E4DC6074D2F9F9E02671DAF521`
- 侧视 GIF：`D7644062795DA9BB110FB210936A01B0D34724190A08405E8AC101EDF9A7251F`
- 正式索引 `manifest.json`：`6BD7CBDFDD8ACCC45AC7D38FD9B6E3D190804B4F939816CCD33654A0D25E3FDA`。它不替代过程 manifest。

## 功能缺口

已有事实：只有陨岩直接撞地 `BatchMeteorLanding` 第 50–58 行、残骸接地 `MeteoriteWreckageEntity` 第 256–266 行，在 `breakExistingOnce` 为 EXECUTED 后播放。普通及其他通用攻击没有已完成通路。

该缺口已经作为待补功能记录，并且阻碍流程第 8 节迁入待验收。运行接入和最终编译也都未完成。不能仅凭本次美术确认，或日后单独的编译成功，越过这个缺口。本轮不修、不复审。

## 表

- 来源：制作中第 11 行。提交前无锁。
- 基线 SHA256 `6C81D90F7CEFA698B2DC529E014591AC7570370BBD0D05AAC4CD9A90EDB13821`，191053 字节，mtime `2026-11-16 22:42:27.534`。
- 备份 `备份/ground-v2r2-confirm-术式制作表-20261116-225341-1.xlsx`，哈希与基线相同。
- 候选 `候选/ground-v2r2-confirm-术式制作表-20261116-225341-1.xlsx`。提交前源哈希未变，独占窗口替换。
- 提交后 SHA256 `32F28AA55F4085FE7506D366EECC250F01CAE5BD6DD5C428F7BE28A806567DEF`，191194 字节，mtime `2026-11-16 22:53:41.311`。候选与源表逐字节相同。
- 只改 DV，从 1500 字增至 1765 字，新 DV SHA256 `C7A9CFB12E087D81C46B360BAE1184A905CBAA88803CC8D9FC8165C22075D5ED`。N 236 字、O 26 字未改。
- 重开：已完成 37、待制作 0、制作中 21、待验收 20，合计 78，B 唯一。27 个 ZIP 条目、每页 20 条验证和 8 个合并区保留。未迁页。

需求快照 `ground-v2r2-confirmed-requirements-snapshot-20261116.json`，SHA256 `2D4816498C893CF326DC8DB25B6CCDE8D9D9A60321EF3F130F5F6FF222A50872`。

Goodall `01a1e4d6-42a7-7803-a050-2175fea78b68` 正在做运行接入。本回执没有读取其实时代码或回执草稿。下一步等接入和最终编译收束后，再连同上述功能缺口重新核对第 8 节。未写 Java，未跑 Gradle，未启动游戏。写入已收束。

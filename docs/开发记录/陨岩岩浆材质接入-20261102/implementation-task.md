# 陨岩 v5 正式归档与运行材质接入

本阶段：素材归档及接入实现；固定 Cursor CLI --model grok-4.7-high-fast，恢复原作者 c944df97-5698-4cb3-ae72-8adf3edc9b0a。主会话只在 sheet-prep 最终完成并停写后启动此任务。请完整读取本文件；旧任务已结束，当前授权严格以本文件为准。

用户 2026-11-02 看过 v5 的 Aseprite 和实际Blockbench预览后回复“采用这个版本”，已授权归档和接入。不要再次要求视觉确认，不重画，不改变尺寸或颜色。此前要求“火焰纹理太少了要更像岩浆块”。无孔洞、平整暗色岩壳之间成片橙红熔岩、局部亮黄。

先读 AGENTS.md、.grok/skills/max-dispatch/SKILL.md、docs/统一开发与术式批量制作流程.md 相关规则、C:/Users/baishang/.codex/skills/xueximod-art-preview/SKILL.md、本目录调度记录.md及sheet-prep-result/report。正式复核额度设计2/2、实现3/3已耗尽，不重置、不重新审逻辑、不启动独立审查。本任务只是已确认资源接入和必要作者资源检查；记录“本阶段额度已用完，返修结果未追加复审”。

当前输入：本目录快照中的最新详细需求、当前表N/O/DV只读（writer已迁制作中）；此前完整功能依据 docs/开发记录/陨岩实体命中续飞-20261101/快照/修订需求-20261102-002838-1.md。全部功能、飞行、实体命中续飞至触地、伤害、碰撞、方块照明与体积保持，不改 Java，不改配置。不通读整仓。

已确认过程稿：D:/alianximod/docs/资源库/过程归档/陨岩_熔岩岩壳_v5-20261102/
读取 manifest.json 与 visual-handoff.md。确认PNG：meteorite-magma-v5.png，32×32，1帧，全不透明，SHA256 a0f7027b8b39c4f9f3aaabb9e05713c42e5b0e86697c89d9294848a9b228e1d8；.aseprite有5层；.bbmodel SHA256 fbd11fd97791f43d95d0a36b37bc95511c34ae26108f99e0de054ed86843fd33，已在真实原生视口看过且模型嵌入与PNG字节一致。
正式目标：D:/alianximod/docs/资源库/材质/方块/陨岩_熔岩岩壳_v5/
运行目标：D:/alianximod/src/main/resources/assets/xueximod/textures/block/meteorite_v1.png

唯一文件所有权/允许写：
1. 正式目标目录（源、PNG、模型、二维/原生视口预览、README/manifest/visual-handoff，保留原字节）。
2. docs/资源库/README.md 中新增/维护本版本的独立索引条目，保留其他内容；你是本轮唯一索引作者。
3. docs/资源库/材质/方块/陨岩_v1/README.md 仅追加历史版本被新稿替代的链接，旧源不删。
4. 当前v5过程稿 README.md、manifest.json、visual-handoff.md 仅更新用户已确认与正式路径/状态，不改图片和模型。其他旧稿不动。
5. src/main/resources/assets/xueximod/textures/block/meteorite_v1.png 唯一运行写入。
6. 本任务目录 implementation-*、runtime-backup/（备份旧运行PNG）。不改制作表或writer专属文件，不改调度记录或其他会话文件。

做法：
- 保留既有未提交内容，禁止 reset/clean/checkout 覆盖；遇非预期同作者写入冲突保留现场报告。
- 先原样归档 .aseprite、原尺寸PNG、.bbmodel、texture-preview.png、bb-hero.png、bb-reverse.png、blockbench-active-view.png。制作脚本保留过程目录，不混入正式源。
- 正式文档写明用户确认日期/原话、版本、32×32静态全不透明、六面同图、一格体积、普通材质。不能把描绘的亮色说成新加真实发光、动画或照明。预览地面不进游戏。
- 创建同一确认版本manifest和接入交接，把“待确认”修正为“用户已确认、已接入待编译/游戏实测”（编译尚未授权不能写通过）。保持源与模型及预览哈希。
- 备份旧运行PNG后，原样复制确认的32×32 PNG到 meteorite_v1.png。核对图片格式/尺寸/不透明度及原稿-正式归档-运行字节一致。
- 只读检查已有 blockstates/meteorite.json、models/block/meteorite.json、models/item/meteorite.json 的运行引用；应继续 cube_all -> xueximod:block/meteorite_v1。引用本已正确就保留。不复制.bbmodel、地面、放大图到运行目录，不新增动画mcmeta。若引用异常或要改范围外文件，交回，不擅自扩大实现。
- 记录本次相关修改所有权、变更路径、SHA256及最后写入时间，必要的作者资源检查结果。无Java改动无需命名门禁。不写或跑额外测试。

编译授权：本阶段无。所有写入完成后输出 implementation-report.md、implementation-result.json、implementation-ownership.json，status=implemented_pending_compile，列出改动、运行引用、备份、最后写入、blockers=[]或真实阻塞。明确停止所有相关写入。主会话收到最终反馈后另行授权编译，不用旧compile结果。不得自行编译、打包、runClient、启动游戏、提交git。当前用户正在自行关闭原runClient，不能终止它。

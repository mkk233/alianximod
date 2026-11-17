# 热波水平正圆v2：确认版接入与外缘一致

固定 Cursor CLI --model grok-4.7-high-fast。当前阶段是已授权实现及必要作者检查，完成后停写，待调度另派最终编译。本阶段不编译、不打包、不启动或操作游戏。

## 最新用户决定（优先于历史交接）

用户已看过同版Aseprite源及真实Blockbench俯视/斜视动画，2026-11-01明确：“按这版接入，同时把范围扩展到这个特效范围”。主会话说明当前外半径已是5格、覆盖预览最大外缘，并澄清内侧需求；用户选定：“保留中心1.5格空区，只确保外缘对齐”。因此：采用 heat-circle-v2，水平世界正圆；中心1.5格空区；外半径基准5格，与视觉最大展开外缘保持一致；一次释放只结算一次伤害。没有授权改成实心盘、每帧伤害、扩大到半径10格或另添聚能机制。

该项原批已记录设计2/2、实现3/3耗尽。此次是用户明确授权的素材替换和必要范围一致性实现，不开额外正式复审，不重置配额，不调用任何子代理或另一个Cursor审查会话。主会话只调度和转达，不读中间代码、不复审。必要实现自查如实记录为作者工作，不冒称新增正式复审通过。

## 必读输入

- D:/alianximod/AGENTS.md、.grok/skills/max-dispatch/SKILL.md、docs/统一开发与术式批量制作流程.md、docs/配置平衡指南.md（只读适用规则）。
- C:/Users/baishang/.codex/skills/xueximod-art-preview/SKILL.md；已经完成同版本实际工具制作与用户确认，不重复绘制、不重复追问。
- D:/alianximod/docs/资源库/过程归档/热波_水平正圆_v2-20261101/README.md、manifest-pending.json、shock_ring_circle.aseprite、shock_ring_circle-strip.png、热波_水平正圆_v2.bbmodel及bb-top-animation.gif/bb-oblique-animation.gif。历史“待确认”已被本次用户确认覆盖，原过程快照保留。
- D:/alianximod/docs/开发记录/热波正圆精修-20261101/Cursor形状输入报告.md：上一轮Cursor只读事实，非额外正式复审。
- 当前 docs/术式制作表.xlsx 的 B=fire_shock_ring N/O/DV及对应详细需求，只读该项。不得用旧领取档案代替现行需求。

## 实施范围

1. 先核对确认版manifest，原样正式归档至 D:/alianximod/docs/资源库/特效/火焰/热波_水平正圆_v2/：可编辑Aseprite、原尺寸竖条、单帧、二维GIF、原生bbmodel、两视角实际GIF/静帧、app proof、交接参数。制作脚本/原帧可留过程目录并引用，不把草稿误写已实测。正式README/manifest记录最新用户确认原话和本项内外半径决定，保留原件及hash。
2. 将确认版64×1024、16帧PNG接入当前真实运行引用。最小变更优先，可直接替换现有 textures/effect/shock_ring_v1/shock_ring.png 并在正式映射说明文件名v1是兼容旧引用，内容版本为heat-circle-v2；也可如确有必要新增明确v2路径并仅更新SHOCK引用。不能加载GIF、灰人偶、地面网格或整个bbmodel到游戏。
3. 现有事实：BatchCarrierVisualRenderer.RING为XZ正方形，half来自实体halfSpan，U/V整帧，X/Z同尺度，Y+0.05；BatchShockRingEffect把有效外半径写入halfSpan，并用于圆环判定；默认radius5、innerRadius1.5、boxHeight3、lifetimeTicks12。依据实际当前代码做最小必要实现，确保判定与视觉载体使用同一份有效外半径，配置修改时两边同步；不要只改视觉或写死一个客户端5。区分半径与直径，不能把10×10格载体误写成半径10。
4. 源PNG本身有透明留边，特效实际可见边缘与载体方形边缘不是同一概念。记录实际同版像素最大外缘与映射关系，避免声称所有帧的可见边都在5格；这是扩散动画。用户要的是外缘一致而不是盲目增加伤害半径。如现有半径数据与映射已一致、仅像素留边/离散像素的误差，不为了制造改动增加判定范围或扩大模型，也不要擅自改已确认图形。确需padding补偿才能满足一致时，先在最终反馈中给出最小差异与依据，保留已可完成的素材接入，不未经授权重画/改变动画。
5. 保留中心1.5格空区、3格判定总高、0.6秒默认视觉、一次结算、陨岩中心排除名单、既有附魔与接续/伤害/费用/锚点/配置行为。不得按每帧扩散重复命中，不添加击退或火种消费，不新增全局聚能底座修复。本任务只热波形状及范围一致性，不顺带动其他术式。
6. 最终作者检查只覆盖本次必要内容：运行PNG与确认源hash/尺寸/帧序一致，资源引用可用，水平两轴等比，中心对齐，同一有效外半径进入命中与显示，内圈保留，配置fallback兼容，不引入辅助模型。没有游戏实测，不能报告看过游戏结果。该检查不作为新正式复审。

## 文件所有权

你是本次唯一源码与运行资源作者，相关旧会话已结束。保留所有现有未提交改动，禁止reset/clean/checkout覆盖。可读少量直接关联文件和实际本机API，不通读整仓。

允许写以下最小范围（没必要不改）：
- D:/alianximod/src/main/resources/assets/xueximod/textures/effect/shock_ring_v1/shock_ring.png
- D:/alianximod/src/main/resources/assets/xueximod/textures/effect/shock_ring_v2/（如选择新路径）
- D:/alianximod/src/main/java/com/bslx/XueXiMod/carrier/BatchVisualTexture.java
- carrier下现行 BatchShockRingEffect.java、BatchCarrierVisual.java、BatchCarrierVisualRenderer.java；若实际包位置不同先仅按对应类定位，写入前将真实路径记录ownership。仅改热波相关最小内容，不影响其他shape。
- D:/alianximod/docs/资源库/特效/火焰/热波_水平正圆_v2/
- D:/alianximod/docs/资源库/过程归档/热波正圆接入-20261101/（仅必要转换/检查记录）
- 本目录 implementation-ownership.json、implementation-report.md、implementation-result.json、index-update.md。

不写制作表、总资源索引、其他任务记录、旧正式源或当前过程确认源，不改 ListedRelease/通用链/判定服务。确有范围外必要修改先报告，不自行扩大。主会话不接管Java。

完成后保存实现报告及结构化result（status、session_id、model、approved_version、changed_paths、archive_path、runtime_path、radius_alignment实际结论与数值、checks_run、unverified、needs_compile:true、source_writes_closed:true、blockers）。为唯一writer准备index-update.md，明确旧v1保留、当前内容为新v2、待最终编译与用户实测；不要提前写编译通过。

此阶段完成全部写入后最终返回，等待另发 compile-task.md。不要询问“是否继续接入”，用户已经批准。

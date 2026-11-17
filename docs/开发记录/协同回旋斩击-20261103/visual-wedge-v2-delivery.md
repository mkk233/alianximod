# 协同斩光 v2：薄壳接入

用户已确认 `cooperative-slash-wedge-v2-20261104`。这次只把独立斜斩 emit 上的无厚度平面换成确认薄壳。伤害、寿命、相机、人物动作和两名演员的语义没有改。没有统一编译，没有游戏验证，没有新增正式复审。

## 确认哈希

| 文件 | SHA256 | 运行时 |
| --- | --- | --- |
| `docs/资源库/动画/剑术/协同斩光_v2/slash_arc_v1.png` | `58E741C8BED808EB4E8CC6DC77FA71FD009A9AAB0B7FEBC104ACCB68E655BFD7` | 已在 `assets/xueximod/textures/entity/xueximod/oblique_slash_v1.png`，本次核对哈希相同，没有改字节 |
| `cooperative_slash_wedge_v2.bbmodel` | `7659B9AEF7A526A6E8313288E4FDD4D1CEA26C06513700C9FA5DB979C606BDC3` | 游戏不加载。壳由代码按交接的五道折线生成 |
| `firstperson_v2.gif` | `5112DDF62DECFDCBFE26AB4032C090E9A3AEB0EF23C6E3897BA60B8153E7B97F` | 只作确认视口，不进资源包 |
| `cooperative_v2.gif` | `FDB8361EEFE1F1363B32E16A63D11137B1BE1627A7B164B5A342DF6B1B70C9E9` | 只作确认视口，不进资源包 |

## 网格与 UV

局部坐标仍是实体原点上的宽向 X、法线 Y、瞄准纵深 Z。不另加眼前 1.5 格。倾角用实体已同步的 `roll`，不用写死的 ±35°，也不朝向相机。

默认折线：纵深 `[-1.15, -0.75, -0.25, 0.50, 1.50]` 格，半厚 `[0, 0.08, 0.16, 0.10, 0]` 格。宽按 `width/5`，纵深按 `length/3`。半厚系数是 `min(1, width/5, length/3, height/0.5)`，再乘半厚表。逻辑厚度为 0 时，峰值半厚按 0.04 格派生，不写回伤害体积。相位 0.92、1、1、0.001 乘在整个壳上。

每个 emit 四段：上、下、左、右各 4 个四边形，共 32 个三角形。前后尖端半厚为 0，不再加盖。`entityTranslucent` 不剔除背面，每片只提交一次，避免闭壳叠成实心黑块。全亮，世界深度仍写。贴图坐标横向 0 到 1；纵向用确认视口的 `0.5 - 纵深/3`，PNG 顶部留在前方，后端裁剪不把整张图拉开。侧边沿左右边界取样。

`models/effect/cooperative_v1/slash_plane_v1.json` 只记录上述网格，`loadedByGame` 为 false。

## 渲染路径

`TechniqueHitRenderer` 原有斜斩提前返回仍调用 `CooperativeSlashPlane`。横劈术式在 `matches` 里直接排除，不使用这张壳。水幻形和空间切割文件没有改。上一轮动作返修的姿态、根偏航和第一人称手臂保留。

## 自检与未跑

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 0。运行 PNG 哈希与确认文件一致。

未统一编译。未游戏验证。设计 2/2、实现 3/3 已用完，本单没有追加复审。

## 仍未在本单修改的问题

`FamilyExecutionService` 的 `ActionPlayback.serverStartTick` 仍是服务器开机刻，客户端世界年龄不一致时动作从收包时刻播放。本单不改共享执行文件。主会话等该作者最终回执后再交回。

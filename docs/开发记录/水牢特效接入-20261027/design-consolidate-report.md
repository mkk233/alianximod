我先完整读取这份汇总说明，再按其中的范围和约束整理设计。两份只读报告都已完成。我先对照它们和已批准需求，只核对分歧点。水牢公告板只改两处：`PrisonShellRenderer.java`，以及把 `docs/资源库/过程归档/水牢泡泡_2D_v3_斜向错层-20261027/water_prison_bubble_v3_strip.png` 原样复制为 `src/main/resources/assets/xueximod/textures/entity/xueximod/water_prison_billboard.png`。目录与现有水效一样放在 `textures/entity/xueximod/`。不加 mcmeta，不新建材质类。同包 `StillSheetMesh.of` 已是半透明、深度测试、只写颜色、最近邻。同一次返回值同时用于 `getBuffer` 和 `endBatch`。每个牢只交 4 个顶点，白顶点色，`uv2` 全亮，不乘旧的蓝染色，不提交反向面。单面加上 `NO_CULL` 不会叠 alpha。

包络以快照 `bounds` 为底。它在提交时已含当时身体、0.20 外扩和聚能倍率，倍率变大则盒子变大，投影出的正方形跟着变大。不必反推倍率，也不改协议，更不把 `bounds` 改成正方形。锁定、碰撞和反弹仍用原盒。实时身体只在身份核对后并入：`level.getEntities().get(targetId)` 命中且 `isAlive()` 时，按 `event.getPartialTick()` 插值碰撞箱，与 `bounds` 取并集；人还在壳内则并集仍是壳。网络编号只作备用，`level.getEntity(id)` 的 UUID 必须等于 `targetId`，否则忽略。实体未加载时只画 `bounds`。UUID 已匹配但死亡或 `isRemoved()` 时，这一帧不画。删除包、到期、换维、登出仍只靠现有 `visible()` 清场。不改 `purge`、`bury`、`holdLocal`。

平面对准当前相机，含俯仰。用 `getLookVector`、`getUpVector`、`getLeftVector`，右方向取左的反方向，两轴半边长相同。阶段仍是 `AFTER_ENTITIES`。pose 先减相机，再用 `Matrix4f.transformPosition`；投影用 `event.getProjectionMatrix().transform`，不要用向量 `mul`。前向深度是 `(角点-相机)·look`，`clipW` 过小的角丢掉。近平面取 `m32()/(m22()-1)`，不是有限正数时用 `0.05`。平面深度 `d = max(近平面+0.05, 最近深度-0.08)`，整张面放在同一深度。边长 `hypot(投影宽×96/70, 投影高×96/74)×1.04`，宽和高都用它。相机在包络盒内，或没有留在前方的角，则不画。

`0.08` 只把水纹放到目标近侧，不能保证任意墙都被挡住。不透明遮挡靠本阶段已写入的深度和 `LEQUAL`。最近角朝相机 `0.08` 格以内的表面仍可能被这张片盖住；不写深度的水方块也挡不住。

帧用游戏时间。`age = (gameTime - birthTick) + partialTick`；小于 0 用第 0 帧，否则 `floor(age / 2)` 对 16 取模。正常 20 TPS 时每帧 100 毫秒。寿命仍只看现有 `endTick` 和 `visible()`。新局部变量避开名称门禁里的 `min`、`max`、`pos`。

允许修改的只有上述 Java 和这张 PNG。禁止改客户端状态、入口、协议、服务端、mixin 和制作表。风险：并集不会缩到比冻结壳更小；实体不在时不跟姿势；贴墙 0.08 格内可能盖住墙面；多牢透明重叠不排序。

可以按此派实现，只动这两个文件。本任务不授权现在改代码。
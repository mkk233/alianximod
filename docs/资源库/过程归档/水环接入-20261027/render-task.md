继续原会话，但唯一Astra已重新拆分调度：上一份综合实现任务现在由两位Cursor并行。你本轮只负责渲染/资源，禁止改网络/生命周期/ModEvents/ModNetwork。你的原会话已停止，本轮是唯一渲染writer。所有前期定位已经完成，不要重新通读报告、Gradle命名规则或重选架构，直接按已拍板的步骤落笔；仅补读你改的文件与必要直接调用。
另一位writer负责GuardSyncPacket/GuardStates/ModNetwork和独立客户端卸载类，公共契约已固定：
GuardSyncPacket.ClientRings.View 新增 public double animationAge(long clientGameTick, float partialTick);
单位tick，非负、刷新保持连续。你在renderer调用view.animationAge(minecraft.level.getGameTime(), partialTick)，只读既有ownerId/radius/bodyHeight/from/toward/poseStartMillis/poseTicks，不碰收包逻辑。现有姿态插值接口原样保留，不重构墙钟姿态；旋转/帧动画只用animationAge。
工作目录D:/alianximod；模型、已合并PNG、before备份及批准规则仍按implementation-task.md（你已读），但写入权限以本轮窄范围为准。
只写：client/GuardRingRenderer.java、必要新客户端网格/材质/资源重载类；assets/xueximod的新water_ring_v8 PNG/mesh JSON/.mcmeta；正式资源目录runtime/派生导出；process水环接入-20261027内导出脚本、渲染定向检查和render-report.md。禁止其他游戏代码；不要编译/启动Gradle，统一编译由主控等两份完成后再指派。
立刻实施：
1. 运行时PNG直接用任务目录water_ring_v8_atlas.png（Aseprite已验证与三源图逐像素一致、alpha只0/255）。768x640: body左列帧24高、outer中列帧40、inner右列帧40。复制到game texture与formal/runtime，原三PNG不改。
2. 从正式bbmodel精确提取3个mesh（576+96+96=768四边形），顶点减pivot[0,15,0]，原UV和贴图映射保留。禁止近似重造环、禁止人物、禁止通用BB运行时。导出确定data缓存读取，支持资源重载，不每帧IO/大量分配。
3. 三层共用当地正Y旋转（48tick整圈，12tick +X→-Z）→源静态Z3度→稳定最小旋转对齐护体法线→scale radius/11.775与原bodyHeight环心。整体96tick闭环，纹理每2tick一帧16帧，禁止额外UV滚动。不用band压扁美术。
4. 一个缓存RenderType和一次可排序透明批次，深度测试开/不写深度/no-cull/每面只提交一次，白色原色、原alpha、fullbright或unlit，NO_OVERLAY若顶点格式需要；近眼淡出保留。世界中心double先减camera再float。当前帧UV仅在全帧边界半像素内缩，不给每个quad单独缩边，不滤波不mipmap。
5. 读取任务目录blockbench-native-transform-reference.json可作为原生旋转坐标金标准（世界坐标减[0,15,0]再scale），验证导出/变换与BB一致。至少检查源网格UV、时序0/96、帧24/40映射、近眼alpha/远坐标、法线越过旧.9阈值连续。可以准备定向测试，但此时不启动Gradle。
写完立即报告render-report.md和终结本轮，交独立Grok审；不等网络writer、不编译。遇到跨模块契约问题回报主控，不改网络文件。不要再花时间做整体方案设计，方案已最终批准。

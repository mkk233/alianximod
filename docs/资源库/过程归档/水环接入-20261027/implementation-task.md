你是本任务唯一的Cursor游戏代码实现代理，模型Grok4.7HighFast。用户已经在本会话授权Cursor改代码和编译、主代理核对、不打包、游戏内用户实测；此前已授权缺失DeepSeek时使用Grok4.7HighFast。此次Cursor DeepSeek已实际调用失败：账户额度不包含该模型；Grok CLI deepseek-flash已完成两个专项只读报告，唯一GPT-6-astra已经完整读齐并做出下面最终裁决。你现在执行这些已经批准的步骤，不再另派代理或请求同样许可。任务是将用户正式确认的v8周身水环接入现有water_ring护体，保持玩法。
工作目录D:/alianximod。根AGENTS需遵守，下面是主代理已完成模型流水线后的落地任务，不需重复通读整仓。禁止打包/build/jar/reobf，禁止启动/控制Minecraft，禁止git commit/reset/revert或动未授权文件。存在大量用户未提交改动，重要原始文件备份在过程任务目录before/，按它比较本次差异。命令git只用 -c safe.directory=D:/alianximod，别更改全局配置。
任务目录：docs/资源库/过程归档/水环接入-20261027/。读取read-render-report.md与read-state-report.md只作为事实报告（部分DS提出平环/墙钟简化不采纳）；不得读取带thought的raw日志。
正式素材目录：docs/资源库/特效/水流/周身水环_细碎毛边_v8/（另一个Cursor仅正在完成此处README/manifest与总索引，暂不要修改这些文档。你可读已经复制的素材）。
素材原始过程目录：docs/资源库/过程归档/周身水环3D验证_v8_细碎水膜毛边-20261027/。
模型：周身水环_细碎水膜毛边_v8.bbmodel（含占位人物只用于预览；运行时只取三个type=mesh水体）。三张source PNG保持原样。
已由主代理通过Aseprite MCP合并运行时PNG：任务目录/water_ring_v8_atlas.png，768x640，左x0 body/broken_saw_flow(256x384,16帧每帧24高)，中x256 outer/ragged_splash_outer(256x640,每帧40高)，右x512 inner/ragged_splash_inner(同外缘)。alpha已实测仅0和255。直接复制采用这个图，不自行重新画图，不导入示意人物。可将派生图与网格运行时导出复制正式目录的runtime/，但此处单独小目录不动现有README/manifest。
允许修改：
- client/GuardRingRenderer.java。
- 必要客户端专用网格/布局/材质小类、导出确定网格json（不写通用BB runtime loader，不重新凭圆环半径拟合）。可在process任务目录写导出/验证脚本。
- network/GuardSyncPacket.java；shu/technique/release/guard/GuardStates.java（仅构包年龄、补快照当前姿态、删除通知目的地，不改玩法状态机）。
- network/ModNetwork.java协议1→2双端严格版本匹配。
- event/ModEvents.java或独立client lifecycle class，补客户端世界卸载清理。
- Runtime新PNG/mesh JSON及.mcmeta关闭过滤/mipmap，限定在assets/xueximod。
- 必要定向测试src/test，过程任务目录的实现报告、编译日志；不动其他功能文件。
已确认现有入口：WaterTechniques → GuardDefinition.personalRing → GuardStates → GuardSyncPacket.ClientRings → GuardRingRenderer。
已确认追踪和离场钩子：shu/technique/release/momentum/ReleaseBaseEvents.java:204-210已有StartTracking调用sendSnapshot；159/166死亡/登出/换维经endCast→closeOwned。不要重复新增注册，原文件原则上不改。
最终实现约束：
1. 从批准模型准确导出3mesh顶点/原始UV/面及必要层级变换为确定runtime数据，不含占位人物、不引入通用模型加载框架。中心pivot[0,15,0]去除展示位置偏移，保留源3°静态Z倾斜和变换次序：当地Ry旋转 → 外层Rz(3°) → 整体+Y对齐现有插值法线 → 按实际环心放置。所有3层完全相同变换。
2. 统一scale=view.radius/11.775，11.775=(10.3+13.25)/2；不按band压扁美术。band保持玩法碰撞原值，不扩大防御体积。姿态对齐采用稳定最小旋转基/四元数，避免旧helper在abs(normal.y)=.9切换引发跳相。保持近眼/第一人称局部淡出。
3. 共根48tick旋转360°，俯视CCW，基准+X在12tick后变-Z（原模型正Y旋转）。贴图每2tick切帧，16帧循环，整体96tick闭环。禁止另外全周UV滚动。法线倾斜时所有层一起绕当地轴转，不固定世界Y。源model是96段body576faces+outer96+inner96，应一致提取，总768quads。
4. Wire只新增animationAgeTicks=服务端发送时now-animationStart；既有animationStart保留数值/语义用于动画身份。客户端收到年龄+本地游戏tick增量+partial推进。刷新revision、重复FULL/POSE不能归零或倒退；新观察者从当前年龄起播；不得用server.getTickCount直接减client.getGameTime，更不得用墙钟起圈。保留/完善revision/sequence门控，不让旧包覆盖新状态。
5. FULL补快照from取服务端当前插值法线，toward保留当前目标，poseTicks取剩余过渡tick；已完成则from/toward相同。避免新观察者重播过时姿态，无需新增wire字段，不改变服务端姿态状态机。
6. 保留已存在死亡/登出/换维关闭机制；客户端level unload清ClientRings。broadcastRemove除当前owner维度，向BodyGuard保存的旧serverLevel观察者补发删除。REMOVE携带真实animationStart，客户端只删匹配动画且revision不落后的view，避免旧通知删新环。不要因此重做生命周期。
7. 同一个缓存RenderType使用合并图，3mesh进入同一个可排序透明批次；深度测试开、无深度写入、无剔除，每面只提交一次。删除旧正反面重复画法。顶点RGB白色，素材原alpha，局部近眼淡出；不沿用原(90,180,220,170)染色。显示原色/fullbright；可用合适无光照材质，若实体shader则NO_OVERLAY/FULL_BRIGHT。不新造非必要shader，source只有0/255 alpha；如用alpha cutoff需确认近眼衰减合理。
8. UV用源逻辑宽256、各部分源帧高24/40/40映射至合并图，U/V限制在整张分图当前帧半像素中心边界（不是每个quad都各缩边导致接缝）。不滤波不mipmap。资源reload应能重读新data，避免每帧解析JSON或分配大量向量。
9. 世界中心与相机坐标先以double相减，之后才转float，保证远坐标正确。渲染不能带入BB展示人物、展示相机、固定绝对世界坐标。
验证与交付：
- 在过程任务目录写独立且有意义的定向检查或测试，验证精确mesh/UV与源对应、3层同变换、0/96tick一致、48tick旋转与32tick纹理、远坐标、帧边界和法线越过旧.9阈值；packet编解码、刷新不断相、中途追踪、快照姿态不重播、旧REMOVE不删新环、世界清理。能纯计算测试则用纯计算，不能在无游戏运行情况下验证的不要谎报。
- Cursor自行执行项目根 .\gradlew.bat compileJava --offline --no-daemon；必要的定向测试/资源处理可运行，绝对不打包。如当前环境编译失败先诊断本次相关问题，别擅自大改项目。将完整编译命令和结果保存任务目录/compile.log。
- 每完成代码请立即写任务目录/implementation-report.md：本次文件、核心行为、验证证据与未实测项，向主代理报告。不要修改全库README、正式asset_manifest，它们由归档任务暂独占；需要补的接入描述写到本报告，之后主代理协调。
- 如自动审批拒绝，停止被拒动作并明确具体工具与理由，不换壳绕过。可以安全完成其他允许工作。
这是获准视觉替换与必要同步修正，不改变持续时间/减伤/魔力/偏转/接触附魔玩法。请开始实际实现并编译。

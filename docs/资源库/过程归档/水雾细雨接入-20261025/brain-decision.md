# 水雾细雨接入裁决

日期：2026-10-25。仅为实施调度文档；未修改游戏代码。执行方为用户指定的 Cursor 工作流。

## 已核对的事实

- `client/DomainWorldRenderer.java` 在 AFTER_TRANSLUCENT_BLOCKS 遍历 DomainSightStore。PRESSURE 分支调用 DomainPressureRenderer；其余分支绘制六面的半透明水纹盒子，尚无独立雨丝层。现有盒子作为雾气表现保留。
- `release/domain/DomainDefinition.java` 的 mistRain 使用默认 VOLUME，范围参数为 7×7×8，持续 200 tick。当前定义仅有 mistRain 与显式 PRESSURE 的 deepPressure。不得把默认分支误称为已经存在完整体积雾系统。
- `DomainInstance` 记录 visualOrigin 和 visualEnd；`DomainSightStore.Sight` 已提供 bounds、visualOrigin、visualEnd、animSeed，移除包会删除快照。无需新增网络字段或服务器计时。
- 素材 README 确认正式 PNG 为 64×2048，32 个 64×64 帧，每帧 1 tick；alpha 为 0、46、99、168。单元平移 (+1,+6)，方向相对水平约 80.54°，32 帧循环。

## 最小实施范围

1. 新增客户端 `DomainMistRainRenderer`，由 DomainWorldRenderer 在现有 VOLUME 盒子绘制完成后调用；PRESSURE 保持原分支。当前 VOLUME 唯一实际定义为 mistRain，因此本次无需新增枚举、包字段或改 DomainDefinition。入口必须显式判断 VOLUME，不能向所有非 PRESSURE 的未来视觉类型自动添加雨。
2. 将确认的 `docs/资源库/特效/水流/水雾细雨80度_v1/mist_rain_80.png` 原样复制到 `src/main/resources/assets/xueximod/textures/domain/mist_rain_80.png`。不复制 GIF、预览底色或 PSD。独立纹理显式选帧，不依赖 animation mcmeta；最近邻、关闭模糊和 mipmap，可使用仅声明 texture 设置的 mcmeta。
3. 保留原六面水纹盒子及透明度；雨是独立叠加层，不以雨替换雾。禁止修改费用、范围、持续时间、领域原生效果、边界、伤害、水刃和一分为三。
4. 更新素材 README 与 docs/资源库/README.md 的采用状态和游戏引用路径，写明本次只编译、未游戏实测。

## 时间与 UV

- 复用客户端世界 gameTime 与 sight.visualOrigin，frame=floorMod(gameTime-visualOrigin+稳定相位,32)。相位来自 animSeed 和固定雨面序号，不能来自逐帧随机数或刷新次数。正常20 TPS下即20fps；世界暂停时动画随之暂停。
- gameTime < visualOrigin 或 gameTime >= visualEnd 时不画新增雨；快照移除后自然停止。不要增加客户端残留实体，也不擅自清理服务端领域。
- 帧内 UV 为 u=localU，v=(frame+localV)/32，localU/localV限制在当前格的0..1。画面顶部取localV=0，底部取1，保证雨向下落。
- 不允许把v扩大到0..领域高度来重复贴图，那会采到其他帧。空间重复必须拆成独立格子，每格重新映射同一帧；边缘不足一格时同时裁几何与对应localUV。

## 雨面布局与角度

- 雨应分布于盒子内部，而非只贴外墙。采用少量固定的世界竖直雨幕，沿X-Y与Z-Y两组交叉布置，避免单组雨面侧视完全消失；各组固定朝向、稳定相位，禁止随帧随机摆动或上下翻转。两组只是覆盖不同视角的轻量视觉层，不宣称为统一物理风场。
- 每个完整纹理格在世界中保持正方形，例如2×2格；宽高拉伸会改变80.54°雨角，禁止把64×64整帧直接拉成7×8墙面。边界裁剪不得重新拉满UV。
- 以bounds约束全部雨幕和边缘格，维持世界Y轴竖直，不使用随相机俯仰翻倒的全向billboard。屏幕投影角随观察方位变化属正常，目标是雨面内相对真实地面的80.54°。
- 使用现有透明实体渲染路径或等效的双面透明渲染类型，顶点白色保留原色和源alpha；不能用cutout丢弃46/99低alpha雨丝。不要叠画两个同位置反向面来加深透明度，优先单份无剔除面。
- 雨层透明度不要再乘原雾盒子的70/255，避免几乎不可见；需要降低叠加密度时调整雨幕数量或单独雨层alpha。
- 使用固定少量雨幕与格子，不为每条雨丝生成实体或每帧分配随机粒子。可沿用DomainPressureRenderer的48/64格距离分级思路，但不得修改其渲染。远处减少内部雨幕，64格外不画新增细雨。
- 雾、雨、重压各自获取并结束对应RenderType批次。避免跨getBuffer切换后继续使用可能失效的旧VertexConsumer引用；不要调用无参数endBatch清空其他系统全部缓冲。

## 验证与交付边界

1. 检查复制前后PNG哈希相同、尺寸64×2048、alpha保留、32帧UV不串帧；frame31到0循环，视觉起止受visualOrigin/visualEnd约束。
2. 代码核查标准格世界宽高相等、上下UV正确、边缘格裁剪、双面与透明混合、三种源alpha保留，以及PRESSURE分支不变。
3. Cursor执行 `gradlew.bat compileJava --offline --no-daemon`。若仍为既有插件缓存缺失，可联网执行一次 `gradlew.bat compileJava --no-daemon`，不修无关Gradle配置、不升级插件、不清缓存。失败必须如实记录；javac局部验证不能冒充Gradle成功。
4. 本任务明确不打包、不启动游戏、不进行游戏内实测。完成报告分别列出编译结果和未验证事项；实际雾中可见度、内部观看层叠、透明排序及不同视角的观感保留为后续实测项目。

## 建议改动清单

- client/DomainWorldRenderer.java（仅新增VOLUME雨层调度及相应批次结束）
- client/DomainMistRainRenderer.java（新增）
- assets/xueximod/textures/domain/mist_rain_80.png（原样复制，必要时附texture设置mcmeta）
- 两份资源库说明/索引

以上Java路径均相对 `src/main/java/com/bslx/XueXiMod/`；assets路径相对 `src/main/resources/`。实现中发现需要扩大到领域玩法或网络协议时应停止该扩展并回报，不自行处理。

## 核对后的修正裁决

原先先雾后雨忽略了 entityTranslucent 的深度写入。必须为本次雾盒和雨层设置专用透明 RenderType，沿用实体透明格式、shader、光照/overlay、混合、无剔除，改为 COLOR_WRITE，保留 LEQUAL 深度测试。两层不写深度，避免雾壳和交叉雨幕遮掉后续半透明层；不能关闭深度测试导致穿墙，也不能用全局 depthMask 临时开关绕过缓冲状态。雾盒几何、颜色与 alpha 不变，重压不改，保持具体类型批次结束。精确跨材质透明排序留用户实测。

编译缺参当前已由其他后续写入修复：ReleaseDefinitions.java:23 末尾现为20f,26,6,false。禁止本任务修改该文件或恢复基线。修复上述透明渲染后，由同一Cursor重新compileJava --offline --no-daemon，缓存必要时联网。不得打包或实测游戏。更新报告，不能沿用早先的编译失败状态。

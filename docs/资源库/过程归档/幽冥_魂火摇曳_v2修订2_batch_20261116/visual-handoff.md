# 幽冥 v2 固定根部、局部火舌摇曳候选 — sway04

- batchId：待制作-20261031；taskId：fire_nether_gloom_v2_domain_preview；attemptId：20261116-art-gloom-sway-04。
- 作者 Avicenna；原生代理 ID：01a1e3a5-bf00-7532-978b-a0d7284fcc0f（父方提供）；直接父方为本次续派主会话，实际 ID 未暴露，记未知。
- 沿用父方记录：Carbon、Argon 两次实际 spawn 均为 `Unknown model`，无代理 ID/产物，按11.1降级请求 `gpt-6-astra` / `max`。本次不重新派代理、不切换服务；实际服务后端未独立暴露。
- **本阶段额度已用完，返修结果未追加复审。** 设计2/2，实现3/3不变。本次是美术返修，不是正式复审。
- 状态：**candidate / 待用户同版本确认 / 未接入**。不能凭本回执迁待验收。

## 本次修改与保留

用户准确原话：「火焰特效一直在变化大小的这个效果有点蠢幽冥的火焰应该摇曳而不是变大」。

输入是上一版魂谷天空危险魂火候选，不采用v3；完整历史N/O/DV及批准覆盖见本目录 `需求快照-row10.json`。approved-v1与最新反馈优先于历史N的旧限制，未改玩法、产种、范围或施法规则。

本次在Aseprite内复用上一版四种青蓝颜色和v2局部形状，冻结0基像素行44–63作为根部；上部四个16像素横向区域使用原16帧的0/5/9/13错相，在高度权重下以2像素步长做最大4像素横移，保留局部断裂/重生。不是整幅平移、整簇摇头或统一缩放。16帧根部RGBA完全一致，四种可见RGBA不变；局部火舌最高点会变化，但整簇几何包络、根部和所有变换固定。

在真实Blockbench删除49个WISP的全部49条scale关键帧，未补rotation/position动画。逐帧记录49组scale始终[1,1,1]，position/rotation也保持一致。全部273个元素几何、组参数及父子层级与输入相同（GUI树展开状态isOpen会变化），49簇位置、固定yaw、UV、交叉面和天空远景代理未改；8枚旧余烬保留其独立生命周期动画，不代替地火缩放。近景暂时隐藏其他簇与余烬，仅为观察单簇，取帧后全部恢复。

## 同版文件与工具真实性

- 二维源：`youming_v2_sway_fixed_root.aseprite`，64×64、16帧、5图层（固定根部+4个局部火舌），100ms/帧，1.6s循环。
- 原尺寸透明导出：`textures/youming_v2_sway_strip.png`，64×1024竖条，帧从上到下0–15；`sway-2d-native.gif`与最近邻4倍 `sway-2d-4x.gif`。
- **真实Aseprite1.3.17官方MCP backend=batch制作和复开，ui=false；不是GUI绘画。** 仅读明确前版源，`save=false / mutates=false`避免写回输入，在内存制作后另存本目录。
- 首次批处理脚本用dofile访问不到wrapper局部sprite，出现nil错误；改成loadfile后显式传入sprite，仅修正一次，随后制作成功、独立复开成功。没有重试live、重启或关闭实例。
- 可编辑真实GUI工程：`youming-v2-fixed-root-sway-candidate.bbmodel`。Blockbench5.2.2 / plugin1.10.1，原生Codecs.project.write保存，Codecs.project.load复开；创建UUID `e88e38bc-306f-4e1c-b4de-8a15f93dadd2`，复开UUID `bd0ab5c3-e2ff-a2d3-b619-3ba215a682d2`。
- 实际主视口main原生Screencam同步取帧：`inside-sway-3p2s.gif`、`close-sway-3p2s.gif`，各64张PNG、20fps采样、3.2s（两轮贴图循环）。近景相邻重复帧由GIF编码合并为32帧，每帧100ms，总时长仍3200ms；全景含余烬运动为64帧。只做GIF调色板编码，未离线渲染、过滤、拼贴或合成视口。
- PNG：`01-inside-sway.png`、`02-close-fixed-root.png`、`03-side-person-height.png`。已逐张view_image，并查看两个最终GIF、二维GIF及近景关键帧；动态帧去重和根部不变另有数值证据。
- 复现：先在真实GUI打开本bbmodel，再经MCP执行 `evidence/restore-preview.native.js` 恢复全亮材质与固定镜头；`evidence/capture-sway.native.js`定义原生取帧函数，inside/close/side为本次视角。脚本只针对本候选名，写入本目录。逐帧时间、纹理相位、全部WISP变换见 `evidence/capture-*.json`。
- 源/导出/工程/预览的SHA256统一列于 `manifest.json`；最终GUI保存完成、Undo无未闭合事务，24个原项目全部保留且saved标志未变。

## 坐标、挂点、尺寸与材质

- Blockbench适用：空间交叉面、固定朝向、挂点、身高与动态必须同时判断。
- 16模型单位=1格；完整X宽7/Z深7/Y高8，脚点XZ±3.5、Y0至+8；不是半径7。只在底面铺49簇代表样本，不向高8体积分层，不沿用旧84簇示意。
- 根部挂点世界minY+0.15格（模型Y2.4），不跟随镜头，不随人物摆动；父WISP组固定origin及yaw，下挂两张零厚度XY/YZ竖直交叉面。
- 保持上版基础宽0.6486–0.9964格、高0.58–1.2296格；没有再次套用比例或扩大几何。透明边界实际可见火焰小于平面矩形。
- UV每面0..64、0..64，纹理64×1024/16帧，保留双面渲染、最近邻像素。全亮预览，材质alpha210/255；`preview_alpha210.png`只是GUI预览透明度派生，运行源PNG仍保留原RGBA透明度。
- 簇间帧相位0/4/8/12；Blockbench原生custom帧序列有前置15/3/7/11以对应其时钟的首帧规则，继承原项目，不改变运行时建议相位。
- 参照地砖1格、人物1.8格；第一人称镜头(0,1.62,2.625)格，FOV68，朝(0,1.375,-3.125)格；侧面检查隐藏天空与远景，黑背景仅该检查视角，不是天空候选。
- 预览3.2秒/20fps只演示摇曳，**不是施法时长**；批准默认200tick=10s未改。

## 仍未实现与接入边界

魂谷暗青蓝天空/青雾远景保留旧候选原生几何与材质代理，不是领域边界蓝墙，不代表修改外部全世界天空。此前0.6s进入、0.8s退出smoothstep是主会话建议及旧版原生色调示意，本次不重做旧过渡；不得把旧GIF中的旧火焰当成新摇曳版。新全景仅展示持续段。

当前Forge无ViewportEvent/FogRenderer/skyColor接入。实际视点包含应使用NetherDomainSight.probe/Sight.covers逐片[min,max)，不能以外包bounds判断洞；NetherDomainTint.gloom可提供布尔入口。维度/visualEnd、removal与无owner存活字段等限制仍在；当前运行Wisps到期停画、没有淡出。本次未运行体积判定、跨边界、退出余辉、到期熄灭或Forge实测。运行代码原整簇脉冲尚未修改，本次取消的是**候选BB工程**内的曲线。

待用户确认同版后，由唯一Grok按确认版接入纹理与取消运行时整簇宽高脉冲，天空/雾需另作获准视觉接入；不可直接把bbmodel、假人、地砖、天空壳和远景块复制为运行资源。产种周期5/批准20的已知差异不属本次美术，未调整。

## 写入隔离与收束

旧sway03 Lua只读取魂谷源并向 `幽冥_魂火摇曳_v2修订2_20261116` 写，旧超时请求是否晚到未独立证实。本次另用带`_batch_`的新目录，显式输出不重叠；未复用旧未知产物、未覆盖旧脚本或阻塞历史。前版目录文件哈希与运行v2 PNG/Java核对见输入保护证据。

仅写本目录和独立 `gloom-v2-sway-batch-preview-20261116.md`。未写src/Java/制作表/正式资源/共享索引/旧候选；未编译、打包或启动游戏；没有子代理。美术候选制作完成，用户确认、正式接入及Forge效果未完成。写入收束后直接向本次父方交final；父ID未知，不向其他聊天发消息。

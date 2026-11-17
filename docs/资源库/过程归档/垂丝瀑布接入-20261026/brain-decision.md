# 垂丝瀑布接入裁决

2026-10-26。Cursor grok-4.7-high-fast负责代码和compileJava，我方核对；不打包、不游戏测试。本裁决没有修改游戏代码。

## 当前代码事实

- ModTechniques注册“垂丝”为water_weeping_arc，使用WeepingCurtainEffect。基础spec.width=0.5、length=0.35、height=0.4；FUNCTION路径，functionId=WeepingCurtain.FUNCTION_ID；visual为普通7或重水8。
- ReleaseDefinitions当前该术式声明energyScalesLength/Width=true，Height/Model=false。SkillSegmentRunner合并能量到CastFrame；TechniqueHitSpec.spawn统一按frame.volumeScale放大允许的尺寸。因此渲染只读entity.width()/length()/height()，不得检查是否含rule_gather_energy再重复乘聚能倍率。
- WeepingCurtain.fit依照地面与时序改变水段中心及高度，并保持实际宽度，用width/0.5推导厚度；原起流、供水、排空、落地粒子及碰撞都应保留。
- TechniqueHitRenderer的visual7/8进入WaterCurtainRenderer。当前使用shulker/spark，画主面和交叉侧面、平移UV、手动蓝色染色，不是新瀑布素材。

## 用户语义

无聚能按既有基础宽度显示最细流水；有聚能从出现时就用结算后的实际宽度，并一直向下流动。不播放从窄到宽，不改变伤害或水段生长/排空逻辑。禁止采用waterfall_charge作为游戏循环。

## 素材处理先决条件

建议先用已存在的Photoshop waterfall(f,12)补绘固定12px宽的16帧循环waterfall_thin，不能取charge的一帧静止放大。保留原flow48宽和原8/16/8分件。

当前左右分件各8px，按12px基准布局必然容不下，不能重叠边缘或把整个48px水体压成12px解决。推荐正式统一方案：以固定窄态为依据，在PS补制左右各4px的窄水边及可重复中体，组成最细4+4+4=12px；中体需要可按整数/分数段横向延展，波纹与左右接缝逐帧对齐。若直接从thin提取边缘会切断轮廓，则修补接缝再导出，不能仅盲裁。所有分件仍16帧、100ms，相位一致。主代理可先补thin供视觉确认，Cursor在合格分件落盘前不要自行修改像素或编造不存在的资源路径。

统一4px边缘方案比按聚能状态切换两套贴图更稳：宽度任意变化时只增加中体，不发生某个倍率突然换轮廓。thin作为最细完整组合参考，flow作为宽态参考，二者不叠画。

如本轮仅补thin且暂不补小边缘，备选是按最终几何宽度选择窄态完整贴图/宽态分件，并清楚规定接缝过渡；不得按术式id或聚能是否存在硬编码。该备选视觉连续性较差，不作为首选交付。

## 渲染最小方案

1. 只替换WaterCurtainRenderer的材质和几何分件方式，保留visual7/8分派、实体数量、动作和碰撞。不要改ReleaseDefinitions、聚能算法、WeepingCurtain.fit或Tally。
2. 以基础0.5格对应12px建立固定横向密度24px/格：4px边缘各1/6格；最终宽度W来自entity.width，左右边贴W/2边界，中部宽W-1/3格。通常W>=0.5；异常窄宽应安全退回固定thin完整图，不允许负中段、无限循环或左右交叠。
3. 中部按素材原生横向周期重复，最后不足一段时裁几何及UV，不拉胖整幅纵向流线。所有分件只画对应当前帧。纵向保持固定世界纹理密度，不把128px一帧随动态水段高度不停拉伸；通过分段/裁剪映射覆盖当前entity.height。
4. 时间仅控制流水：16帧每2tick循环，frame=floor((统一时间+稳定相位)/2)%16。当前素材有两个相同的0.8秒子循环亦可原样保留，不擅自缩减帧数。左右中三件时间源相同；不要让宽度改变重置流水。
5. 可沿用实体客户端tickCount计算循环（无需新增服务器激活协议）；不以每帧渲染次数计时。V=(frame+localV)/16，localV顶部0、底部1，垂直重复必须分段，不把V伸到其他动画帧。上下可拼接且逐帧真正向下，不沿用旧负UV滚动再叠加造成双倍速度。
6. 位置和主面朝向沿用现有entity中心/yaw；动态高度仍来自已同步盒子。交叉侧面若保留，需按其实际length独立安全布局，不能把宽主面分件强塞窄侧面。可仅保留主面双面可见来避免双层水片，厚度观感改变须在报告说明，不动碰撞厚度。
7. 新水色使用白色顶点及OverlayTexture.NO_OVERLAY；普通不得再次乘旧蓝色。重水可保留轻微独立色调差异，但不能造成粉色或遮掉白沫。透明混合、正常深度测试、COLOR_WRITE、NO_CULL，一份面不重复正反画；RenderType静态复用。

## 验证与交付

- 先核对PS新增thin及正式分件尺寸、帧数、接缝与最细完整预览；宽度12/24/48px组合中边缘不重叠，中体不拉胖，原素材保留。
- 检查基础宽0.5与现有聚能产生的数个实际宽度：出现即目标宽度、时间变化不再扩宽，连续流水，宽度只应用一次；左右中帧同步，frame15到0正确。
- 保持普通/重水、地面截断、起流/排空，以及原一分为三生成行为；不要额外spawn或改伤害次数。
- 游戏资源原样复制确认文件，更新README与总索引，明确charge只为美术演示、不用于游戏。
- Cursor使用既有可用Gradle缓存compileJava --offline --no-daemon；不打包不游戏测试，实测缺项如实报告。

# 协同、回旋、斩击 v1：确认版

Blockbench 适用性：适用。双人站位、朝向、持剑挂点、倾斜斩光与时间配合均涉及空间关系。

状态：2026-11-03用户明确回复“采用这版，继续接入”，确认并授权游戏接入。版本标识cooperative-slash-visual-v1-aligned。客户端和共享链已接入，最终离线compileJava与processResources均通过，批准PNG/动作JSON/语言资源已核对运行输出。最后返修未追加复审，尚未游戏验证或打包。下方原生空间参数为确认时冻结交接。

## 同版文件

- 二维源：`slash_arc_v1.aseprite`，128×128，单帧 RGBA。
- 原尺寸透明导出：`slash_arc_v1.png`，SHA256 `58E741C8BED808EB4E8CC6DC77FA71FD009A9AAB0B7FEBC104ACCB68E655BFD7`。
- 原生工程：`cooperative_slash_v1.bbmodel`，SHA256 `4021566A08D74C34E761ADBDF9EBB96E56638B836F2C660224020B066A09DD7A`。
- 实际确认预览：`solo_aligned_v1.gif`（21帧）、`spin_aligned_v1.gif`（31帧）、`cooperative_aligned_v1.gif`（36帧）。均来自真实Blockbench offscreen视口`combo_preview_view`，原生PNG保留在`../../../过程归档/协同回旋斩击-v1-20261103/aligned_native_frames/`。
- 预览SHA256依次为`B51EB79FFD66DBFEEF5FCC4E43806646A31B04CB24EDF02DFFD6D36534CC461E`、`AD023E06D437DE97D9492DC42A9E7234BD4E9A32E0CC788816F8D3D797649EA8`、`07CF2D86508B6E52F3B50CEADBA0513F25F5588752BFCFF1B6E82ACF72E8F192`。
- Aseprite只负责把原生PNG逐帧加中灰背景并组装GIF，另存同名`.aseprite`；没有另造渲染器。动作段50fps，只有末帧额外停0.65秒便于观看，不是额外施法硬直。终帧在动画结束前1ms取样，防止Blockbench once时间线结束时回到编辑姿态；运行期结束应明确清姿态、清斩光。
- 过程目录较早的`*_native_preview.gif`和`native_frames/`是未对齐的过程稿，不是本次确认版本，不得据此接入。
- 参考：`../../../过程归档/协同回旋斩击-v1-20261103/参考/视频30至37秒关键帧.jpg`，只供动作取向，不是可加载素材。

## 当前示意参数

- 原生工程UUID：`1c240b49-05eb-14a6-d3fd-ead56de5455b`；独立新工程，未覆盖其它Blockbench项目。
- Y向上，假人面向-Z，X为左右；Steve宽臂、头8×8×8、身8×12×4、臂4×12×4、腿4×12×4。模型地面Y=0，头顶Y=32。
- 本版示意按16模型单位对应1游戏格绘制，以标准Steve立方体部件为尺；这不是所有Blockbench资源的通用比例。接入者须核对实际PlayerModel的渲染变换，维持标准Steve比例；碰撞盒不随骨骼姿态改变，逻辑锚点实际读取演员眼点，不能把示意眼高26/16硬编码为所有姿态眼高。
- 根组`player_root`固定位置；`clone_root`只在开头0.10秒从[23,0,7]归位至[20,0,-8]，对应侧向1.25格、前向0.5格；之后不作绕行或穿身交换。运行时位置由安全规则求解，不照抄起始坐标；两刻内无法完成即回退单人。
- 每人有body/head/arm_r/arm_l/leg_r/leg_l独立组；剑组位于各自arm_r下，握柄枢轴[-6,13,0]。剑、皮肤与地面都是示意辅助。
- 斩光是独立攻击的两面透明平面：原始平面[-40,2,-24]至[40,50,-24]，枢轴[0,26,-24]。以16单位/格换算，覆盖载体宽5、深3格，中心在示意眼点前1.5格。薄盒命中厚0.5格是服务端体积，透明平面自身没有0.5格实心厚度。
- 攻击平面转X=-90°再绕前向倾斜，玩家Z=+35°、分身Z=-35°；分身另平移[20,0,-8]。运行时应以aim/right/up构造相同面，不能原封不动把Blockbench Euler当Forge坐标。正对刀面边缘的角度视觉会变窄，非镜头公告板。
- `slash_arc_plane_v1`是当前BB内实际使用的纹理，字节对应`slash_arc_v1.png`，全图UV、双面、emissive；黑色主体与冷白边缘，透明区域不填色。旧BB内缓存纹理已移除，不能引用旧侧弧缓存。
- 单独斩击动画0.40秒，0秒立即出刀，从切入姿态开始，8刻收势。回旋动画0.60秒，0.30秒即第6刻出刀，6刻收势。协同演示0.70秒包含开头最多2刻归位，正式回旋从0.10秒开始，0.40秒出刀；运行时归位已就绪可提前开始，不强制等满两刻。斩光从实际出刀刻开始，维持4刻可视窗口，尾部收束。客户端用服务器冻结时间推进，不使用GIF的停留时长。
- 动画名：`animation.preview_solo`、`animation.preview_spin`、`animation.preview_cooperative`。回旋只含角色动作；斩光轨道用于示意攻击触发，不能导入回旋动作本体当成动作自带伤害或特效。
- 运行资源计划：动作`actions/cooperative_v1/oblique_slash.json`和`spin.json`（客户端提取演员骨骼，协同去掉0.10秒归位段后与spin共用时间轴），斩光`textures/entity/xueximod/oblique_slash_v1.png`取同版原尺寸PNG。实际路径由V/I唯一作者登记并记录映射，用户现已确认，交固定Cursor接入并记录实际映射。

## 客户端实际交付映射

- V最终回执见[客户端交付说明](../../../../开发记录/协同回旋斩击-20261103/visual-delivery.md)。这份回执只代表客户端作者完成，不代表完整功能或游戏验收。
- `src/main/resources/assets/xueximod/actions/cooperative_v1/oblique_slash.json`和`spin.json`已按批准骨骼轨道转换；normal对应player，mirrored对应clone。正式回旋不再叠加预览归位平移。
- `src/main/resources/assets/xueximod/textures/entity/xueximod/oblique_slash_v1.png`已写入；最终编译任务已确认src和build/resources/main的SHA256均与批准PNG一致。
- `models/effect/cooperative_v1/slash_plane_v1.json`为平面/相位数据记录，作者说明游戏不加载它。斩光实际由TechniqueHitRenderer的本批接点随攻击实体渲染。
- 原生模型和六份确认资源内容未变。源码编译和资源输出同步已经完成，游戏效果待用户实测。

最终验证记录见[编译与资源同步报告](../../../../开发记录/协同回旋斩击-20261103/compile-final-report.md)：两条Gradle命令均实际执行，编译0错误、45条removal警告，1377份src文件在编译窗口前后无内容变化。动作与语言文件输出也与src一致。[实测说明](../../../../开发记录/协同回旋斩击-20261103/游戏实测说明.md)包含单斩、回旋、普通分身协同、冲刺接续及取消/费用观察项；这些游戏观察尚未验证。

## 接入边界

- 本工程全部player/clone假人、钢剑占位、preview参考地面留在示意中；不得把整个.bbmodel加载为游戏分身。
- 普通分身在这里用青灰色假人区别玩家，只是示意；不更换既有水幻形已确认的Steve水线资源，不回退其皮肤/散水缓冲分开提交的修复。
- 游戏仍须独立处理服务器时间、受击、复制份数、费用、冷却、权限、安全落点、相机及配置。Blockbench预览不能证明这些机制已正确执行，也不能代表Forge透明排序、光照、网络时序、第一人称或各光影模组的实际效果。
- 本次确认绑定本目录六个文件及本交接。转换/接入由固定Cursor执行；若修改形状、比例、位置、方向或动画观感，受影响部分须重新展示确认。正式源与过程帧均保留。


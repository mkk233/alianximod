模型：Cursor CLI --model grok-4.7-high-fast，禁止换模型或降档。
当前阶段：只读定位。仅收集事实与提出方案，禁止修改任何文件（包含报告）、编译、打包、启动游戏、改制作表、提交。直接在最终答复交付完整报告。
遵循 D:/alianximod/AGENTS.md、.grok/skills/max-dispatch/SKILL.md 和 docs/统一开发与术式批量制作流程.md 的最新版本。这是用户直接指定水牢特效接入，不是领取表内新任务；不动工作簿。
共享工作区已有大量未提交改动，必须保留。
复审时点：本阶段所有可推进定位任务返回后集中处理；本单不自行另开正式复审。
用户已确认的最新需求：
1. 使用已确认原素材。
准确素材路径：D:/alianximod/docs/资源库/过程归档/水牢泡泡_2D_v3_斜向错层-20261027/water_prison_bubble_v3_strip.png，96×1536，16个96×96 RGBA帧，每帧100ms，1.6秒循环。
2. 游戏是纯2D单张平面，随当前客户端渲染相机朝向（含俯仰），多人各客户端独立，无服务端朝向同步。不加载体积球壳/流带模型。
3. 始终保持原图宽高比例，平面两轴用同一个尺寸；只整体放大缩小，包住目标碰撞箱；禁止按宽高分开拉成椭圆。
4. 已确认预览：D:/alianximod/docs/资源库/过程归档/水牢公告板验证_v4_等比缩放-20261027/；参考 preview-billboard.js 的设计但不要盲搬 THREE API。
5. 透明水纹可覆盖目标前方，世界遮挡仍正常；不得渲染成墙后永远可见。使用透明混合、深度测试、关闭深度写入；避免正反两次叠色。
6. 保留现有水牢命中、湿度、持续、聚能和生命周期逻辑，本次仅换表现及必要的客户端尺寸采样。不得借此重做服务端、底座或网络协议。若确需修改范围外文件，先在报告中说明。
7. 默认不打包。实现结束先交付待统一编译，编译仅后续指定执行者做。

你的范围：D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonView.java、PrisonShell.java、PrisonRuntime.java、PrisonAim.java；D:/alianximod/src/main/java/com/bslx/XueXiMod/network/prison/PrisonSyncPacket.java、PrisonClientBridge.java；及client/PrisonClientEntry.java、PrisonClientState.java。只追踪这几个符号必要上下游，不通读整仓。
目标：查清水牢客户端现有目标实体、碰撞箱/壳尺寸、位置、更新/删除包、寿命与聚能输入来源；明确只换客户端表现应该保留哪些不变量，是否能零协议改动接入。给玩家实测的具体前置（装配/潮湿条件）与步骤，以现行代码为据；新旧命中/尺寸不要混淆。交付路径与行号，以及建议的最小改动范围。不要实现或跑编译。
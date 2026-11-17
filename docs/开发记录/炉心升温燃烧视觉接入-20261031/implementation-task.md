# 已批准升温 v2 与燃烧 v3 的客户端视觉接入

唯一实现执行者：本 Cursor CLI 会话，显式 --model grok-4.7-high-fast。当前用户授权：『让cursor接入，你不做检查节省token，检查完全交给cursor』。Codex 主会话不审代码，不调用脑子，不跑编译。你承担实现和必要自查；后续集中检查亦全部由 Cursor。固定模型不可用即报告，不切模型。

阶段：实现与必要自查，完成停止写入交最终反馈。此轮不编译，之后统一编译要等相关写入收束。读 D:/alianximod/AGENTS.md、.grok/skills/max-dispatch/SKILL.md、docs/统一开发与术式批量制作流程.md。用户新指令优先；已批准视觉无需重新问。保留所有现有脏改动，不 reset/clean/checkout，不打包，不 build/jar/reobf/runClient，不启动或操作游戏。

## 批次与分工
本批仅 fire_heat_up 升温、fire_burn 燃烧，二者原来在制作中。不领取别的术式，不改冷却。唯一制作表写回者预先指定为后续独立 Cursor 会话 heat-burn-sheet-writer（同模型）；你只读表与历史详细需求，不写 docs/术式制作表.xlsx。写回者会在最终编译通过后迁这两项到待验收，不提前标完成。
读取这两项当前 N/O/DV 详细需求和已有炉心接入记录。不要把待验收展示短句当详细需求；批准的新美术替换旧 O。你的报告记录采用源与实际加载引用，供后续写回。

## 唯一批准美术
正式源目录：
D:/alianximod/docs/资源库/特效/火焰/升温_贴身炽热_v2/
D:/alianximod/docs/资源库/特效/火焰/燃烧_双手附焰与火焰披风_v3/
先读两目录 README.md、manifest.json；保持批准源文件不变。
升温：heat_up-v2.aseprite、heat_up-v2-strip.png、heat_up-v2-back-strip.png、heat_up-v2-front-strip.png；96x112 每帧，20帧每帧80ms持续循环。heat_up-v2-entry.aseprite 和 entry-strip.png 为8帧每帧60ms单次入场。
燃烧：overdrive-v3.aseprite 是正确完整5层源（20帧80ms）；overdrive-v3-combined-strip.png 是纯效果合成。独立部件 overdrive-v3-cape、rear-fire、body-fire、hand-fire、embers 各有 aseprite 和 strip.png。overdrive-v3-entry.aseprite 8帧60ms含同5层。front-back-preview/heat_up-v2-preview 含灰色人偶或文字，仅预览，禁止运行时加载。过程归档里有一个空的 overdrive-v3-combined.aseprite，禁止使用它，必须正式目录 overdrive-v3.aseprite。
源是2D概念，需要映射至人体正确部位；不能整个合成图当悬空光环。

## 批准表现与约束
升温：贴身橙热边缘、前臂和躯干侧部向上热流、少量余烬；头和中心清晰，不做地面火圈。
燃烧：双手附火（两个拳部最亮金白核心，向上橙色火舌），必须跟随左右手骨骼/持物/攻击/行走动作；肩躯干髋腿局部附焰呈身体燃烧感；额外肩背附着的火焰披风往下垂，飘动并有破碎火焰边、内部火流。披风方向随身体背部，不是总朝相机的平面，不是地面圆环。分层避免前后遮挡错误/模特变形。玩家第三人称和其他客户端可见，苗条/标准手臂适配；自视第一人称不成为挡屏的原版燃烧遮罩。若现有能力支持手部局部第一人称效果可合适接入，不改变状态/伤害逻辑。
状态进入成功播放对应entry一次；普通炉心升温转燃烧直接播放燃烧entry，不先重播升温；维持循环流动；状态消失移除效果。保留冷却原有退出蒸汽。不要改变服务端炉心、成功/失败费用/CD、F/火种倍率、叠加/牛奶/死亡/NBT与判定机制，不设置原版着火。
若需手掌局部裁切或入场层导出，允许从批准 Aseprite 分层源无损导出配套图片，保留原像素设计、透明度、帧时序并记录裁切/UV/锚点；优先 Aseprite 批处理/Lua，禁 AI 生图。衍生脚本放 docs/资源库/过程归档/炉心升温燃烧视觉接入-20261031/；批准原件不动。

## 文件所有权与并发
另一批 docs/开发记录/术式批量制作/待制作-20261031/ 正在实现。只读其中 author-a-ownership.json、author-b-ownership.json 或对应 task.md 的所有权段，别跟读其正在写的代码。本批优先只写 src/main/java/com/bslx/XueXiMod/client/FireVisualClient.java 及你新增的独立炉心渲染/layer类（命名明确Furnace，先定位现有注册避免抢写共享类）、src/main/resources/assets/xueximod/textures/effect/furnace_v2/ 及本任务独有新目录。
禁止写 FurnaceService/FurnaceMath/FireRuntimeEvents/火种接口、ReleaseDefinitions/ModTechniques/Technique/链/命中/费用/法杖/语言、其他批的 Domain/渲染/宿主类。跨界必要注册改动先报告交接，不抢写。开始先把实际文件独占路径写本任务 ownership.json。
允许更新两正式资源目录 README.md 的实际接入说明；资源库总 README.md 属共享索引，本轮只写本任务 index-update.md 说明待合并条目，避免其他批并发丢失。旧 furnace_v1/cool 仍需保留，不删除旧档案。只读源码范围限炉心状态/客户端渲染接缝和必要模型Forge API，勿通读整仓。

## 交付
对自己实现做必要检查：正确部件/姿态/首尾帧/透明资源/帧坐标/入场与升级时序/移除/冷却兼容，别擅自添大规模测试。此轮不运行 Gradle/javac，不做独立额外审查。完成写 implementation-report.md 和 implementation-result.json，包含准确改动文件绝对路径、实际资源引用、关键行号、SHA256/mtime、已做检查、未做的统一编译及游戏验证、明确任何缺口，并声明停止写入。报告可据实说明第一人称范围，不声称已经看过游戏效果。最终反馈精简，留报告路径，别流式向主会话汇报中间内容。

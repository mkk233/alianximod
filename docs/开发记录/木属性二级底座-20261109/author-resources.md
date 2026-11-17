这是独立的可写实现工作项，已由统一启动器显式 Start -WriteMode。旧只读检查会话已经结束并移交所有权，没有源码写入；不继承旧任务的只读限制。用户实现授权与架构裁决未变，仍在同一个 batchId，校验轮次未重置。下面原实现单中有关沿用旧 session 的字样由本段取代：从本次新建的实现会话起，后续返修统一 Resume 本会话。完整读取当前 TaskFile 和架构裁决、对应事实报告。效果作者另读伤害包装补充事实.md。不要派子代理、不要修改启动器或旧运行记录。

# 资源作者 A 实现单

沿用本 task 和原 Cursor session，固定 --model grok-4.7-high-fast。完整读 author-resources.json、本文件、架构裁决.md（第1至3节和第6节），再按指定范围写入。你前次只读报告“无git”不成立：主会话 `git -c safe.directory=D:/alianximod rev-parse --show-toplevel` 已成功，保护全部既有改动。

用户已授权实际开发花瓣与岁华底座，死亡清本人花瓣与岁华、不奖励。所有设计未决项按裁决的显式参数边界处理，不制作具体花谱/枝条子弹/花海攻击/大招，不制作图像或渲染，不修改制作表。主会话已有唯一gpt-6-astra设计裁决1/2；本单不再请模型或子代理审查。

唯一作者范围：新目录 `src/main/java/com/bslx/XueXiMod/status/wood/resource/`。可在 `tools/wood-base-checks/resources/` 准备必要的有意义验证代码/脚本，以及写 `资源作者交付.md`。禁止写其他Java、注册、网络、事件、语言、mixins和另一作者目录。没有编译授权，不运行Gradle/javac/测试；准备验证交统一收束后执行。

落实裁决的单SavedData、60000精度、三度六项快照、不可伪造单次生产票据、完整批次发布、根区间预算守恒、分割/整组消费/无回充来源、空间查询与截断、容量拒绝、服务端主线程、满额稳定截断、自然/非自然清理、未知版本保护与生命周期服务。所有public mutation都需要真实运行线程与有效actor，不能只做纯数学库。

将Forge公共接线留给整合作者C，你提供清楚的可调用方法（server tick、owner死亡/登出/换维、区块/维度卸载、server start/stop、login/respawn/dimension sync需要读的快照）。可以提供客户端只读快照缓存，但不能自己注册事件或网络包。客户端类必须隔离，专用服务器不能加载Minecraft客户端类。公共ResourceStatus登记与SuihuaSyncPacket由C做。

请在文档中交付**完整准确的公开签名、包名、参数单位、成功/拒绝/异常语义、生命周期调用点**。告诉C如何收到余额变化通知（例如可安装的只读sink或明确drainDirtyOwners），避免C全量每tick广播。准备只读ResourceStatus provider供C注册或明确readSuihua用法。不要改ResourceView签名。

生产票据不能因多次publish/加载/旧编号重新注册而再获得预算；ready回调要防重入修改导致两次消费；坏存档不能制造重复区间，ROOT/FRAGMENT计数不被伪造超限；外部discard原因不能取得自然凋谢权限。地面原始记录在本轮会按裁决于重启清除无奖，岁华保留，记录此语义。

必要验证材料重点用可重复的序列与不变量：整数区间拆分-消费-到期守恒、多次消费/重复票据/旧revision拒绝、取消ready不扣、ULTIMATE始终零、满额成分稳定、存档往返及坏数据、非自然清理无奖。沿用现有检查风格，避免为每个getter写镜像测试，避免引入新测试框架。不能跑的验证如实说明。

写前记录允许目录若有既存文件的基线，若目标突然被别人写入先停对应文件交回，不覆盖；不用reset/clean/checkout/提交。共享工作区A/B/C同时写互斥文件，不跑统一编译。

完成 `资源作者交付.md` 后只返回最终反馈（含精确文件列表、API合同、未跑验证、writesSettled），随后停止写入，待C整合。标记JSON格式沿用前次。workStatus可为reported_done/blocked/needs_input/failed，成功仍是“待整合与统一编译”。


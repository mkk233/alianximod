模型：Cursor CLI --model grok-4.7-high-fast。沿原author-core/task/session278437cd-c996-44f0-bbc9-a793ec2d3ece显式Resume。完整读取TaskFile、本Prompt、记录最新用户v3与载具事实（旧事实中remaining禁止已失效，以下为准）。本单是明确规则的实现阶段A，不做新架构裁决/复审，不改计数。
用户最新确定：测试法杖保持32格；剑术保持四个独立入口、每链8格。只禁止剑术使用固定多重数量>=8；穷尽REMAINING不限制，一分为八不在多重禁用范围。三/四重正常。任何格数、ChainRole、复制/解析/时序/付款机制均不变。
本次只写TaskFile五路径，独占新SwordMulticastGate、SpellAssemblyMenu、SkillAssemblyPacket、ModTechniques及原交付记录。SkillUsePacket/ReleaseDefinition/语言文件仍由外部C占用，本单禁止打开其运行中内容或修改；本阶段不接施放检查，不写locale，交回后等协调再阶段B完成。
实现明确最小规则：
1. 在shu/technique/release/SwordMulticastGate.java建立纯资格检查，共享给当前三个既有入口：公开单Technique谓词及单链List<Technique>谓词（方法名可按规范），仅读ReleaseRegistry对应definition.multicastSpec。精确只有kind==FIXED且fixedCount()>=8返回受限。必须先分kind再fixedCount（非fixed会抛）。null/无spec/remaining不因此受限；不按八个ID分支，不查看replication.factor。链谓词只查传入这条链，不拼接四入口，不写玩家状态/付款/冷却。
2. 当前真实gui/SpellAssemblyMenu.rejectReason的非法杖分支、network/SkillAssemblyPacket.put的非法杖分支调用同一单术式资格点，在变更/消耗装配物品前拒绝；法杖分支不套这个限制，保留原学会/类型/槽数资格。必要在包已有整链检查处用单链谓词，避免已有无效链经包重新提交。保留取出/修正旧非法配置能力，不偷偷删除或迁移旧NBT，不影响另外三个合法剑入口。
3. 提示拟统一新键gui.xueximod.sword.multicast_limit，中文“剑术不能使用八重及以上的固定多重释放”，英文“Sword techniques cannot use fixed multicast counts of eight or more”。本阶段不写被占用locale，仅按现有消息返回签名接入这个语义/键；优先使用已有Component消息通道，不重构消息协议。向阶段B准确交付所需键和调用签名。
4. ModTechniques仅更新八重和十六重description，精确分别：
“按多重释放规则，依次释放后续最多8个装配格的内容，不能用于剑术。”
“按多重释放规则，依次释放后续最多16个装配格的内容，不能用于剑术。”
其余六项短句保持原样，尤其穷尽不加禁用。不要改变注册ID/数量/冷却/类型。
开始前核对carrier-before基线（新helper原不存在）；有他人新变化先交回，不恢复覆盖源码。原未提交改动必须保留。
本单无编译/表格/游戏权限，不读其他作者运行中源码，不派其他模型。改Java后执行powershell -NoProfile -File tools/check-name-gate.ps1，只修本次文件的命名问题，交回真实结果和未跑编译/游戏。不要新增独立测试阶段。
追加核心作者交付.md，准确写阶段A完成路径/行号、谓词真值、法杖不受限制、四入口独立，以及阶段B施放检查/locale尚未接入，不把阶段A当完整限制。最后标准报告，writesSettled=true、changedFiles真实，nextAction等外部C收束后阶段B。设计2/2和实现2/3不重置，源码全收束才做最后一次第3轮复核。

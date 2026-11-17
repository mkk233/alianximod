# B：自动发射源与攻击实体适配

固定 Cursor CLI --model grok-4.7-high-fast，新可写会话。本阶段实现。读本目录 implementation-contract.md（正式接口）、design-decision-1.md、记录.md、read-domain-final.md、read-capability-facts-final.md。设计2/2耗尽不追加；实现0/3，所有作者结束后集中审。本单不复审旧接触计费。

完整实现B职责：新增release/aim/LaunchAim，CastFrame.execution/launchAim，HitSpec的自动来源字段，所有已定位效果与实体使用领域显式位置/方向/局部目标，跨tick不再读玩家准星/全局标记，不污染玩家锁定。领域真实范围约束每个实际生成源点，包括二次volumePoint；形状偏移及后续飞行不硬钳。手动路径保持。

独占源码文件（在D:/alianximod/src/main/java/com/bslx/XueXiMod下rg确认真实路径）：CastFrame.java、TechniqueHitSpec.java、TechniqueHitEntity.java、ProjectileMotion.java、FollowCasterMotion.java、TrackingSupport.java、SpaceCutEffect.java、LiaoliEffect.java、PhantomSwordEffect.java、SwordEntity.java、SwordVortex.java；新增release/aim/LaunchAim.java。StillAimMotion.java仅确需显式输入时允许你改。TechniqueHitEntity/TrackingSupport原候选查询作者已released，先读最新版本保留其查询优化；不要从旧备份覆盖。其他已释放文件如SwordEntityMotion/HitVolumes/LockTargetService/ContactEnchantState不在本次写权限。需要额外直接接点时返回事实并交主会话指定，不擅改。

A拥有bind包及network、ReleaseBatch；C拥有领域/登记/ReleaseDefinition。按正式契约使用其接口，不创建占位类型、不改别人的未完成代码。LaunchAim保存TargetCheck，跨tick每次重验同世界/完整域内/存活/关系；失效保留合法备用方向，不跨域重选、不回准星、不持有领域取消令牌导致已生成攻击关域消失。

必须实际接入：燎里自动持续绕过releaseSlot松键/后续换手结束，但保留自然寿命；跟随改到该次发射源与局部目标；幻剑悬停本地瞄准起飞冻结；万剑归宗保持操纵既存剑，使用显式控制锚点与局部方向，无现存剑不伪造生成；空间切割自动路径不先读取准星；垂丝/斩击不可变弹道不改；无锁目标仍按本轮方向正常放。自动来源在每个真正生成之前校验frame.execution.token；已经生成的实体不因领域随后关闭消失。效果里的装备资格实际出手检查主人当前手持，武器数值仍用本轮副本，不能只靠副本绕过幻剑资格，不按id写白名单。

实施前把拟改旧文件完整内容保存本目录backup-B/相对路径。保留已有未提交改动，不reset/clean/checkout。Shell若Rejected，不换壳/工具绕过同一拒绝命令；原生读写获准即可做源码及完整内容备份，hash/mtime未取得填null。不能完整备份则停对应文件报告。文档只写implementation-B-final.md、evidence-B.json及本作者备份。

无编译/测试/打包/游戏/提交/表格/权限配置授权。Shell允许才跑现有命名门，拒绝如实记未跑。全部写入完成后返回并released=true，给改动清单、各能力实时输入替换事实、保留的手动路径、跨作者待接点与未验证项，标待统一编译。不读取其他作者实时日志，不等待他们源码再自作跨文件修改。

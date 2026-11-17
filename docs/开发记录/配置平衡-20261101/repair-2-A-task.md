固定Cursor CLI --model grok-4.7-high-fast，原作者A执行review-2问题1和4。先完整读review-2-final.md。设计2/2已用尽，实现复核2/3需返修；本单只落实明确修复，不新设计、不正式复审，不编译/测试/游戏/改表/提交。
独占生产范围：shu/technique/release/TechniqueHits.java、ReleaseBatch.java、HitVolumes.java（后2同release目录）、shu/technique/release/entity/**、network/SkillSegmentMath.java及你原A拥有的必要紧邻报价/账本调用文件。HitVolumes现明确新增归A。B同时修SwordEntityMotion与接触系统，C不再写。禁止跨写B/C目录，不读他们中间源码。
共同接口合同（必须实现，B将直接据此接入）：
- 在TechniqueHits提供classifyHit、classifyRoot、classifyConfirmed，参数/重载分别与现有hurt、hurtRoot、hurtConfirmed对应，返回与现有classifyRespecting完全相同的裁决类型。保留原正伤路径、权限、无敌帧语义，仅拆分结果。
- 有限且恰好0、权限成立 => ESTABLISHED_NO_DAMAGE；负数、NaN、正负无穷、权限不允许 => 无效。正伤按原真正hurt结果。不能把非有限/负数都并进0，也不调用原版hurt处理0。
- 旧hurt/hurtRoot/hurtConfirmed布尔接口恢复/统一为“正伤被接受”，即裁决grantsSeeds()，合法0返回false；不要让布尔同时表示可接续。需要0接续的生产路径一律读typed裁决：continues()决定成立/接续，grantsSeeds()决定正伤限定产种。classifyRespecting同一0/非法边界。
- A的TechniqueHitSettle、ReleaseBatch.flush所有含flow/不含flow、respectLanding与普通分支都消费正确裁决，不能把continues直接构造DAMAGE_ACCEPTED；retireSeeds/ResourceSettlement使用grantsSeeds，0仍可接续并正确收束账本。检查自己名下其余hurt*调用是否还承担可接续用途，按此合同接入；HitVolumes不留下反义布尔约定，未用路径无需扩功能。
- B同时会把SwordEntityMotion、ContactEnchantState、DomainContactBilling及confirmTracked改用这些接口。不要改B文件；最终给接口签名与裁决语义清单（本批命中裁决接口.md）。
费用问题4：quotedMana仅对领域/屏障/护环这种真实费用由另一单价承担的0宿主占位跳过读取，用定义能力区分，不能按术式ID新分叉。其余原始单价为0的规则/动作/索敌/剑来等必须仍走BalanceDisplay.mana读取已配置标准manaCost，保持原收费次数/预留/退款，不新加扣费点。原始0默认仍免费，配置为正数才改变金额。不要把水系生成费重复加进宿主预扣。
保存本次修前字节pre-repair-2/A/，原始pre-A及pre-repair-1保留，更新author-A-manifest.json。必要命名门禁不扩修OriginalHumanoid。最终完整交回改动、全生产调用裁决映射、0费用覆盖与占位排除依据、未完成/阻塞；待统一编译，全部写入释放。不得把返修说成已通过正式复核。

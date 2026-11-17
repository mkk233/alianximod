我先把任务说明完整读一遍，再按里面的范围执行。任务是只读事实调查。我先读指定文档和相关入口，再按七项定向核对代码与制作表。底座合同和批次记录已对上。接下来核对登记、分身入口、水系攻击和制作表这几处现况。制作表是 xlsx，我只用只读方式抽出四张状态页里和“水幻形”相关的行。水幻形还没有正式术式。现有能对上的只有开发用幻影分身 `clone_phantom`，以及另一套不做事的旧幻影桩。制作表四页本次没有解开，行、稳定 ID、`N`/`O`/`DV` 和资源明细都空着，不能当成“表里没有”。

## 1. 登记现状

没有名为“水幻形”的稳定 ID、`ChainRole`、效果、语言键或贴图。

已登记的是三个开发术式，都标 `TechniqueAvailability.DEVELOPMENT_ONLY`：

| ID | 显示名 | `ChainRole` | `ReleaseKind` | 显式耗魔 |
|---|---|---|---|---|
| `clone_phantom` | 幻影分身·基础 | `ACTION` | `ACTION` | 10 |
| `clone_body` | 普通分身·基础 | `ACTION` | `ACTION` | 10 |
| `clone_response_pulse` | 分身基础脉冲 | `ATTACK` | `ACTION` | 2 |

出处：`CloneBalance.java` 19–21、29–34 行；`CloneTechniqueRegistry.java` 58–103 行；`CloneReleaseDefinition.registerAll` 24–30 行。`lifetimeTicks()` 固定返回 1，只为满足释放接口，分身寿命另读 `CloneSpec`。

旧效果执行器对这三个 ID 直接抛 `DEVELOPMENT_ENTRY_ONLY`（`CloneTechniqueRegistry.java` 52–56 行）。真正生成走权限 2 的 `/shu clone spawn phantom|normal`，再进 `CloneService.spawn`。幻影在 `specFor` 里写成一次响应并在预算用尽后结束（`CloneService.java` 638–644 行）。

语言只有实体名：`zh_cn.json` 126 行 `entity.xueximod.clone_actor` =「分身」，`en_us.json` 69 行 = `Clone`。术式名写在 Java 里，没有 `clone_phantom` 或“水幻形”的 lang 键。实体注册路径是 `clone_actor`（`CloneEntityTypes.java` 16–19 行）。

表现是 `CloneRenderer`：原版 `HumanoidModel` 加 `minecraft:textures/entity/zombie/zombie.png`（`CloneRenderer.java` 21–41 行）。`src/main/resources` 下没有分身贴图。动作序号变化时只挥主手（44–57 行），没有撒水或散架表现。

旧幻影包 `shu/technique/release/phantom/` 与这套分身分开，也不是水幻形：

- `PhantomSpec`：只有 `origin()`、`aim()`、`lifetimeTicks()`。
- `PhantomFactory.spawn(ServerLevel, ServerPlayer, TechniqueHitSpec)` 调用 `TechniqueHitSpec.spawn` 后返回永远 `active()==false` 的 `Dormant`。
- `PhantomHandle.end` 为空。`PhantomEndReason` 只有 `EXPIRE`、`DISMISS`、`OWNER_LOST`。

`phantom_sword`（幻剑术）是另一条 `ChainRole.ATTACK` 的剑术，不要并进水幻形。

## 2. 分身入口、规格与现有签名

普通玩家入口看注册时的 `TechniqueAvailability`。构造默认是 `PLAYER`（`Technique.java` 47–51、69 行）。`TechniqueAccess.allowsPlayerUse` 在 `DEVELOPMENT_ONLY` 时为假（18–19 行）；`skillAllowed` 整链里有一段开发术式就拒绝（32–45 行）。过滤点：`SkillUsePacket.java` 185、244 行，`SkillAssemblyPacket.java` 118 行，`SpellAssemblyMenu.java` 339 行，`ImprintActions.java` 33 行，`PlayerShuData.java` 102 行，以及 `ShuCommands`、`StaffSkills`、`ImprintDeskMenu`、`ShuManagementMenu`、`PlayerConnectionHandler`。三个现有分身 ID 进不了学习、刻印和装配。

`CloneService.spawn(ServerPlayer, CloneKind)` 反过来要求生成术式和脉冲都保持 `DEVELOPMENT_ONLY`，且 `actionKey` 必须是 `clone_response_pulse`（648–660 行）。现在的生成入口不能直接变成一条 `PLAYER` 术式。

`CloneKind` 只有 `PHANTOM`、`NORMAL`。`CloneSpec` 没有表现字段，也没有“响应哪一条术式”的字段，只有 `actionKey`。没有独立的 presentation 类型。

现有签名：

- `CloneService.spawn(ServerPlayer owner, CloneKind kind)` → `CloneResults.SpawnResult`（51 行）。生成在主人脚底，移速属性 0，眼高 1.74。
- `CloneService.request(ServerPlayer owner, CloneHandle handle, CloneTriggerInput input)` → `RequestResult`（155 行）。`executeTick = requestedTick + 1`，`expireTick = requestedTick + 2`（214–216 行）。
- `CloneService.onAcceptedHurt(CloneEntity actor, DamageSource source)`（231 行）。
- `CloneTriggerInput(Source source, Vec3 manualLook, UUID attackerId, int sourceDepth)`，`Source` 为 `MANUAL`、`HURT`。
- `CloneActorFrame`：世界、分身 ID、代次、主人 ID、脚底、眼睛、单位视线、目标 ID、动作序号、`AimSource`（`MANUAL_DIRECTION` / `HURT_TARGET`）。
- `CloneAction.actionKey()` / `prepare(CloneActionContext)`。上下文是主人 `ServerPlayer`、帧、根事件、动作令牌、取消令牌。
- `ClonePreparedAction`：接受与否、拒绝原因、`ConfiguredDamage`、魔力、冷却、目标 UUID、`commit()`、`cancelBeforeCommit()`。
- `CloneActionRegistry.register/get/admit`。`register` 只给 `ClonePulseAction` 赋 `CloneActionCapabilities.full()`，其他动作得到全假的 `NO_CHAIN`（27–32 行）。`perform` 用 `admit(actionKey, full())`（456–462 行），对不上就是 `UNSUPPORTED_CHAIN`。
- `CloneBalance.readSpawn(BalanceSide, CloneKind)`、`readPulse(BalanceSide)`。脉冲伤害走一次 `BalanceService.damageContribution`。`PULSE_DAMAGE_MULTIPLIER` 只是说明默认倍率为 1，不再乘第二遍。
- 结束：`CloneRuntime.endActor` 取消待执行请求、清诱敌、`discard()`（334–357 行）。

一次响应落在 `ResponseBudget.ONE` 和 `endAfterBudgetSpent==true`。跨过提交线才把预算标成已用（560–565 行）；`settleCommitted` 随后以 `BUDGET_SPENT` 结束（578–588 行）。提交前失败会回到 `READY`，不扣次数、不收费。

受击前的清理在 `CloneEntity.hurt`（204–230 行）：非致死且 `super.hurt` 接受后才 `onAcceptedHurt`；已经死亡则 `endLoaded(DEATH)`，不反击。响应进行中 `CloneSourceScope.blocksResponse()` 会拒绝新的受击。

没有触碰、碰撞或接触溶解入口。破散目前就是实体 `discard()`，渲染层没有对应表现。

脉冲本身是现有唯一走分身几何的动作：眼睛和方向来自 `CloneActorFrame`，结算是 `TechniqueHits.classifyRespecting(target, owner, amount, TideMarkSuccess.NONE)`（`ClonePulseAction.java` 448–450 行），伤害源是主人的 `playerAttack`。默认固定伤害 2、射程 8、耗魔 2、冷却 20。不读主手，不加武器伤害。

## 3. 水系攻击与演员约束

`WaterReleaseDefinition.role()` 一律返回 `ChainRole.ATTACK`（366–367 行），含水幕、雾雨、水环、深海重压、水牢。下面只列真正打出命中体的四条。潮印 `tide_mark` 是附魔，不是攻击段。

四条的 `execute` 都是 `(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast)`，最后都 `TechniqueHitSpec.spawn(level, player, spec)`。`spawn` 的签名要求 `ServerPlayer`（`TechniqueHitSpec.java` 191 行）。命中体记下施术者 UUID；没写起点时用施术者眼睛，没写方向时用施术者视线（`TechniqueHitEntity.java` 507–518、583–597 行）。非自动寿命时，施术者为空或不活着，释放停止（1102–1108 行）。结算金额来自当次链的 `CastFrame.damage`，伤害源仍是该 `ServerPlayer` 的 `playerAttack`。

伤害公式在 `SkillSegmentMath.segmentDamage`（142–146 行）：基础伤害大于 0 时为基础伤害加武器加成再加魔法伤害。武器加成是主人 `ATTACK_DAMAGE - 1`，且不小于 0（`SkillUsePacket.java` 248–249 行）。随后 `configuredSegmentDamage` 用 `BalanceDisplay.damage` 做一次倍率（332–336 行）。

| ID | 名称 | 基础伤害 | 代码耗魔 | 冷却 | 持剑 / 身体 |
|---|---|---|---|---|---|
| `water_blade` | 缺月 | 7 | `ceil(7×2)=14`（`explicitCost=-1`） | 14 | `swordForm()` 把 `heldSword` 和 `hostGroupSubmit` 设为真。`HeldSwordGate.allows` 看主手是不是剑。`WaterBeatBridge.capture` 读主手物品和选中槽 |
| `tide_sever` | 断潮 | 6 | `ceil(6×2)=12` | 20 | `requiresHeldSword()` 为假。锚点是 `player.getEyePosition()`，方向是 `cast.getLook()` |
| `water_yielding_ribbon` | 让锋 | 40 | `ceil(40×2)=80` | 600 | 锚点用 `player.position()`。`WaterPartingService.prepare(ServerPlayer, …)` 绑主人 |
| `water_weeping_arc` | 垂丝 | 2 | 显式 10（`RETAINED_MANA`） | 20 | 没有几何覆盖时起点是 `player.getEyePosition()` 前 2.2 格 |

`water_blade` 的专项只声明寿命 10 刻、速度 1.3、厚度 0.35，不另声明伤害或耗魔。旧编号 `water_open_ring` 规范成 `water_blade`。四条都在 `BalanceCatalog.DAMAGE_HOSTS` 里，标准伤害倍率只挂一次。

这四条都把 `ServerPlayer` 当作施术者、归属和结算主体。水刃还绑定主手和宿主挥击。分身动作上下文里没有术式、链、武器或主手。按现在的 `CloneActionRegistry.register`，新动作也拿不到 `full()` 能力。现有效果不能直接当成独立 `CloneAction` 调用。

## 4. 同一条装配链能拿到什么

当次释放能看到：

- `Skill.getTechniques()`：有序术式列表。
- `TechniqueChain.split(Skill)` → `List<ChainSegment>`。每段只有 `actionIndex()` 和 `enchantIndexes()`（`ChainSegment.java` 5–19 行）。
- `SkillUsePacket` 在排同一发时看下一段：下一段存在、本段是 `ACTION`、下一段是触发释放且不操纵剑实体，才把间隔接到下一触发（854 行附近）。这是同一次释放的间隔，不会把下一段存进分身。
- 领域绑定另有冻结子计划：`BindingLayout.compile(Skill)`、`BoundReleasePlan`。里面有冻结术式、胚胎、原始下标、`nextOriginalIndex()`（没读到的下一格，-1 表示旧工厂没这份游标）。`DomainBindingRuntime` 在开域后 20 刻、之后每 20 刻调用 `BoundReleases.executeAutomatic`。上下文仍是在线主人、武器复制和 `weaponBonus`。

分身请求只有 `actionKey`、来源、冻结视线或攻击者 UUID。`CloneService.spawn` 不接收 `Skill`。没有“捕获链上下一段，等分身被碰到再放”的入口。分身自己的延迟只是受击接受后下一 tick 执行脉冲，再过一 tick 过期。

## 5. 失败、死亡、主人离线

这些语义代码里已经有，和水幻形需求不是同一件事：

- 提交前拒绝（无目标、被挡、魔力不足、冷却、忙碌、队列满）不结束幻影，次数还在。
- 提交之后，伤害被免疫也消费这一次，幻影以 `BUDGET_SPENT` 结束。
- 致死受击不反击，原因是 `DEATH`。
- 主人登出：`CloneLifecycleEvents.onLogout` 用 `OWNER_LOGOUT` 结束；`watchOwners` 每 20 刻再查一次，主人不在或已断开也是 `OWNER_LOGOUT`。
- 主人死亡是 `OWNER_DEATH`，换维是 `OWNER_DIMENSION`。
- 执行时主人已不可用且还没跨提交线：拒绝 `OWNER_UNAVAILABLE`，退款，幻影保持。若同时触发上面的结束，待执行请求会被取消。
- 读档把 `fromSave` 设上并丢弃，不恢复。区块离开是 `UNLOAD`，世界卸载是 `WORLD_UNLOAD`，停服是 `SERVER_STOP`。
- 单纯碰到、没形成被接受的伤害：没有处理。

## 6. 制作表

`docs/术式制作表.xlsx` 在。文本检索打不开里面的压缩 XML，本会话没有解表。四张状态页（待制作、制作中、待验收、已完成）里“水幻形”及候选 ID 的行号、`B`、`N`、`O`、`DV` 和「资源状态明细」都未读出，这里不补。批次记录写明尚未领取，本次也没有领任务。

## 7. 本会话 MCP

Aseprite 不在本会话工具目录里。按名称搜索没有这个命名空间。

Blockbench 命名空间状态是 `ready`，目录里有这些工具，本次没有调用：

`activate_texture`，`add_armature`，`add_armature_bone`，`add_group`，`add_locator`，`add_texture_group`，`animation_copy_paste`，`animation_graph_editor`，`animation_timeline`，`apply_texture`，`assign_texture_channel`，`auto_uv_mesh`，`batch_keyframe_operations`，`bone_rigging`，`capture_app_screenshot`，`capture_screenshot`，`clear_vertex_weights`，`color_picker_tool`，`configure_material`，`create_animation`，`create_brush_preset`，`create_cylinder`，`create_mesh_face`，`create_offscreen_view`，`create_particle_effect`，`create_pbr_material`，`create_polyhedron`，`create_project`，`create_sphere`，`create_texture`，`delete_mesh_elements`，`delete_offscreen_view`，`delete_texture`，`duplicate_element`，`emulate_clicks`，`export_model`，`export_particle_pack`，`extrude_mesh`，`filter_by_material`，`find_elements_by_criteria`，`from_geo_json`，`geckolib_list_easings`，`get_armature`，`get_armature_bone`，`get_average_fps`，`get_capabilities`，`get_cube_uv`，`get_material_info`，`get_mesh_info`，`get_project_info`，`get_selection`，`get_texture`，`get_undo_stack`，`get_vertex_weights`，`import_texture_set`，`inspect_block_bounds`，`list_armature_bones`，`list_armatures`，`list_export_formats`，`list_materials`，`list_modes`，`list_molang_variables`，`list_outline`，`list_particle_effects`，`list_particle_presets`，`list_textures`，`list_views`，`load_brush_preset`，`manage_keyframes`，`manage_particle_keyframes`，`merge_mesh_vertices`，`modify_cube`，`move_mesh_vertices`，`paint_settings`，`paint_with_brush`，`place_cube`，`place_mesh`，`remove_armature`，`remove_armature_bone`，`remove_element`，`rename_element`，`resize_offscreen_view`，`risky_eval`，`rotate_mesh_uv`，`save_checkpoint`，`save_material_config`，`select_all_of_type`，`select_armature_bones`，`select_mesh_elements`，`set_camera_angle`，`set_ik_controller`，`set_mesh_uv`，`set_mode`，`set_vertex_weight`，`set_vertex_weights_batch`，`subdivide_mesh`，`texture_layer_management`，`texture_selection`，`trigger_action`，`undo`，`update_armature`，`update_armature_bone`，`update_armature_bones_batch`，`update_particle_effect`，`variable_placeholders`。

Aseprite 预览要由主会话按美术技能做。

## 仍要设计者拍板

- 响应从同一条装配链指定，还是先固定缺月。用户还没回。
- 水线条人形的美术版本还没确认，不能接资源。
- “碰到”是接触，还是沿用现在的非致死受击。
- 反击被拒绝、魔力不足、没有目标时，人影是否仍然散掉。
- 一击致死时是否还要打出反击。
- 下一 tick 执行前主人离线、死亡或换维，这一次还打不打。
- 正式术式用新的玩家可用 ID，还是改现有 `clone_phantom`。现有生成门禁和动作能力检查都锁着开发脉冲。
- 制作表四页解开之后，才知道有没有既有编号和原文需求。

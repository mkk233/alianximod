沿同author-adapters task和已收束read-02完整上下文，执行已批准玩法内的必要实测返修。只授权一个Java文件的唯一作者：
src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/motion/VolumeMotionRuntime.java。
本轮可调用apply_patch工具直接写入；该工具通过本地原生Codex apply_patch后端执行你的原样补丁，主会话不编写Java。先读取最新文件及hash再补丁，保留原有未提交改动，无reset/clean，不加行内注释。原批次设计2/2实现3/3保持，不进行追加正式复审。

1. 实现你已定位的非整数底高支撑水错层修复，统一waterShared/baseWater的采样语义，覆盖本次临时水首个完整内含层，保留既有下方天然水支撑，不扩大领域、不整柱扫描、不一律贴地。采用现有几何/水判定能力，防止引入无水隔层也错误挂载的行为。不要修改StillWaterLease。
2. 入卷溃散目前未确定是合法资源/容量不足还是误判。不要改水势数值、阈值、湿度、收费、溃败条件或偷偷补充水势。给真实溃败出口加入足够而精简的一次性诊断日志：原因、领域/释放身份、可获取的水势和费用/结算前后、候选实体及AABB、容量/占位数据、组合状态。每次关闭一条可用日志即可，不逐tick刷屏，不新增玩家聊天提示，不在实体尚未接近时刷日志。使用项目已有日志设施/风格，避免大而复杂的新系统。
3. 为确定当次运行配置，可用list_files定位run/saves下的serverconfig/xueximod-balance.json以及run/defaultconfigs相关json，必要时读run/logs/latest.log识别当前世界名字。仅限本任务相关文件，禁止写世界/存档/配置，不读玩家NBT或无关隐私。找不到明确当前世界则如实保留未核实边界。
4. 不修你附带发现的带空间索敌CastExecutionSession落点问题：用户本次纯四格链不触发，保持任务聚焦。不得触碰另一个活跃崩溃修复作者的PressureBreath.java/DomainService.java。

无编译权限，不跑Gradle/打包/游戏，不改制作表或旧被审批拒绝的候选。先完成最小修复，再汇报真实改动、诊断日志可采集什么、仍未证实的第二现象、精确后续游戏步骤、实际配置发现；实现交付必须写待统一编译、未新增正式复审。不要将静态推理写成游戏验证。

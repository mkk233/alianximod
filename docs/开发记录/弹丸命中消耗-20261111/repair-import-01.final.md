# repair-import-01 final

- 状态：单行导入已补回，源码写入已停止。尚未开始 compile-02。
- 任务：taskId=projectile-hit-consumption，attemptId=repair-import-01。
- 范围：只恢复 `BatchFlightBook.java` 中仍被 `releaseArmedTrail` 使用的 `import net.minecraft.world.entity.Entity;`。未改其他逻辑，未做正式复审，未增加额度。
- 位置：第 18 行，位于 `ServerPlayer` 导入之后、`LivingEntity` 导入之前。
- 原因：compile-01 在第 451 行报告找不到符号 `Entity`。compile-01 的日志和回执未覆盖。

## 命名门

- 命令：`powershell -NoProfile -File tools/check-name-gate.ps1`。
- 脚本退出码：1。
- 共 8 处，全部位于既有客户端文件：`BatchCarrierVisualRenderer.java` 4 处，`NetherDomainWisps.java` 4 处。
- `BatchFlightBook.java` 不在结果中。既有 8 项未扩展处理。

## 七文件收束

SHA-256 与最后写入时间：

- `FAB51BEA77C563E4ADEB572A981B7E62120527E04A57944C733566F54F3F8AEC`，2026-11-11 16:39:32.218，`BatchSparkEffect.java`，未变。
- `DA98A74B2D9C9B256410C16B2845C68E7FEC4D38E0659CA9D7EDA42ABDA4B718`，2026-11-11 16:50:09.853，`BatchFlightBook.java`，仅新增上述导入。
- `C6F7B55B73C875B15536DF4AF8273F8CCB7E69CA87E7F36E7AEFC3615D5B6D04`，2026-11-11 16:41:52.197，`BatchCarrierVisual.java`，未变。
- `384B1307B9F361D2528DD6C44CCBE0F300CB6C3E540FADA0E1D6F424BBAD25F7`，2026-11-11 16:39:18.124，`TechniqueHitSpec.java`，未变。
- `E6A8AC90C2CA1AB94E61C5C4D4F7007A4E8BB0CDA948610019615D020345195A`，2026-11-11 16:43:19.239，`TechniqueHitEntity.java`，未变。
- `54C4FB88E353C93FE8190DE9A15FCFB604FB9B3925930DBBD35356A92C322AEE`，2026-11-11 16:43:36.263，`TechniqueHitSettle.java`，未变。
- `8E712A44F99C63F45CA57AB2D0CF960119604420E0EEB2EDC80709A2036CB870`，2026-11-11 16:42:32.856，`FireOrbEffect.java`，未变。

没有下属代理。源码写入到此停止，随后才允许 compile-02。

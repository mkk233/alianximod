组合水龙卷的入卷体积上限已按批准改为 48 立方格。独立旋流仍是 12。溃败、收费、座位、湿度和几何都没动。

改动点：
- `BalanceKeys`：在 `OCCUPIED_VOLUME_LIMIT` 旁增加 `LIFTED_OCCUPIED_VOLUME_LIMIT = "liftedOccupiedVolumeLimit"`。
- `DomainDefinition`：`occupiedVolumeLimit` 说明改为「独立旋流入卷体积上限。组合成立时不读这份」。其后登记新键，`RawDefault` 48，边界 `strictlyPositive(1_000_000)`，单位 `CUBIC_BLOCKS`，说明为「水龙卷入卷体积上限。只在组合成立时读取」。
- `VolumeMotionRuntime`：抽出私有方法 `occupiedVolumeLimit(host)`。`combination(host)` 为真时读新键，缺省 48；否则读 `occupiedVolumeLimit`，缺省 12。`acquire` 的上限和 `occupancyText` 都改调这一处，条件没有复制，其他判定未改。
- `docs/配置平衡指南.md`：`water_vortex` 键清单加入新键。表格中 `occupiedVolumeLimit` 的生效时机改为「独立旋流入卷」，并新增 `liftedOccupiedVolumeLimit` 一行，默认 48，单位与边界同上，只在组合成立时读取。

最终 SHA256：
- `BalanceKeys.java`：`b086c241ea992ca33e70f920909a45b3ee278ebd3b890e15f547db4e5b969986`
- `DomainDefinition.java`：`2763add74feacaea90bbba66522066a23978cee1c2d65779fa157ddc0e2e0b23`
- `VolumeMotionRuntime.java`：`f09c2cb94059fc85d97c2ae14575c879862d2395d1e7fbd529d6b5161c2ec7c8`
- `docs/配置平衡指南.md`：`c11e008abd8f7e9f19ec1fa9fde7f5651cbb8f54342f80ee7b502097cf23aae4`

命名门退出码：0。未编译，未做游戏实测。
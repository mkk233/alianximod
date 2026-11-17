# 燎里剑尖后扩环交付包记录

用户原话：“这完成后打个包我看看”。这只覆盖本次默认不打包，不是游戏实测通过。制作表未改，`fire_liaoli` 仍在待验收第 5 行，不迁已完成。源表 SHA256 仍是 `95C7CDFBBE48A25DD2D89AD019AE2A6B3278E7211F89689DF45489448014B768`，LastWriteTime 2026-10-30 03:22:01.806，168014 字节。

命令 `.\gradlew.bat jar --offline --no-daemon`，2026-10-30 03:29:23.3437651 至 03:29:58.5459300，退出码 0。BUILD SUCCESSFUL in 34s。`compileJava`、`processResources`、`jar`、`reobfJar` 都执行了。重混淆是 `RenameJarInPlace`。源码没有被这次编译改写。

Gradle 产物：`D:\alianximod\build\libs\xueximod-0.0.1-1.20.1.jar`。

交付副本：`D:\alianximod\docs\开发记录\术式批量制作\火系六式-20261029\交付包\xueximod-liaoli-tip-ring-burst-20261030-032958.jar`。

两份都是 5305898 字节，SHA256 `B389E53F64A18F287789C130DE7E43CC2770EF31FB13714910CEAD6C3BE2AFE6`，LastWriteTime 2026-10-30 03:29:58.2017640。没有复制到游戏目录。

打包前后 777 个 `src/main` 文件与 03:16 冻结清单逐行一致。包内 1292 个条目。`mods.toml` 是 `xueximod` `0.0.1-1.20.1`。`fire_seed_flame.png` 哈希 `106DD7C62A1E360C859FB598B320EBCF10A40568555C763465023380E5DE800B`，`rear_ring.png` 哈希 `857C37C052D082548A93B0E00868375C973D33B2CDB2C0B477C46C384A1A0CDE`。包内 `LiaoliEffect.class` 与开发目录的 class 哈希不同，是重混淆产物。

旧检查没有跑。贴墙可能取残留速度，身体偏航和视线偏航不同时的环包络没有验证。滑翔、激流、游泳的旧限制仍在。未游戏实测。设计 2/2、实现 3/3 承接。本阶段额度已用完，返修结果未追加复审。

本单停止写入。未改制作表，未改 src，未跑 Gradle，未改资源库，未操作游戏，未提交。

# 剑尖后扩环与迎面伤害：模组 JAR

这次按用户“这完成后打个包我看看”打包。默认不打包的限制只对这次交付放开。不是复审。设计 2/2、实现 3/3 不重置。本阶段额度已用完，返修结果未追加复审。没有游戏实测，也没有运行旧定向检查。

## 打包前

表写回已经停止，没有改 `src`。`src/main` 777 个文件与 03:16 编译后的冻结清单逐行一致。最后源码仍是 `TechniqueHitEntity.java`，2026-10-30 03:09:26.8377589。

开始前工作区里有两个 `dependencyInsight --dependency jade` 的 Gradle 包装进程，71024 和 70480。等到 2026-10-30T03:28:58.4885575+08:00 它们结束。没有杀掉它们，也没有结束历史空闲 daemon。结束后再对，源码差异仍是 0，没有别的 Gradle 包装进程。

配置未改。产物名来自现有 `archivesName=xueximod`、`mod_version=0.0.1-1.20.1`。`jar` 任务 `finalizedBy reobfJar`，重混淆是 `RenameJarInPlace`。没有跑 test、check，也没有跑会连带源码 zip 的 `verifyReleaseExcludesAuthorizedAssets`。

## 命令

`D:\alianximod` 下执行 `.\gradlew.bat jar --offline --no-daemon`。JDK 17，`GRADLE_USER_HOME=C:\Users\baishang\.gradle`，沿用 `-Xmx3G`。日志：`docs/开发记录/术式批量制作/火系六式-20261029/liaoli-tip-ring-burst-package-gradle.log`。

开始 2026-10-30T03:29:23.3437651+08:00，结束 2026-10-30T03:29:58.5459300+08:00，退出码 0。BUILD SUCCESSFUL in 34s。8 个任务里 6 个执行、2 个 UP-TO-DATE。`compileJava`、`processResources`、`jar`、`reobfJar` 都执行了。`compileJava` 仍有 34 条待删除 API 警告，没有错误。源码没有被这次编译改写。

## 产物

Gradle 输出：

`D:\alianximod\build\libs\xueximod-0.0.1-1.20.1.jar`

5305898 字节，SHA256 `B389E53F64A18F287789C130DE7E43CC2770EF31FB13714910CEAD6C3BE2AFE6`，2026-10-30 03:29:58.2017640。

交付副本，没有覆盖已有同名文件：

`D:\alianximod\docs\开发记录\术式批量制作\火系六式-20261029\交付包\xueximod-liaoli-tip-ring-burst-20261030-032958.jar`

字节、SHA256 和时间与上面那份相同。没有复制到游戏目录。

## 包内核对

1292 个条目。`mods.toml` 已展开：`modId="xueximod"`，`version="0.0.1-1.20.1"`，`loaderVersion="[47,)"`，Forge `versionRange="[47,)"`，Minecraft `versionRange="[1.20.1,1.21)"`。

清单：`Specification-Title: xueximod`，`Implementation-Version: 0.0.1-1.20.1`，`MixinConfigs: xueximod.mixins.json`，时间戳 `2026-10-30T03:29:34+0800`。`xueximod.mixins.json` 在包内。

这些类都在：`LiaoliV6Layout`、`LiaoliV6Renderer`、`LiaoliEffect`、`FireConsumerPlans`、`LiaoliRingTrail`、`LiaoliRingPacket`、`LiaoliRingBridge`。`LiaoliEffect.class` 的包内哈希是 `DCDFF363…`，开发目录里的同名 class 是 `D11B22BC…`，两者不同，说明这是重混淆后的产物，不是把开发 class 直接压进 zip。

`fire_seed_flame.png` 与正式归档都是 `106DD7C62A1E360C859FB598B320EBCF10A40568555C763465023380E5DE800B`。`rear_ring.png` 与 `docs/资源库/特效/火焰/燎里_v6/rear_ring.png` 都是 `857C37C052D082548A93B0E00868375C973D33B2CDB2C0B477C46C384A1A0CDE`。

## 源码

打包后再对，777 个 `src/main` 文件与打包前、也与 03:16 编译冻结清单逐行相同。清单是 `liaoli-tip-ring-burst-package-src-main-before.txt` 和 `liaoli-tip-ring-burst-package-src-main-after.txt`。

这次打包包装进程已经结束。没有再写产物。不自行改表。

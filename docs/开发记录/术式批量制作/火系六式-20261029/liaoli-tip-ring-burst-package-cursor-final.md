先读这份打包任务的授权范围，再按它核对停写和编译结果。用户已授权打包。先核对构建配置、写回结果和上一轮源码冻结，再决定实际的 Gradle 命令。工作区里还有两个正在跑的 Gradle 查询，先等它们结束，再打包。源码冻结没变，Gradle 查询已经结束。正在写入打包报告。重混淆后的模组 JAR 已经打好，退出码 0。源码 777 个文件和 03:16 的编译冻结一致。没有进游戏，也没有跑旧检查。

交付文件：

`D:\alianximod\docs\开发记录\术式批量制作\火系六式-20261029\交付包\xueximod-liaoli-tip-ring-burst-20261030-032958.jar`

5305898 字节，SHA256 `B389E53F64A18F287789C130DE7E43CC2770EF31FB13714910CEAD6C3BE2AFE6`，2026-10-30 03:29:58.2017640。Gradle 原产物在 `D:\alianximod\build\libs\xueximod-0.0.1-1.20.1.jar`，内容相同。没有复制到游戏目录。

## 打包

写回已经停写，没有改 `src`。打包前有两个查询 Jade 依赖的 Gradle 进程，等到 03:28:58 结束，没有杀掉它们。

`D:\alianximod` 下执行 `.\gradlew.bat jar --offline --no-daemon`。`jar` 结束后接着跑现有的 `reobfJar`。开始 2026-10-30T03:29:23.3437651+08:00，结束 03:29:58.5459300+08:00。BUILD SUCCESSFUL in 34s。`compileJava`、`jar`、`reobfJar` 都执行了。34 条待删除 API 警告，没有错误。日志：`docs/开发记录/术式批量制作/火系六式-20261029/liaoli-tip-ring-burst-package-gradle.log`。

打包后再对，777 个 `src/main` 文件与打包前、也与编译冻结清单逐行相同。

## 包内

`modId` 是 `xueximod`，版本 `0.0.1-1.20.1`，加载器 `[47,)`，Forge `[47,)`，Minecraft `[1.20.1,1.21)`。清单里的 Mixin 配置是 `xueximod.mixins.json`。

`LiaoliV6Layout`、`LiaoliV6Renderer`、`LiaoliEffect`、`FireConsumerPlans`，以及 `LiaoliRingTrail`、`LiaoliRingPacket`、`LiaoliRingBridge` 都在包里。`LiaoliEffect.class` 和开发目录里的同名 class 哈希不同，这是重混淆后的结果。火苗图和 `rear_ring.png` 都与正式归档一致。

说明在 `liaoli-tip-ring-burst-package-report.md`，结果在 `liaoli-tip-ring-burst-package-results.json`。这次打包进程已经结束，没有继续写产物，也没有改表。那两个正在查询依赖的 Gradle 进程已经结束，退出码是 0。模组包就是在那之后打好的，交付文件仍是 `D:\alianximod\docs\开发记录\术式批量制作\火系六式-20261029\交付包\xueximod-liaoli-tip-ring-burst-20261030-032958.jar`。

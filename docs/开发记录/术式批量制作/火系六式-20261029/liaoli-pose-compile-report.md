# 燎里剑阵姿态：最终编译

本单只编译。设计 2/2、实现 3/3 已用完。这次返修没有追加复审。没有运行 `LiaoliV6LayoutCheck`，也没有另开测试。编译通过不是游戏实测。

## 停写与所有权

源码作者是 `1f4155a4-e537-4518-a3f7-893f09c5daff`。实现报告写明没有范围外阻塞，并停止所有源码写入。契约同步只改了已有检查文件，报告写明该任务写入已经停止，且没有运行这份检查。

本批记录把 `LiaoliV6Layout.java` 和 `LiaoliV6Renderer.java` 记给这位作者。表作者的退回迁移已经停止写入，不写 `src`。检查文件由同一作者在契约同步里写入。

领域附魔底座记录的最后源码收束和 23:32 编译已经结束，覆盖不到这次 00:46 之后的姿态源码。火种共享燃料记录的源码停写和 21:36 编译也更早。这两处只说明那些任务当时已收束，不代替这次作者停写。

最后一次生产写入是 `LiaoliV6Renderer.java`，2026-10-30 00:47:36.0356603。检查文件更晚，00:53:56.3400226。编译开始于 00:56:57，晚于这两次。

编译前采样 2026-10-30T00:56:48.1734792+08:00。两份生产 Java、六张燎里 v6 贴图与实现证据一致；检查文件与契约同步证据一致。差异 0。当时没有正在进行的 Gradle 构建。已有空闲 daemon 73124，没有结束它。

## 编译

命令：`.\gradlew.bat compileJava --offline --no-daemon`

目录：`D:\alianximod`。`JAVA_HOME=C:\Program Files\Java\jdk-17`，`GRADLE_USER_HOME=C:\Users\baishang\.gradle`，沿用 `-Xmx3G`。日志：`docs/开发记录/术式批量制作/火系六式-20261029/liaoli-pose-gradle.log`。

开始 2026-10-30T00:56:57.0754540+08:00，结束 2026-10-30T00:57:38.5669724+08:00，退出码 0。`compileJava` 实际执行。BUILD SUCCESSFUL in 40s。4 个任务里 2 个执行、2 个 UP-TO-DATE。32 条待删除 API 警告，没有错误。警告没有让构建失败，本单不改这些调用。

这次单次进程已经结束。空闲 daemon 73124 仍在，不是这次启动的。

## 哈希

`src/main` 共 769 个文件。编译前后逐行清单相同，内容差异 0。清单是 `liaoli-pose-compile-src-main-before.txt` 和 `liaoli-pose-compile-src-main-after.txt`。

两份生产文件仍是：

- `LiaoliV6Layout.java` `98169A38537D34EA19B95DC9482A863D7A85E95562A2BC44AF2C49D599394C35`，2026-10-30 00:46:47.1639184
- `LiaoliV6Renderer.java` `ABB8B26D9696672275CE319F4B55E01050FD7CFCBCFEE3C3E1D9B236C3DC99AA`，2026-10-30 00:47:36.0356603

六张 `textures/effect/liaoli_v6/` 贴图哈希未变。检查文件仍是 `45BC9DB109B6CE2A5F1C8A8C83F5FC2CCB4F0B9F14F1AAC84A6E64C28F2A9173`。

## 未跑

没有运行 `LiaoliV6LayoutCheck`。没有 `build`、`jar`、`reobf`、`runClient`，没有启动游戏，没有重跑名称门禁。

本单写入到此停止。不自行写表。

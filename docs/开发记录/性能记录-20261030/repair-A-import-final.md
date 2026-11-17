我先读取这份返修说明，确认授权范围后再改。第 5 行仍是错误的 `java.io` 导入。我先保存修改前副本，再只改这一条包名。# A 导入返修最终报告

这是明确步骤的单点返修，不是新设计，也不是正式复审，不增加校验轮次。只改了一处 import。没有跑命名脚本、测试、`gradlew` 或游戏，没有打包。

## 结论

`PerformanceRecorder.java` 第 5 行原来的 `java.io.FileAlreadyExistsException` 已改为 `java.nio.file.FileAlreadyExistsException`。该类属于 `java.nio.file`，不是 `java.io`。源文件在修改前并未被别人修好，所以按授权改了这一条。

定向逐行对比修改前副本和现文件：796 行对 796 行，只有第 5 行不同。没有改逻辑、格式、接口、其他源码、文档或制作表。

## 修改位置

`src/main/java/com/bslx/XueXiMod/debug/performance/PerformanceRecorder.java` 第 5 行。

修改前：

`import java.io.FileAlreadyExistsException;`

修改后：

`import java.nio.file.FileAlreadyExistsException;`

同文件仍保留 `java.io.File` 与 `java.io.IOException`。字节数从 34455 增至 34461，相差 6 字节，与 `io` 换成 `nio.file` 的长度一致。

## 哈希

修改前副本：`docs/开发记录/性能记录-20261030/before-A-import-repair/PerformanceRecorder.java`

- 备份 UTC：`2026-10-29T20:58:22.709Z`
- 修改前 sha256：`897A55048AE9C1AAA6CF8FE8D47E8FE8A53C9C73240421555D79244082473065`
- 备份 sha256：`897A55048AE9C1AAA6CF8FE8D47E8FE8A53C9C73240421555D79244082473065`
- 修改前字节：34455

修改后源文件：

- 对比 UTC：`2026-10-29T20:58:36.848Z`
- 修改后 sha256：`375D26B74F181922F4BAC10FB47364880A62C811ECB903881035DD8105D430DA`
- 修改后字节：34461

报告 `repair-A-import-final.md`：2184 字节，sha256 `C22EB62CD18BD352B524A5DE415111134DDB1DDE091097FED26FB2E36692536D`，哈希时间 `2026-10-29T20:58:54.049Z`。

证据 `repair-A-import-evidence.json`：1447 字节，sha256 `052E168D9D0538C03FDA09596AE494EBC28F7C8283F5AACAA177EB7877B9D876`，哈希时间 `2026-10-29T20:59:04.604Z`。证据记录了报告哈希；自身哈希不写回文件。

## 编译与实测

待统一编译。本次没有编译授权，没有运行 `gradlew`，没有启动游戏，没有打包。不能从这次改动推断编译已经通过，也不能推断游戏效果。

## 所有权

A 对 `PerformanceRecorder.java` 的这一处导入返修已经写完，所有权释放。一并释放本目录的 `before-A-import-repair/PerformanceRecorder.java`、`repair-A-import-final.md` 与 `repair-A-import-evidence.json`。

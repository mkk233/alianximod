我先读编译修复的授权范围，再只改它允许的部分。四条转发少了末尾的显式宿主参数。我只补上默认 `false`，灼地那条仍是 `true`。灼地编译错误返修已停写。只改了 `ReleaseDefinitions.java` 里 4 处少一个参数的 `define` 转发。设计仍是 2/2，实现仍是 3/3，这次没有追加复审。

最长的 `define` 需要末尾的 `domainHostMarked`。下面 4 处转发原来停在多重释放说明和复制说明，现在各补一个 `false`，前面的参数没有动：

- `cohortSword` 第 53–57 行，补在第 57 行
- 带多重释放说明的重载，第 76–80 行，补在第 80 行
- 只带混链拒绝的重载，第 89–93 行，补在第 93 行
- 带能量缩放的重载，第 102–106 行，补在第 106 行

这 4 处旧定义的原有参数都还在，显式宿主是默认 `false`。第 109–119 行的能量链重载本来就传 `false`，没有改。第 44 行灼地仍传 `true`。登记数值和术式没有改。

文件：`D:\alianximod\src\main\java\com\bslx\XueXiMod\shu\technique\release\ReleaseDefinitions.java`  
LastWriteTime：`2026-10-30 22:36:31.732`  
SHA256：`AA7066898DA2D95CFE9905C44D13F5AC71A71812D4287B63C3ECACA9A77DDEFD`

命名门禁退出码 1。本次文件没有命中。剩下的是未改的 `OriginalHumanoid.java:165` 变量 `i`，以及 `:202` 变量 `to`。

没有编译。统一编译仍待主会话派原编译者重跑 `.\gradlew.bat compileJava --offline --no-daemon`。

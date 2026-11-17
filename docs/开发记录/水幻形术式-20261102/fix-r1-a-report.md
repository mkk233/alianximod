# 水幻形 A 组第 1 轮返修报告

只修复核问题 2 需要的主人查询。没有改 D/C/B，没有改 `ClonePresentationNotice` 构造，没有改 S2C 线格式，没有复审另外两个问题。

**未编译。未进游戏。** 本报告写完后停止写入。

命名门禁已执行 `powershell -NoProfile -File tools/check-name-gate.ps1`，退出码 0。

## 改动

`CloneService.findPresentationOwner`，170–187 行：

`public static @Nullable ServerPlayer findPresentationOwner(MinecraftServer server, UUID deploymentId)`

只接受当前服务器线程。`CloneRuntime.get(server)` 只取这一台服务器自己的运行态。包内 `CapturedResponseRuntime.presentationOwnerId`（102–114 行）从该运行态的部署表读取仍未释放、且 `sessionToken` 等于本次会话的租约，返回租约上的 `ownerId`。再在这台服务器的在线名单里找这名玩家；断线或已移除则返回 `null`。不读身体实体，不保留跨世界的 UUID 表。维度是否相同由 D 在发送前判断。

## 发布时点

`publish` 同步调用接收器，下面几处发出时部署仍在当前会话的表里：

- 驻留散水：`CapturedCasts` 209 行，转票并登记之后、`endActor` 之前。随后的身体结束保留租约，不在这次发布前摘表。
- 体积斩击：`CapturedResponseRuntime` 266 行，`submit` 成功之后、散水和结束身体之前。租约已是 `VOLUME`，尚未 `release()`。
- 同一次执行的散水：268 行，仍在结束身体之前。
- 失败散水：286 行，288 行 `release()` 摘表之前。

因此 D 在 `relay` 里按 `deploymentId` 调用上述方法时，能查到仍有效的部署主人。不需要额外的全局表。

没有自行追加复审。

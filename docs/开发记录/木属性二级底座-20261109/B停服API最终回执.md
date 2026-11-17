# B停服API最终回执（主会话转达）

- task: author-effects；session `2cae2855-f675-4742-aba1-d28598279385`。
- attempt: `a20261109100522753b9cf`。
- 最终状态：reported / reported_done / valid / writesSettled=true；原进程结束。
- 本次新增公开签名：`CounterService.onServerStopping(MinecraftServer server)`。
- 作者报告：要求服务端线程，清空全部内存窗口、待反击和当前线程残留帧/反击标记，不依赖在线列表；重复调用无害，不改持久化。
- 已更新通用效果作者交付.md。B源码现已收束，可供C只读接线；仍由B唯一拥有写权限。
- 这只是作者最终工作回执，不是返修复审通过。未运行检查脚本或编译。

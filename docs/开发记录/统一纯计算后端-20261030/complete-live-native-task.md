# 作者A：完成现有调用的显式native分发

固定Cursor CLI --model grok-4.7-high-fast，继续Agent会话157c03a0-8753-46c8-bfd4-42136cdeb189。设计2/2、实现3/3已用完，不追加复审。这是根据你implementation-A-final.md明确交回的接线边界，继续完成用户步骤4/5要求，不重新审查既有公式。

你报告“当前每实体/每攻击体入口始终直接调用JavaReference；native只在尚未被现有循环使用的批量入口”。本单补齐显式启用C后端后，现有已接入数学调用确实能进入C。不能只留下必须由未来代码调用的C入口然后把当前游戏可切换后端写成已完成。

行为要求：默认java继续低开销直达参考公式，无批量临时数组。用户明确设置-Dxueximod.compute.backend=native并提供有效library、握手能力成功时，当前已接入的轨迹、转向/步长、走廊数学调用可以真实进入对应JNI操作；无需新增未知开关或假造8条重复数据。单条用现有batch ABI count=1即可，不能延迟/重排游戏动作。实际批量入口可保留文档化的小批量Java阈值，但它不应让显式native模式下的所有现有适配永远留在Java。单条JNI可能更慢，文档明确此模式供后端接入/对比，不声称性能提升。

独占可写：shu/compute/ComputeCalls.java、ComputeSelection.java、必要的新增Java侧分发辅助类，已有4个适配类，docs/纯计算后端使用说明.md，本目录implementation-A-live-native-final.md、evidence-A-live-native.json、backup-A-live-native/。性能枚举若无需新增勿改；现有计数按一次逻辑请求对应实际Java/native/fallback，不因内部batch重入重复。保留当前ABI和数学，不重复实现公式。

作者B即将并行写C并只读以下冻结文件：abi-contract.md、JavaReference.java、ComputeCodes.java、ComputeOutput.java、XuexiComputeNative.java。你禁止改这些文件，也不写native/compute，直到B最终返回。若分发确实要求改ABI，先最终返回具体缺口，不与B并发改协议。只在本单最终报告说明abi文档末尾“标量始终Java”的状态句待总文档收尾同步，不在B读契约时修改它。

native侧使用有界缓冲，避免把每次native调用变成大量不受控临时分配；输入数字不包含世界对象，输出在成功后交回原适配。注意重入或不同线程不能共享错误输出，缓冲不得跨次泄露；加载失败、能力缺失/非0状态和链接错误继续同输入Java回退，原业务异常不吞。遵循原有精度边界，不改变JavaReference/玩法阈值。

本单不编译Java/C、不跑独立测试/游戏、不build/jar、不做新正式复审、不动共享注册器/网络/领域/制作表。如审批拒绝保留原因，不绕过安全设置。按允许工具保留自己修改前备份，记录路径/长度/UTC时间，无需hash。最终反馈实际现有调用如何在显式native模式分发、默认Java与回退路径、文档更新、未完成及released=true，待统一编译。

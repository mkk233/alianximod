继续当前网络writer任务。唯一GPT-6-astra核对当前GuardSyncPacket后已裁决以下最小修正，请执行，不再派代理或重做设计：
1. 同激活同revision同sequence的FULL/POSE不重复applyPose重置poseStartMillis。覆盖字段前计算freshActivation、sameRevision、sameSequence；anchorAge允许向前校准，FULL可更新尺寸元数据，只有新激活/更新有效姿态才applyPose。不要把公共packetLags的<改为<=，REMOVE同序仍须能删。
2. POSE必须packet.revision==view.revision，保持FULL建立新revision，POSE同代更新。当前允许高revision POSE抢占状态需修。
3. 添加定向回归验证重复FULL/POSE保留poseStartMillis，合法新POSE更新；高revision POSE不改变view。
不新增tombstone、不改animationStart类型/身份协议，正常连接发送有序，无实际异步旧FULL晚于REMOVE路径，脑子已裁决不扩scope。
其余文件边界仍wire-task.md，渲染writer已结束但别改渲染文件。更新wire-report.md并结束，不跑Gradle。只修上述明确问题。

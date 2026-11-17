# A返修：先固定真实发布观察接口

固定Cursor CLI --model grok-4.7-high-fast，继续A原可写会话。读本目录review-decision-1.md，这是实现1/3轮正式返修清单，设计2/2耗尽，不另审不重设计。

本单只写repair-commit-contract.md（协调文档），暂不改源码。你作为唯一预算/执行作者，给B精确的公共发布观察接口包名、类、方法、返回值与调用时点，按裁决R1/R2/R3全部职责：可嵌套每round/member/copy尝试、成功报告幂等、取消后禁止新发布、在途回执结清前不退款、done实际结清后才真、HostGroup原子路径不误用松散成功。B只需在实际addFreshEntity接受或剑控制状态真正提交后调用通知，并在发布前查是否允许；明确“返回false不提交、返回true后异常仍保留费用”的用法。不能把实体数量当收费份数。

接口用现有ReleaseExecutionContext衔接，贴合本轮批准的实现修复，不另造全局事务/新Mixin，不按术式ID。不需要再次读整仓；可读自己AutomaticRound、GroupBudget、SkillSegmentRunner、bind现行实现来确定签名。禁止写B/C文件或读取进行中源码。此单不编译、不测试、不命名门、不改权限、不改表。

只落精确契约并给最终反馈后结束；主会话随后让你完整实现A并让B并行按同契约接入。不要在本单提前写一半源码或长驻等待。原用户需求和正式契约保持，修的是已确认费用窗口。

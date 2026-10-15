---
name: mod-dispatch
description: Use when building or reviewing a mod with third-party subagents, or when the user says 调度, 用ds, 问脑子, or 4.7审核.
when-to-use: 调度 用ds读取 问GPT 并行改 4.7审核 做mod
user-invocable: true
argument-hint: "[任务]"
license: MIT
---

# 调度

命令：`/mod-dispatch`

给其他模组用的调度命令。不包含任何一个具体模组的设计。主代理只调度。每次 `spawn_subagent` 必须写 `model`。不写会继承当前会话的模型。禁止 `grok-4.5`。

| 活 | model |
|---|---|
| 只读通读 | `deepseek-flash` |
| 改文件 | `deepseek-v4-pro` |
| 架构、根因、并发、数据一致性 | `gpt-6-luna`（`brain` / `gpt6`） |
| 一项改完立刻审 | `grok-4.7` |

难度高于「改一个文件、查一个符号、跑一条命令」时，先问 `gpt-6-luna`，再派 DeepSeek 落地。

## 流水线

1. 按文件切开。两人不能写同一文件。通读开满，一块一个 `deepseek-flash`。
2. 只读。prompt 写绝对路径、禁止改文件、要路径和行号。截断就 `resume_from` 同一个，不要另开一个猜。
3. 齐了由主代理压成事实清单。只开一个 `gpt-6-luna`。没拍板不改代码。
4. 只改已拍板且用户没否决的项。互斥文件用 `deepseek-v4-pro` 并行。
5. 一个改完立刻 `grok-4.7` 审。不过就同一文件再派 DeepSeek，再审。没跑的编译或游戏内验证要写明。

子代理 prompt 写范围、目标、约束、产出。不要把整仓贴进去。

项目自己的构建命令、包名和设计文档以该项目的说明为准，不要写死在这个技能里。

接第三方模型用 `/mod-add`。这个命令不改模型配置。

## 红线

| 借口 | 现实 |
|---|---|
| 模块不多，我自己读更快 | 通读交给 flash |
| 先改着，GPT 回头看 | 没拍板不改 |
| 一起审省事 | 一项一审 |
| 省略 model | 会继承主会话模型 |

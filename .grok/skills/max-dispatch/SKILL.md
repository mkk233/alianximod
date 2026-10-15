---
name: max-dispatch
description: Use when the user wants max-parallel codebase audit or implementation with DeepSeek and GPT, or says 用ds读取, 汇总给gpt, 拉满效率, 最大数量子agent, 通读审查, 并行改然后4.7审核. Use when reviewing a whole mod/repo/module with as many subagents as allowed.
when-to-use: 用ds读取 汇总给gpt 拉满效率 最大数量子agent 通读 并行改 4.7审核 DeepSeek GPT 子模型
user-invocable: true
argument-hint: "[read|fix] [scope]"
---

# Max Dispatch

两条先于流水线：

1. **并行优先。** 主代理只调度。通读、审查、并行修改走下面的模型表，不自己通读整仓。
2. **难度升高就先问。** 这一步只要比照着改、读一个文件、跑一条命令更难一点，或明显难，先问 `gpt-6-luna`，不要先自己试。问完再派 DeepSeek 落地，一项结束立刻用 Grok 4.7 审。

**不写 `model` 会继承主会话 Grok。每次 spawn 必须显式传 model。禁止 grok-4.5。**

## 模型

| 活 | model（必填） |
|---|---|
| 只读通读 | `deepseek-flash` |
| 改文件 | `deepseek-v4-pro` |
| 不合理/架构裁决 | `gpt-6-luna`（别名 `gpt6`） |
| 单项改完审核 | `grok-4.7` |
| 禁止 | `grok-4.5`、把 Grok 当通读/改代码子模型 |

## 流水线（不可跳步）

1. **切块** — 按文件/包切开，两个人不能写同一文件。先数并发上限（常见 32），通读开满，一块一个 flash。
2. **通读** — 只读。prompt 写死路径、禁止改文件、要结构化中文摘要。截断就 `resume_from` 同一子代理补全，不要另开一个猜。
3. **等齐再汇总** — 父代理压成事实清单（路径+行号+现象），**只开一个** `gpt-6-luna` 做脑子。GPT 没拍板前不要改代码。
4. **改** — 只改 GPT 已确认且用户没否决的项。互斥文件则 `deepseek-v4-pro` 并行（用户说 N 并行就 N，上限内）。用户没让删的系统不要删。
5. **一项结束立刻审** — 该 DS 一结束就 spawn `grok-4.7` 审这一项，不要等 15 个都改完再审。不过 → 同一文件再派 DS 修 → 再 4.7。全过后再向用户汇报。没跑的验证（编译/测试）必须明说。

## 子代理 prompt

- 范围：允许读/改的绝对路径
- 目标：一条结论，不要整仓
- 约束：禁止改范围外文件
- 产出：路径、行号、做了什么 / 发现什么

## 红线

- 自己通读整仓、自己大面积改
- `model: grok-4.5` 或省略 model
- GPT 还没裁决就开始改
- 两个 writer 碰同一文件
- 全部改完才审
- 截断报告当完整

| 借口 | 现实 |
|---|---|
| 模块不多，我自己读更快 | 通读就是为了拉满并行，开 flash |
| 先改着，GPT 回头看 | 没有裁决就没有改动清单 |
| 一起审省 spawn | 用户要的是一项结束立刻 4.7 |
| 不写 model，配置会钉 DeepSeek | 省略会继承 Grok |

---
name: mod-add
description: Use when the user says 添加模型, 第三方模型, 中转站, or /model is missing Gemini, DeepSeek, GPT, or image models.
when-to-use: 添加模型 第三方模型 中转站 /model Gemini 图像模型
user-invocable: true
argument-hint: "[--video]"
license: MIT
---

# 添加模型

命令：`/mod-add`

把中转站上的模型写进 `~/.grok/config.toml`，使 `/model` 能选到。给任何项目用，不绑定某一个模组。密钥只从已有第三方 `[model.*]` 条目复制。不要把密钥写进技能、仓库或对话。不要改 `[models].default`。

## 初次询问

状态文件：`~/.grok/skills/mod-add/state.json`。不提交进仓库。

文件不存在时先问，等回答再改配置：

> 要把中转站上还没进 `/model` 的第三方模型加进去吗？包含对话模型和图像模型。视频模型默认不加。

- 同意：写入 `{"asked":true,"add_models":true}`，然后添加。
- 拒绝：写入 `{"asked":true,"add_models":false}`。以后不再问，也不自动加。
- 用户之后说「添加模型」或运行本命令并要求添加：把 `add_models` 改成 `true` 再添加。

`add_models` 已是 `true` 时不再询问。只补配置里还没有的 id。参数含 `--video` 时才加入 id 里带 `video` 的模型。

## 写入

1. 从已有第三方条目读取 `base_url` 和 `api_key`。没有第三方条目就停下来问地址和密钥，不要编一个。
2. `GET {base_url}/models`。
3. 每个新 id 追加一段。**id 含 `.` 时表名必须加引号。** 不加引号时 TOML 会把名字拆开，`/model` 里只剩半截，`grok inspect` 会出现 `unknown field`。

```toml
[model."example-model-1.5"]
model = "example-model-1.5"
base_url = "http://relay.example/v1"
name = "Example Model 1.5"
description = "第三方模型"
api_backend = "chat_completions"
context_window = 32000
api_key = "从已有条目复制"
```

对话模型的 `context_window` 用 `1000000`，图像模型用 `32000`。`api_backend` 用 `chat_completions`，除非该条目明确要求别的协议。

4. 跑 `grok models`。列表中的名字必须和 id 完整一致。
5. 告诉用户：当前会话的 `/model` 是启动时读的，新开一个 Grok 会话再选。

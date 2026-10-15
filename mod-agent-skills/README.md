# Mod agent skills

两个 Grok 技能，给做模组的人调度第三方子模型。不含任何一个具体模组的设计、密钥或进度。以 MIT 许可提供，见 `LICENSE`。

| 命令 | 目录 | 作用 |
|---|---|---|
| `/mod-dispatch` | `mod-dispatch/` | 调度：通读、裁决、改代码、审核 |
| `/mod-add` | `mod-add/` | 添加：把中转站模型写入 `/model` 列表 |

## 安装

把 `mod-dispatch` 和 `mod-add` 两个目录复制到 `~/.grok/skills/`，新开一个 Grok 会话。斜杠菜单里会出现 `/mod-dispatch` 和 `/mod-add`。

## `/mod-add`

第一次运行会询问是否把中转站上的对话模型和图像模型加进 `/model`。视频模型默认不加，需要时给命令加上 `--video`。

选择写在 `~/.grok/skills/mod-add/state.json`，不要提交。密钥只放在 `~/.grok/config.toml`。模型 id 里有 `.` 时，配置表名必须加引号，例如 `[model."example-model-1.5"]`。

当前会话的 `/model` 列表在启动时读取。添加之后要新开一个 Grok 会话才能选到新模型。

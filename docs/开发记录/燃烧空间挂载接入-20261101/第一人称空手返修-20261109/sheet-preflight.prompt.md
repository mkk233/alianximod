# fire_burn 唯一表格写回者：当前状态对账与返修入制作中

固定模型与统一启动器契约。先完整读TaskFile、记录.md、docs/统一开发与术式批量制作流程.md第2/8/9节，按需要读取当前spreadsheets技能 C:/Users/baishang/.codex/plugins/cache/openai-primary-runtime/spreadsheets/26.1104.22356/skills/spreadsheets/SKILL.md（若目录名不同定位准确SKILL.md，不凭猜测）。只操作制作表的fire_burn，不改源码，不编译，不游戏，不领取其他编号。

用户2026-11-09反馈第三人称正确，第一人称空手火焰下移。主会话已决定定位并修复该运行挂点，完整术式尚不能由这句话判定已完成。旧来源：docs/开发记录/燃烧空间挂载接入-20261101/sheet-writer-report.md以及快照/批准要求与资源映射-20261101-214704-1.md、变更前-NODV-20261101-214704-1.json。旧row为待验收7仅历史，不凭旧行号改表。

本阶段唯一写入者是你；先读取当前工作簿四状态页和资源明细、hash/mtime、最新N/O/DV和本项快照，确认当前是否有其他作者声明占表。若不能确认独占或源hash变化，保留候选返回，不覆盖。只有fire_burn在待验收且能取得最近已批准完整N/O快照时，按第8节备份候选整行A:EN+相关明细迁回制作中，恢复最新批准完整N/O并合并当前用户改动；DV保留全文追加有日期明确问题与第三人称通过事实，引用本子目录。若已经制作中只追加本次反馈，不覆盖后来的需求。若历史已完成，按流程不把历史行迁出，不以此认定这次修复验收完成，单独报告状态。若编号重复/不存在或无快照，返回具体待澄清事实，不瞎补、不扩scope。

不要在这阶段迁待验收/已完成。四页总数与唯一性、源表提交前hash/重开核对，按流程保真；保留格式、公式、验证、批注、用户后续内容和原始档案。输出本子目录表格/preflight-report.md和JSON，记录原当前N/O/DV、页行、快照来源、实际写回有无、前后hash、独占证据与最终停止写入。后续编译完成后再明确Resume你更新待验收与资源索引，本次不动总资源索引。
最终必须附一块标记JSON，标记各独占一行：
<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done","summary":"实际结果","changedFiles":[],"checks":[],"notRun":[],"writesSettled":true,"blocker":null,"nextAction":"下一步"}
<<<END_CURSOR_TASK_REPORT>>>
workStatus仅reported_done/blocked/needs_input/failed；数组项为字符串，不写taskId/attemptId/session/inputHash。返回完整最终结果后停止，不流式过程汇报。
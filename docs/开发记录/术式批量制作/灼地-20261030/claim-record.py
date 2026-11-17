# -*- coding: utf-8 -*-
"""Write the batch record from the verified claim snapshot."""
import html
import json
import re
import zipfile
from pathlib import Path

batch = Path(r"D:\alianximod\docs\开发记录\术式批量制作\灼地-20261030")
snapshot = json.loads((batch / "claim-snapshot.json").read_text(encoding="utf-8"))
reopen = json.loads((batch / "claim-reopen.json").read_text(encoding="utf-8"))
inventory = json.loads((batch / "claim-inventory.json").read_text(encoding="utf-8"))
live = Path(r"D:\alianximod\docs\术式制作表.xlsx")

with zipfile.ZipFile(live) as archive:
    comment_xml = archive.read("xl/comments/comment2.xml").decode("utf-8")
comments = []
for block in re.findall(r"<comment\b[\s\S]*?</comment>", comment_xml):
    ref = re.search(r'\bref="([^"]+)"', block)
    texts = re.findall(r"<(?:[\w]+:)?t(?: [^>]*)?>([\s\S]*?)</(?:[\w]+:)?t>", block)
    comments.append((ref.group(1) if ref else "", html.unescape("".join(texts))))

lines = []
add = lines.append
add("# 灼地-20261030")
add("- scope：完整术式。本单只领取并留原始档案，不实现、不编译、不迁待验收。")
add("- 阶段：制作中。")
add("- 作者：主会话调度。写回执行者是本会话。源码作者尚待指定。另一 Cursor intake 只读，不写表、不写源码。")
add("- 写回执行者：本会话 `8a5b872e-ba31-4d30-a430-5ca648e47013`，Cursor CLI Grok 4.7 High Fast。允许路径是 `docs/术式制作表.xlsx` 和 `docs/开发记录/术式批量制作/灼地-20261030/`。")
add("- 文件所有权：`docs/术式制作表.xlsx` 与本批记录由本会话写入。`src` 本单不改。")
add("- Cursor 会话/日志：`claim-task.md`，model grok-4.7-high-fast，会话 `8a5b872e-ba31-4d30-a430-5ca648e47013`。开批时其他同工作区 Codex 会话在 list_threads 中均为 idle 或 notLoaded，只有主会话 active。这只是开批快照，最终编译前另查。")
add(f"- 源表哈希 / 最近更新：领取基线 SHA256 `{snapshot['baselineSha256']}`，LastWriteTime `{snapshot['sourceLastWriteTime']}`，{snapshot['sourceBytes']} 字节。提交后 SHA256 `{reopen['sha256']}`，LastWriteTime `{reopen['mtime']}`，{reopen['bytes']} 字节。")
add("- 项 fire_scorch_field 灼地：领取时在待制作第 4 行。名称、编号、类型、属性、释放和链上角色见下面原文。弹道 R 为空，本次不补猜。")
add("- 需求快照：下面 N/O/DV 是领取前原文。最新已批准需求版本是该基线表中的现行原文，加上用户本次“实现待制作表中灼地”的指令。本单没有把 N/O 改成待验收展示，也没有把设计反馈写进 DV。")
add("- 待验收展示：尚未进入待验收，N/O 不转换。")
add("- 资源映射：特效仍是文字意图，没有已确认的资源文件名。首条火种配置在主行 DW:EN。第二条在资源状态明细第 7 行、配置序号 2。本单没有新归档、没有把确认版复制进 `src/main/resources`。")
add("- 设计反馈：本单不判断设计或实现正确性，不追加 `[设计反馈]`。")
add("- 批准版本与证据：批准版本是基线 `83D13762D627FF953A184F5B7F6247E88A972ED9FA139099D6FFA5A12DFEFB9E` 上的现行行，以及本批实现指令。历史 `docs/开发记录/灼地产种设计入表-20261030` 只是输入编辑记录，不是已实现，也不是本编号的制作校验。")
add("- 产物：尚无代码路径、游戏资源路径或确认版归档。")
add("- 校验轮次：检索本编号的制作校验记录，`记录.md` 中没有 `fire_scorch_field`。因此本批沿用设计 0/2、实现 0/3，不自定新额度，本单也不消耗轮次。当前唯一 Astra 等待事实未判断，后续由主会话下达准确轮次。没有独立测试阶段。")
add("- 复审时点：状态对账不是正式正确性复审。本单不重置阶段校验计数，也不做正式复审。")
add("- 编译 / 逻辑复核 / 未跑验证：编译待统一编译，本单没有编译授权，没有跑 `compileJava`。游戏未跑。名称门禁未跑，因为没有改 Java。")
add(f"- 原始档案：待制作原始数据第 9 行。源头 `docs/术式制作表.xlsx` 待制作第 4 行，源 LastWriteTime `{snapshot['sourceLastWriteTime']}`，源哈希 `{snapshot['baselineSha256']}`，采集时间 `{snapshot['capturedAt']}`。备份 `{snapshot['backupName']}` 与该基线相同。只追加这一次，没有覆盖已有 6 条档案。档案 DV 没有流程标记。资源明细原文记在该行 EX/EV，活页明细仍在第 7 行。")
add(f"- 表迁移：`fire_scorch_field` 从待制作第 4 行迁到制作中第 6 行，成功。时间 `{reopen['mtime']}`。提交后哈希 `{reopen['sha256']}`。重开核对：已完成 40、待制作 13、制作中 4、待验收 3，合计 60，四页编号唯一。待制作已无该编号。档案内同编号只此一条首次快照，不计入四页重复。数据表与备份逐字节相同。资源状态明细序号仍是 2。")
add("- 实测：游戏未跑。实现和统一编译都还没做，本单不交实测四行来代替尚未存在的实现。")
add("- 下一步 / 阻塞：源码作者尚待指定。指定后再实现。编译仍待统一编译，且要等相关写入收束。最终编译前另查同工作区会话。")
add("")
add("## 非空列")
add("")
for cell in inventory["targetCells"]:
    header = cell["row2"] or cell["row1"] or ""
    add(f"### {cell['column']} {header}")
    add("")
    add(cell["text"])
    add("")
add("## 资源状态明细第 7 行")
add("")
labels = {
    "A": "术式编号", "B": "配置序号", "C": "配置启用", "D": "资源", "E": "状态", "F": "操作",
    "G": "作用对象", "H": "触发时机", "I": "数值", "J": "状态amplifier", "K": "寿命策略",
    "L": "寿命tick", "M": "周期tick", "N": "首次延迟tick", "O": "计量单位", "P": "生效条件",
    "Q": "结算约束", "R": "迁移来源", "S": "关联说明", "T": "附加明细键",
}
for column, label in labels.items():
    value = snapshot["detail"].get(column)
    add(f"- {column} {label}：{value if value not in (None, '') else '空'}")
add("")
add("## 待制作表头批注")
add("")
add("这些批注都在表头第 2 行，没有锚在灼地数据行。搬行时没有改批注。")
add("")
for ref, text in comments:
    add(f"### {ref}")
    add("")
    add(text)
    add("")
add("## 提交核对")
add("")
add(f"- 四页：已完成 {reopen['counts']['已完成']}，待制作 {reopen['counts']['待制作']}，制作中 {reopen['counts']['制作中']}，待验收 {reopen['counts']['待验收']}。")
add(f"- 四页编号 {reopen['total']}，唯一 {reopen['unique']}。")
add(f"- 制作中第 6 行名称 {reopen['makingName']}，伤害 {reopen['damage']}，冷却 {reopen['cooldown']}，弹道仍空：{reopen['trajectoryEmpty']}。")
add(f"- 制作中 N/O/DV 与快照一致：{reopen['makingN']} / {reopen['makingO']} / {reopen['makingDv']}。")
add(f"- 档案 N/O/原文 DV 一致：{reopen['archiveN']} / {reopen['archiveO']} / {reopen['archiveDvOriginal']}。档案不含流程标记：{not reopen['archiveHasMarker']}。")
add(f"- 明细第 7 行 {reopen['detailId']}，序号 {reopen['detailSeq']}，数值 {reopen['detailValue']}，周期 {reopen['detailPeriod']}。")
add(f"- 数据表未改：{reopen['dataSheetUnchanged']}。档案说明行仍在：{reopen['noteKept']}。")
add("- 受影响区域已渲染：`claim-preview-making.png`、`claim-preview-pending.png`、`claim-preview-archive.png`。制作中第 6 行、待制作前部接上的陨岩、档案第 9 行都能读到名称和编号。")
add("- 三张改动工作表没有 `#REF!` 等公式错误。其余部件与备份逐字节相同，不用整本再导出覆盖源表。")
add("")
(batch / "记录.md").write_text("\n".join(lines), encoding="utf-8")
snapshot["submitted"] = True
snapshot["submittedSha256"] = reopen["sha256"]
snapshot["submittedLastWriteTime"] = reopen["mtime"]
(batch / "claim-snapshot.json").write_text(json.dumps(snapshot, ensure_ascii=False, indent=2), encoding="utf-8")
print("record-bytes", (batch / "记录.md").stat().st_size)

# -*- coding: utf-8 -*-
import json
from pathlib import Path

work = Path(r"D:\alianximod\docs\开发记录\燃烧空间挂载接入-20261101\第一人称空手返修-20261109\表格")
prep = json.loads((work / "move-prepare-result.json").read_text(encoding="utf-8"))
nodv = json.loads(Path(prep["snapshots"]["nodv"]).read_text(encoding="utf-8"))
report = {
    "workStatus": "reported_done",
    "id": "fire_burn",
    "from": prep["from"],
    "to": prep["to"],
    "countsBefore": prep["countsBefore"],
    "countsAfter": prep["countsAfter"],
    "totalBefore": 62,
    "totalAfter": 62,
    "uniqueAfter": 62,
    "snapshotSource": prep["snapshots"]["approved"],
    "currentSnapshot": prep["snapshots"]["nodv"],
    "fullRowSnapshot": prep["snapshots"]["aen"],
    "currentN": nodv["currentN"],
    "currentO": nodv["currentO"],
    "currentDV": nodv["currentDV"],
    "restoredN": nodv["restoredN"],
    "restoredO": nodv["restoredO"],
    "restoredDV": nodv["restoredDV"],
    "detailRowsMoved": 0,
    "archiveUntouched": True,
    "written": True,
    "hashBefore": prep["baselineSha256"],
    "hashAfter": prep["candidateSha256"],
    "mtimeBefore": prep["baselineMtime"],
    "mtimeAfter": "2026-11-09T17:26:58.348+08:00",
    "backup": prep["backup"],
    "candidate": prep["candidate"],
    "exclusive": "读取时源表哈希等于水流写回已停写后的 7EE23F670605C912A393457C9DEE61663BDE69BBF06996CA7DA78ACF04275FE5，LastWriteTime 2026-11-09 10:34:50。多重与分裂扩展尚未授权写表。提交窗口以 FileShare.None 独占打开，锁内哈希仍等于该基线后才覆盖。",
    "reopen": "重开后字节等于候选，哈希 137F6BC91F1AA2F61A5D28A051F316348DB84E8C845BFDA8D2BF3FFC6F411145。fire_burn 只在制作中第19行。四页 37/0/17/8，共62且编号唯一。档案第7行仍在。资源明细无该编号。相对备份只变 sheet3、sheet4、workbook.xml。共享字符串、样式、批注、数据表、档案和资源明细字节未变。",
    "stopped": True,
}
(work / "preflight-report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
print("wrote", work / "preflight-report.json")

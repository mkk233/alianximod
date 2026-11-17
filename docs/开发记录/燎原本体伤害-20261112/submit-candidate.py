# -*- coding: utf-8 -*-
import hashlib
import json
import os
from datetime import datetime
from pathlib import Path

live = Path(r"D:\alianximod\docs\术式制作表.xlsx")
task = Path(r"D:\alianximod\docs\开发记录\燎原本体伤害-20261112")
candidate = task / "术式制作表-候选.xlsx"
meta_path = task / "candidate-meta.json"
meta = json.loads(meta_path.read_text(encoding="utf-8"))
before = live.read_bytes()
before_hash = hashlib.sha256(before).hexdigest().upper()
if before_hash != meta["baselineSha256"] or len(before) != meta["baselineBytes"]:
    raise SystemExit("Live workbook changed; candidate preserved and not submitted")
candidate_bytes = candidate.read_bytes()
candidate_hash = hashlib.sha256(candidate_bytes).hexdigest().upper()
if candidate_hash != "0C24BFA4EC405190964E9A3938FE21866112A694E8CE730B492B986B4C8BBF57":
    raise SystemExit("Candidate hash changed after reopen")
temporary = live.with_name("术式制作表.sheet-repair-move-01.tmp.xlsx")
temporary.write_bytes(candidate_bytes)
if hashlib.sha256(temporary.read_bytes()).hexdigest().upper() != candidate_hash:
    temporary.unlink(missing_ok=True)
    raise SystemExit("Temporary copy hash mismatch")
os.replace(temporary, live)
after = live.read_bytes()
after_hash = hashlib.sha256(after).hexdigest().upper()
stat = live.stat()
result = {
    "submitted": after_hash == candidate_hash,
    "beforeSha256": before_hash,
    "afterSha256": after_hash,
    "bytes": len(after),
    "mtime": datetime.fromtimestamp(stat.st_mtime).isoformat(timespec="milliseconds"),
}
(task / "submit-result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
if after_hash != candidate_hash:
    raise SystemExit("Submitted workbook hash mismatch")
print(json.dumps(result, ensure_ascii=False))

# -*- coding: utf-8 -*-
import hashlib
import os
from pathlib import Path

LIVE = Path(r"D:\alianximod\docs\术式制作表.xlsx")
CANDIDATE = Path(__file__).with_name("候选") / "术式制作表-queue-values-20261111.xlsx"
BASELINE = "59640cbc39cc00ecfc41c07ea5bcffecc1ead7622252432e3f6dd510a1f6c23f"


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


before = digest(LIVE)
if before != BASELINE:
    raise SystemExit("LIVE HASH CHANGED " + before)
payload = CANDIDATE.read_bytes()
handle = LIVE.open("r+b")
try:
    locked = handle.read()
    locked_hash = hashlib.sha256(locked).hexdigest()
    if locked_hash != BASELINE:
        raise SystemExit("LOCKED HASH CHANGED " + locked_hash)
    handle.seek(0)
    handle.write(payload)
    handle.truncate()
    handle.flush()
    os.fsync(handle.fileno())
finally:
    handle.close()
after = digest(LIVE)
print("committed", after)

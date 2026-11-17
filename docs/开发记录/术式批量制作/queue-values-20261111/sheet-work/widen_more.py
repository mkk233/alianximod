# -*- coding: utf-8 -*-
from pathlib import Path
path = Path(__file__).with_name("update_values.py")
text = path.read_text(encoding="utf-8")
old = 'width="32"'
if text.count(old) != 1:
    raise SystemExit(text.count(old))
text = text.replace(old, 'width="38"', 1)
path.write_text(text, encoding="utf-8", newline="\n")
print("38")

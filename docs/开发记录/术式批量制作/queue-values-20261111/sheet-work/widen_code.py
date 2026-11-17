# -*- coding: utf-8 -*-
from pathlib import Path
path = Path(__file__).with_name("update_values.py")
text = path.read_text(encoding="utf-8")
old = 'lookup_new = lookup_new.replace(\'ref="A1:BV16"\', \'ref="A1:BV17"\', 1)\n'
new = old + '    lookup_new = lookup_new.replace(\'<col width="24" customWidth="1" style="19" min="6" max="6"/>\', \'<col width="32" customWidth="1" style="19" min="6" max="6"/>\', 1)\n'
if old not in text:
    raise SystemExit("lookup dimension line missing")
text = text.replace(old, new, 1)
text = text.replace('ht="48"', 'ht="32"', 1).replace('ht="64"', 'ht="48"', 1)
path.write_text(text, encoding="utf-8", newline="\n")
print("widened")

# -*- coding: utf-8 -*-
from pathlib import Path
path = Path(__file__).with_name("update_values.py")
text = path.read_text(encoding="utf-8")
text = text.replace('ht="31.2"', 'ht="48"', 1).replace('ht="46.8"', 'ht="64"', 1)
path.write_text(text, encoding="utf-8", newline="\n")
print(text.count('ht="48"'), text.count('ht="64"'))

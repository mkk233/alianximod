import importlib.util
import zipfile
from pathlib import Path

spec = importlib.util.spec_from_file_location(
    "register_claim",
    r"D:\alianximod\docs\开发记录\术式批量制作\水流组合-20261103\sheet-work\register_claim.py",
)
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)

def summarize(path):
    with zipfile.ZipFile(path) as book:
        shared = mod.shared_items(book.read("xl/sharedStrings.xml").decode("utf-8"))
        print("FILE", Path(path).name)
        for title, part in mod.PARTS.items():
            if title not in ("已完成", "待制作", "制作中", "待验收", "待制作原始数据"):
                continue
            found = mod.status_b(book.read(part).decode("utf-8"), shared)
            print(" ", title, len(found), [(row, code) for row, code, _name in found if code in {
                "water_vortex", "water_bubble_column", "water_deep_pressure", "tide_sever", "water_yielding_ribbon"
            }])
        if "待制作原始数据" in mod.PARTS:
            archive = book.read(mod.PARTS["待制作原始数据"]).decode("utf-8")
            rows = mod.row_map(archive)
            for number in (24, 25):
                if number not in rows:
                    print("  archive", number, "ABSENT")
                    continue
                row = rows[number]
                print(
                    "  archive",
                    number,
                    mod.cell_value(row, "A", number, shared),
                    mod.cell_value(row, "B", number, shared),
                    mod.cell_value(row, "EQ", number, shared),
                    mod.cell_value(row, "ER", number, shared),
                    mod.cell_value(row, "EX", number, shared),
                    "制作中" in (mod.cell_value(row, "DV", number, shared) or ""),
                )

live = r"D:\alianximod\docs\术式制作表.xlsx"
stage = r"D:\alianximod\docs\开发记录\术式批量制作\水流组合-20261103\sheet-work\候选\术式制作表-待制作登记-20261103-192416-1.xlsx"
summarize(stage)
summarize(live)
with zipfile.ZipFile(live) as book:
    shared = mod.shared_items(book.read("xl/sharedStrings.xml").decode("utf-8"))
    making = mod.row_map(book.read(mod.PARTS["制作中"]).decode("utf-8"))
    for number, code in ((18, "water_vortex"), (19, "water_bubble_column"), (20, "water_deep_pressure")):
        row = making[number]
        print("LIVE", number, code, "Nlen", len(mod.cell_value(row, "N", number, shared)), "Ostart", mod.cell_value(row, "O", number, shared)[:24])
        print("  CR/CS/CT/CU/CV/CW", [mod.cell_value(row, col, number, shared) for col in ("CR", "CS", "CT", "CU", "CV", "CW", "BU", "BV", "F", "L", "R", "S")])

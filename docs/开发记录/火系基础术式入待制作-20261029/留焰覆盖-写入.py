# -*- coding: utf-8 -*-
"""Replace the pending 焰流 row with 留焰. Does not archive the old row."""
import hashlib
import html
import json
import re
import zipfile
from datetime import datetime
from pathlib import Path

root = Path(r"D:\alianximod")
batch = root / "docs" / "开发记录" / "火系基础术式入待制作-20261029"
live = root / "docs" / "术式制作表.xlsx"
output = batch / "留焰覆盖-candidate.xlsx"
feature = (
    "留焰是火系附魔，不是独立攻击，也不单独生成攻击体。它附在已经会移动的攻击载体上，沿载体真实经过的路径留下短暂存在的火焰轨迹。敌人接触这条火径时，按该载体自己的规则受到攻击。作用是把已经走过的路径变成短暂危险区域。它不是慢速漂移的火源，也不是持续朝一个方向喷射的光束。\n"
    "普通移动投射体装上留焰后留下直线火径，可以封住追击路线。风暴轨迹、留焰和兼容的移动攻击载体一起使用时，旋转运动不断留下火径，组合形成火焰风暴；载体例子是已有火球。喷涌、留焰和兼容移动载体一起使用时，沿上升和下落路径留下弧形火迹。这些只是组合关系，不表示通用载体或组合支持已经实现，也不把留焰写成能独立伤人的攻击。\n"
    "火径和原攻击共用同一次真实的施法上下文，以及对同一目标的命中计时。多个重叠片段不按片段数重复伤害。载体本体和火径也不能把同一次命中结算两次。不能按片段或按tick另造释放标记来绕过这次去重。留焰不额外复制一次攻击伤害。\n"
    "留焰自己不生产火种，也不消费火种。种火、引燃、引爆仍由各自的附魔提供，并继续遵守同一次释放去重和每个目标一份共享火种余额。引燃可以多层共存并逐段争用火种。引爆只使用当时的剩余余额。不能靠火径重叠把产种、挂层或即时引爆乘倍。\n"
    "不放置会蔓延的原版火方块，不破坏地形。它只是短时攻击轨迹及其表现，不生成陨石残留，也不取代灼地那样停驻在区域里的火场。"
)
visual = (
    "移动攻击体后方出现连贯的火焰尾迹，沿实际路径短暂停留，由旧到新衰减后熄灭。"
    "旋转时形成连续的弧形或环绕火径，喷涌时形成上升再下降的弧线。"
    "看得见的火径和有效判定位置对应，不把整个视觉漏斗都算成伤害。"
)
note = (
    "火径寿命、宽度、采样间距、片段上限、费用、冷却，以及不同载体的伤害映射和有效命中策略都未定稿。"
    "数值格留空，不用0代替。释放标签用持续，只表示火径会短暂留下；命中体存活刻数未定，所以不填。"
    "现有链上角色没有附魔或留焰这一项，弹道格留空。"
    "2026-10-29按用户授权用留焰覆盖本行，不另建条目，也不把组合写成已经实现。"
)
upwell_old = "需要有伤害的中央火柱时另装焰流并计费。"
upwell_new = "原先用独立焰流充当中央火柱的引用已失效。留焰不是独立火柱，只附在兼容的移动载体上沿路径留短暂火径。"
continue_old = "不改写焰流等其他载体的后续寿命。"
continue_new = "不改写其他载体的后续寿命。旧焰流作为独立载体的引用已失效。"


def fail(message):
    raise SystemExit(message)


def sha256(data):
    return hashlib.sha256(data).hexdigest().upper()


def col_index(letters):
    index = 0
    for char in letters:
        index = index * 26 + ord(char) - 64
    return index


def rows_of(xml):
    return re.findall(r"<(?:[\w]+:)?row\b[^>]*>[\s\S]*?</(?:[\w]+:)?row>", xml)


def row_number(row):
    return int(re.search(r'\br="(\d+)"', row).group(1))


def cell_text(row, column, number):
    match = re.search(rf'<c r="{column}{number}"[^>]*>([\s\S]*?)</c>', row)
    if not match:
        return None
    inner = match.group(1)
    inline = re.search(r"<t(?: [^>]*)?>([\s\S]*)</t>", inner)
    if inline:
        return html.unescape(inline.group(1))
    value = re.search(r"<v(?: [^>]*)?>([\s\S]*)</v>", inner)
    return html.unescape(value.group(1)) if value else None


def xml_text(value):
    return html.escape(value, quote=False)


def cell_xml(column, number, value, long_text=False):
    style = "16" if long_text or column in ("B", "N", "O", "DV") else "15"
    space = ' xml:space="preserve"' if "\n" in value else ""
    return f'<c r="{column}{number}" s="{style}" t="str"><v{space}>{xml_text(value)}</v></c>'


def replace_sentence(xml, code, column, old, new):
    located = None
    for row in rows_of(xml):
        number = row_number(row)
        if cell_text(row, "B", number) == code:
            located = (row, number)
            break
    if not located:
        fail(f"{code} missing")
    row, number = located
    current = cell_text(row, column, number)
    if current.count(old) != 1:
        fail(f"{code} sentence count is {current.count(old)}")
    updated = current.replace(old, new, 1)
    escaped = xml_text(updated)
    cell = re.search(rf'<c r="{column}{number}"[^>]*>[\s\S]*?</c>', row).group(0)
    replaced, count = re.subn(
        r"(<(?:[\w]+:)?v\b[^>]*>)[\s\S]*(</(?:[\w]+:)?v>)",
        lambda match: match.group(1) + escaped + match.group(2),
        cell,
        count=1,
    )
    if count != 1:
        fail(f"{code} cell was not replaced")
    return xml.replace(row, row.replace(cell, replaced, 1), 1), updated


live_bytes = live.read_bytes()
live_stat = live.stat()
baseline = sha256(live_bytes)
with zipfile.ZipFile(live) as archive:
    order = archive.infolist()
    parts = {info.filename: archive.read(info.filename) for info in order}
sheet2 = parts["xl/worksheets/sheet2.xml"].decode("utf-8")
found = []
for title, name in (
    ("已完成", "xl/worksheets/sheet1.xml"),
    ("待制作", "xl/worksheets/sheet2.xml"),
    ("制作中", "xl/worksheets/sheet3.xml"),
    ("待验收", "xl/worksheets/sheet4.xml"),
):
    for row in rows_of(parts[name].decode("utf-8")):
        number = row_number(row)
        code = cell_text(row, "B", number)
        label = cell_text(row, "A", number)
        if code in ("fire_flame_flow", "fire_flame_trail") or label in ("焰流", "留焰"):
            found.append((title, number, label, code))
if len(found) != 1 or found[0][0] != "待制作" or found[0][3] != "fire_flame_flow":
    fail(f"Identity conflict: {found}")
number = found[0][1]
source = next(row for row in rows_of(sheet2) if row_number(row) == number and cell_text(row, "B", number) == "fire_flame_flow")
opener = re.match(r"<row\b[^>]*>", source).group(0)
values = [
    ("A", "留焰", True),
    ("B", "fire_flame_trail", True),
    ("C", "通用", False),
    ("D", "火", False),
    ("J", "是", False),
    ("M", "否", False),
    ("N", feature, True),
    ("O", visual, True),
    ("P", "规则", False),
    ("Q", "持续", False),
    ("AB", "否", False),
    ("DV", note, True),
    ("DW", "否", False),
]
values.sort(key=lambda item: col_index(item[0]))
rebuilt = opener + "".join(cell_xml(column, number, value, long_text) for column, value, long_text in values) + "</row>"
sheet2 = sheet2.replace(source, rebuilt, 1)
sheet2, upwell_text = replace_sentence(sheet2, "fire_upwell", "N", upwell_old, upwell_new)
sheet2, continue_text = replace_sentence(sheet2, "fire_hit_continue", "N", continue_old, continue_new)
changed = {"xl/worksheets/sheet2.xml": sheet2.encode("utf-8")}
with zipfile.ZipFile(live) as src, zipfile.ZipFile(output, "w") as dst:
    for info in order:
        dst.writestr(info, changed.get(info.filename, src.read(info.filename)))
with zipfile.ZipFile(live) as src, zipfile.ZipFile(output) as dst:
    different = [name for name in src.namelist() if src.read(name) != dst.read(name)]
    if different != ["xl/worksheets/sheet2.xml"]:
        fail(f"Unexpected parts: {different}")
    pending = dst.read("xl/worksheets/sheet2.xml").decode("utf-8")
    ids = []
    for name in (
        "xl/worksheets/sheet1.xml",
        "xl/worksheets/sheet2.xml",
        "xl/worksheets/sheet3.xml",
        "xl/worksheets/sheet4.xml",
    ):
        for row in rows_of(dst.read(name).decode("utf-8")):
            number = row_number(row)
            code = cell_text(row, "B", number)
            if number >= 3 and code:
                ids.append(code)
    if len(ids) != 59 or len(set(ids)) != 59 or ids.count("fire_flame_trail") != 1:
        fail("Status ids changed beyond the replacement")
    if "fire_flame_flow" in pending:
        fail("Old id remains on 待制作")
    row = [item for item in rows_of(pending) if cell_text(item, "B", row_number(item)) == "fire_flame_trail"][0]
    number = row_number(row)
    for column in ("E", "R", "S", "T", "U", "AH", "AK"):
        if cell_text(row, column, number) is not None:
            fail(f"Old field {column} remains")
    if cell_text(row, "J", number) != "是" or cell_text(row, "P", number) != "规则":
        fail("Enchant classification missing")
    if cell_text(row, "DW", number) != "否":
        fail("Resource flag missing")
    if upwell_old in pending or continue_old in pending:
        fail("Old dependency sentence remains")
summary = {
    "submitted": False,
    "baselineSha256": baseline,
    "baselineMtime": datetime.fromtimestamp(live_stat.st_mtime).astimezone().isoformat(timespec="milliseconds"),
    "baselineBytes": len(live_bytes),
    "candidate": str(output),
    "candidateSha256": sha256(output.read_bytes()),
    "row": number,
    "id": "fire_flame_trail",
}
(batch / "留焰覆盖-summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"row": number, "candidateSha256": summary["candidateSha256"], "baseline": baseline}, ensure_ascii=False))

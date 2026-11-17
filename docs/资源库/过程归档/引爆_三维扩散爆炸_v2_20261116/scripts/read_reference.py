from pathlib import Path
import zipfile
import hashlib
import json
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
CACHE = Path.home() / ".gradle/caches/forge_gradle"
for name in ["source", "textures", "frames/orange", "frames/soul", "previews", "previews/main", "previews/side", "previews/strength", "blockbench", "evidence", "reference"]:
    (ROOT / name).mkdir(parents=True, exist_ok=True)
archive = CACHE / "bundeled_repo/net/minecraft/client/1.20.1/client-1.20.1-extra.jar"
records = []
with zipfile.ZipFile(archive) as bundle:
    names = sorted(name for name in bundle.namelist() if name.startswith("assets/minecraft/textures/particle/explosion_") or name == "assets/minecraft/particles/explosion.json")
    for name in names:
        data = bundle.read(name)
        (ROOT / "reference" / Path(name).name).write_bytes(data)
        records.append({"entry": name, "sha256": hashlib.sha256(data).hexdigest()})
sources = next(CACHE.glob("minecraft_user_repo/net/minecraftforge/forge/*_mapped_parchment*/*-sources.jar"))
with zipfile.ZipFile(sources) as bundle:
    for name in ["net/minecraft/client/particle/HugeExplosionParticle.java", "net/minecraft/client/particle/HugeExplosionSeedParticle.java"]:
        if name in bundle.namelist():
            data = bundle.read(name)
            (ROOT / "reference" / Path(name).name.replace(".java", ".java.txt")).write_bytes(data)
            records.append({"entry": name, "sha256": hashlib.sha256(data).hexdigest()})
board = Image.new("RGB", (800, 240), "#25303b")
draw = ImageDraw.Draw(board)
for index, path in enumerate(sorted((ROOT / "reference").glob("explosion_*.png"), key=lambda item: int(item.stem.split("_")[-1]))):
    frame = Image.open(path).convert("RGBA")
    board.paste(frame.resize((88, 88), Image.Resampling.NEAREST), ((index % 8) * 100 + 6, (index // 8) * 120 + 20), frame.resize((88, 88), Image.Resampling.NEAREST))
    draw.text(((index % 8) * 100 + 6, (index // 8) * 120 + 4), path.stem, fill="white")
board.save(ROOT / "reference/vanilla_explosion_board.png")
(ROOT / "evidence/local-reference.json").write_text(json.dumps({"archive": str(archive), "sources": str(sources), "entries": records, "purpose": "read-only local Minecraft 1.20.1 visual reference; not runtime candidate assets"}, ensure_ascii=False, indent=2), encoding="utf-8")
snapshot = Path("D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/detonate-v2-input-snapshot-20261116.json")
(ROOT / "evidence/input-snapshot-used.json").write_bytes(snapshot.read_bytes())
print(json.dumps({"files": len(records), "source_archive": str(sources)}, ensure_ascii=False))

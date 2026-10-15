# Handoff: Phantom sword orientation & embed (2026-10-14)

For terminal Grok / next session. Also read root `AGENTS.md` and `幻剑术.txt`.

## Problem (in-game)

1. Hover/fly trajectory matched look, but the sword model looked slanted (wrong render transform).
2. When planted in ground/wall, tip did not embed into the block; angle with the face did not feel like a stuck sword (wanted tip in, half blade+handle out, face angle 45–90°).

## Files changed

1. `src/main/java/com/bslx/XueXiMod/client/SwordEntityRenderer.java`
   - FIXED item tip defaults to **+Y**.
   - New pose: `YP(-yRot)` → `XP(xRot)` → `XP(-90)` so tip aligns with entity facing.
   - Removed old `YP(yRot-90)` + `ZP(xRot)` + `XP(90)` (Z-axis pitch caused the diagonal look).
   - Lerp `yRotO`/`xRotO` with partialTicks.

2. `src/main/java/com/bslx/XueXiMod/shu\technique\sword\SwordEntity.java`
   - `plant(BlockHitResult)`: tip direction into the block (`face.opposite` normal); position uses tipEmbed=0.5 / tipFromCenter=0.5 so center sits on the hit surface.
   - New `plantTipDirection(into, incoming)`: keep inbound tilt; if angle with face normal >45°, re-blend to exactly 45° with the face (dot(into)>=cos45).
   - Real and phantom swords share this path (same entity).
   - `aim()` unchanged (standard entity look-at). Hover still retargets look each tick.

`PhantomSwordEffect.java` untouched (spawn/hover logic already sets aim via look).

## Orientation formulas

- **Entity facing** (`aim(dir)`):  
  `yRot = atan2(dz, dx)*180/π - 90`, `xRot = -atan2(dy, horiz)*180/π`
- **Render** (tip +Y → entity forward):  
  `R = YP(-yRot) * XP(xRot) * XP(-90)`
- **Plant tip**: `into = opposite(hitFace).normal`; clamp so `|angle(tip, into)| ≤ 45°` ⇒ face angle ∈ [45°, 90°].
- **Plant position**: `pos = hitLoc + tipDir * (tipEmbed - tipFromCenter)` with both = 0.5 → center on surface, tip ~0.5 into block.

## Build

- `gradlew.bat compileJava --offline --no-daemon` → **BUILD SUCCESSFUL**
- Only existing `ResourceLocation` deprecation warnings.
- **Not tested in-game.**

## Known limits / follow-ups

- Item model origin may not be exact geometric mid-blade; if embed depth looks off, tune `tipFromCenter` / `tipEmbed` (e.g. 0.45–0.6).
- No extra roll around the tip axis; if the flat of the blade looks edge-on, add a fixed ZP/YP roll after the tip align.
- CustomModelData / non-vanilla sword models may need different tip axis assumptions.
- Hover “blade horizontal” is automatic when look pitch≈0 after the render fix; steep look still tips the sword toward the crosshair (by design).

## Suggested next checks in-game

1. Cast phantom sword: hover tip toward crosshair; fly tip along path (not slanted).
2. Hit floor / each wall / ceiling: tip buried, ~half out, angle 45–90° with face.
3. Drop real sword into a wall: same plant pose as phantom.
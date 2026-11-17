import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const batch = "D:/alianximod/docs/开发记录/术式批量制作/火系六式-20261029";
const summary = JSON.parse(await fs.readFile(`${batch}/liaoli-pose-return-summary.json`, "utf8"));
const intake = JSON.parse(await fs.readFile(`${batch}/liaoli-pose-intake.json`, "utf8"));
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(summary.candidate));
const making = workbook.worksheets.getItem("制作中");
const pending = workbook.worksheets.getItem("待验收");
if (making.getRange("B6").values[0][0] !== "fire_liaoli") throw new Error("燎里不在制作中第6行");
if (making.getRange("N6").values[0][0] !== intake.restored.n) throw new Error("N mismatch");
if (making.getRange("O6").values[0][0] !== intake.restored.o) throw new Error("O mismatch");
if (!String(making.getRange("DV6").values[0][0]).startsWith(intake.currentDisplay.dv)) throw new Error("DV prefix lost");
if (making.getRange("E6").values[0][0] !== 6) throw new Error("Damage changed");
for (const [row, code] of [[3, "fire_heat_up"], [4, "fire_burn"], [5, "fire_cool"]]) {
  if (making.getRange(`B${row}`).values[0][0] !== code) throw new Error(`${code} moved`);
}
if (pending.getRange("B3").values[0][0] !== "fire_fentian") throw new Error("焚天 moved wrong");
if (pending.getRange("B4").values[0][0] !== "fire_liaoyuan") throw new Error("燎原 moved wrong");
if (pending.getRange("B5").values[0][0]) throw new Error("待验收第5行仍有编号");
workbook.recalculate();
const preview = await workbook.render({ sheetName: "制作中", range: "A1:D6", scale: 1, format: "png" });
const previewBytes = new Uint8Array(await preview.arrayBuffer());
await fs.writeFile(`${batch}/liaoli-pose-return-preview.png`, previewBytes);
const artifact = await SpreadsheetFile.exportXlsx(workbook);
await artifact.save(`${batch}/liaoli-pose-return-candidate-artifact.xlsx`);
console.log(JSON.stringify({
  previewBytes: previewBytes.length,
  b6: making.getRange("B6").values[0][0],
  e6: making.getRange("E6").values[0][0],
  pending3: pending.getRange("B3").values[0][0],
  pending4: pending.getRange("B4").values[0][0],
}));

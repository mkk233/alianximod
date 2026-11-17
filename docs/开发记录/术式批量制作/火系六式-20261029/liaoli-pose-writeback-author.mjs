import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const batch = "D:/alianximod/docs/开发记录/术式批量制作/火系六式-20261029";
const summary = JSON.parse(await fs.readFile(`${batch}/liaoli-pose-writeback-summary.json`, "utf8"));
const snapshot = JSON.parse(await fs.readFile(`${batch}/liaoli-pose-transition-snapshot.json`, "utf8"));
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(summary.candidate));
const pending = workbook.worksheets.getItem("待验收");
const making = workbook.worksheets.getItem("制作中");
if (pending.getRange("B5").values[0][0] !== "fire_liaoli") throw new Error("待验收第5行不是燎里");
if (pending.getRange("N5").values[0][0] !== snapshot.conversion.nAfter) throw new Error("N mismatch");
if (pending.getRange("O5").values[0][0] !== snapshot.conversion.oAfter) throw new Error("O mismatch");
if (!String(pending.getRange("DV5").values[0][0]).startsWith(snapshot.currentDetailed.dv)) throw new Error("DV prefix lost");
if (!String(pending.getRange("DV5").values[0][0]).includes("本阶段额度已用完，返修结果未追加复审")) throw new Error("Quota note missing");
for (const [row, code] of [[3, "fire_heat_up"], [4, "fire_burn"], [5, "fire_cool"]]) {
  if (making.getRange(`B${row}`).values[0][0] !== code) throw new Error(`${code} moved`);
}
if (making.getRange("B6").values[0][0]) throw new Error("制作中第6行仍有编号");
if (pending.getRange("B3").values[0][0] !== "fire_fentian") throw new Error("焚天 moved");
if (pending.getRange("B4").values[0][0] !== "fire_liaoyuan") throw new Error("燎原 moved");
workbook.recalculate();
const preview = await workbook.render({ sheetName: "待验收", range: "A1:D5", scale: 1, format: "png" });
const previewBytes = new Uint8Array(await preview.arrayBuffer());
await fs.writeFile(`${batch}/liaoli-pose-writeback-preview.png`, previewBytes);
const artifact = await SpreadsheetFile.exportXlsx(workbook);
await artifact.save(`${batch}/liaoli-pose-writeback-candidate-artifact.xlsx`);
console.log(JSON.stringify({
  previewBytes: previewBytes.length,
  b5: pending.getRange("B5").values[0][0],
  n5: pending.getRange("N5").values[0][0],
}));

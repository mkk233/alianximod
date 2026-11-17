import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const batch = "D:/alianximod/docs/开发记录/术式批量制作/火系六式-20261029";
const summary = JSON.parse(await fs.readFile(`${batch}/liaoli-hit-seed-summary.json`, "utf8"));
const intake = JSON.parse(await fs.readFile(`${batch}/liaoli-hit-seed-intake.json`, "utf8"));
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(summary.candidate));
const making = workbook.worksheets.getItem("制作中");
const pending = workbook.worksheets.getItem("待验收");
if (making.getRange("B6").values[0][0] !== "fire_liaoli") throw new Error("制作中第6行不是燎里");
const n = String(making.getRange("N6").values[0][0]);
const o = String(making.getRange("O6").values[0][0]);
const dv = String(making.getRange("DV6").values[0][0]);
if (n !== intake.restored.n || !n.includes("严格大于50") || !n.includes("0.36×S×(1+F)")) throw new Error("N mismatch");
if (o !== intake.restored.o || !o.includes("预览未确认")) throw new Error("O mismatch");
if (!dv.startsWith(intake.currentDisplay.dv) || !dv.includes("本阶段额度已用完，返修结果未追加复审")) throw new Error("DV mismatch");
if (String(making.getRange("E6").values[0][0]) !== "6") throw new Error("damage changed");
if (making.getRange("EC6").values[0][0] !== "全部") throw new Error("resource amount changed");
for (const [row, code] of [[3, "fire_heat_up"], [4, "fire_burn"], [5, "fire_cool"]]) {
  if (making.getRange(`B${row}`).values[0][0] !== code) throw new Error(`${code} moved`);
}
if (pending.getRange("B3").values[0][0] !== "fire_fentian") throw new Error("焚天 moved");
if (pending.getRange("B4").values[0][0] !== "fire_liaoyuan") throw new Error("燎原 moved");
if (pending.getRange("B5").values[0][0]) throw new Error("待验收第5行仍有编号");
workbook.recalculate();
const preview = await workbook.render({ sheetName: "制作中", range: "A1:D6", scale: 1, format: "png" });
const previewBytes = new Uint8Array(await preview.arrayBuffer());
await fs.writeFile(`${batch}/liaoli-hit-seed-preview.png`, previewBytes);
const artifact = await SpreadsheetFile.exportXlsx(workbook);
await artifact.save(`${batch}/liaoli-hit-seed-candidate-artifact.xlsx`);
console.log(JSON.stringify({ previewBytes: previewBytes.length, b6: making.getRange("B6").values[0][0] }));

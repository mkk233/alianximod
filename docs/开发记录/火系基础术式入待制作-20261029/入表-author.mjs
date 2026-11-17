import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const batch = "D:/alianximod/docs/开发记录/火系基础术式入待制作-20261029";
const summary = JSON.parse(await fs.readFile(`${batch}/入表-summary.json`, "utf8"));
const specs = JSON.parse(await fs.readFile(`${batch}/入表-rows.json`, "utf8"));
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(summary.backup));
const sheet = workbook.worksheets.getItem("待制作");
const textColumns = ["C", "D", "J", "M", "P", "Q", "R", "S", "AB", "AH", "AK", "DW", "DX", "DZ", "EA", "EB", "EC", "EI", "EJ", "EK", "EL", "EM"];
const numberColumns = ["E", "U", "AS", "BV", "BW", "EG"];

specs.forEach((spec, index) => {
  const row = index + 3;
  const values = { A: spec.name, B: spec.id, N: spec.n, O: spec.o, DV: spec.dv };
  if (spec.ej) values.EJ = spec.ej;
  if (spec.ek) values.EK = spec.ek;
  for (const column of textColumns) {
    if (spec[column] !== undefined) values[column] = spec[column];
  }
  for (const column of numberColumns) {
    if (spec[column] !== undefined) values[column] = spec[column];
  }
  for (const [column, value] of Object.entries(values)) {
    sheet.getRange(`${column}${row}`).values = [[value]];
  }
});

workbook.recalculate();
if (sheet.getRange("B3").values[0][0] !== "fire_flame_flow") throw new Error("焰流 id missing");
if (sheet.getRange("E3").values[0][0] !== 2) throw new Error("焰流 damage missing");
if (sheet.getRange("E5").values[0][0] !== 24) throw new Error("陨岩 damage missing");
if (sheet.getRange("EG12").values[0][0] !== 10) throw new Error("引燃 period missing");
if (sheet.getRange("EC13").values[0][0] !== "全部") throw new Error("引爆 amount missing");
if (sheet.getRange("EC14").values[0][0]) throw new Error("幽冥 amount was filled");
if (sheet.getRange("B15").values[0][0] !== "fire_break_ground") throw new Error("崩地 id missing");

const preview = await workbook.render({ sheetName: "待制作", range: "A1:D15", scale: 1, format: "png" });
const previewBytes = new Uint8Array(await preview.arrayBuffer());
await fs.writeFile(`${batch}/入表-preview.png`, previewBytes);
const artifact = await SpreadsheetFile.exportXlsx(workbook);
const artifactPath = `${batch}/入表-candidate-artifact.xlsx`;
await artifact.save(artifactPath);
console.log(JSON.stringify({
  artifact: artifactPath,
  previewBytes: previewBytes.length,
  b3: sheet.getRange("B3").values[0][0],
  e3: sheet.getRange("E3").values[0][0],
  eg12: sheet.getRange("EG12").values[0][0],
  ec13: sheet.getRange("EC13").values[0][0],
  b15: sheet.getRange("B15").values[0][0],
}));

import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const batch = "D:/alianximod/docs/开发记录/术式批量制作/灼地-20261030";
const snapshot = JSON.parse(await fs.readFile(`${batch}/shape-snapshot.json`, "utf8"));
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(snapshot.candidate));
const making = workbook.worksheets.getItem("制作中");
const archive = workbook.worksheets.getItem("待制作原始数据");
if (making.getRange("A6").values[0][0] !== "灼地" || making.getRange("B6").values[0][0] !== "fire_scorch_field") {
  throw new Error("第6行不是灼地");
}
if (Number(making.getRange("G6").values[0][0]) !== 20 || Number(making.getRange("H6").values[0][0]) !== 0) {
  throw new Error("等级或复杂度变了");
}
if (Number(making.getRange("E6").values[0][0]) !== 1 || Number(making.getRange("F6").values[0][0]) !== 30) {
  throw new Error("伤害或冷却变了");
}
if (Number(making.getRange("AS6").values[0][0]) !== 2 || Number(making.getRange("BW6").values[0][0]) !== 100) {
  throw new Error("半径或寿命变了");
}
const dv = String(making.getRange("DV6").values[0][0]);
if (dv !== snapshot.dvAfter || !dv.startsWith(snapshot.dvBefore) || !dv.endsWith(snapshot.feedback)) {
  throw new Error("DV 没有完整保留旧文");
}
const archiveDv = String(archive.getRange("DV9").values[0][0]);
if (archiveDv.includes("方形接触体") || archive.getRange("G9").values[0][0] || archive.getRange("H9").values[0][0]) {
  throw new Error("档案被改");
}
if (making.getRange("B3").values[0][0] !== "fire_heat_up" || making.getRange("B5").values[0][0] !== "fire_cool") {
  throw new Error("相邻行变了");
}
workbook.recalculate();
const preview = await workbook.render({ sheetName: "制作中", range: "A6:H6", scale: 1, format: "png" });
await fs.writeFile(`${batch}/shape-preview.png`, new Uint8Array(await preview.arrayBuffer()));
const artifact = await SpreadsheetFile.exportXlsx(workbook);
await artifact.save(`${batch}/候选/术式制作表-shape-artifact.xlsx`);
console.log(JSON.stringify({ name: "灼地", levelCap: 20, complexity: 0, feedback: true }));

import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const batch = "D:/alianximod/docs/开发记录/术式批量制作/灼地-20261030";
const snapshot = JSON.parse(await fs.readFile(`${batch}/params-snapshot.json`, "utf8"));
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(snapshot.candidate));
const making = workbook.worksheets.getItem("制作中");
const archive = workbook.worksheets.getItem("待制作原始数据");
if (making.getRange("G2").values[0][0] !== "等级上限" || making.getRange("H2").values[0][0] !== "复杂度") {
  throw new Error("表头不对");
}
if (making.getRange("B6").values[0][0] !== "fire_scorch_field" || making.getRange("A6").values[0][0] !== "灼地") {
  throw new Error("第6行不是灼地");
}
if (Number(making.getRange("G6").values[0][0]) !== 20 || Number(making.getRange("H6").values[0][0]) !== 0) {
  throw new Error("等级上限或复杂度不对");
}
if (making.getRange("K6").values[0][0] || making.getRange("L6").values[0][0]) throw new Error("未定费用被填上");
if (Number(making.getRange("E6").values[0][0]) !== 1 || Number(making.getRange("F6").values[0][0]) !== 30) {
  throw new Error("伤害或冷却变了");
}
const dv = String(making.getRange("DV6").values[0][0]);
if (!dv.startsWith(snapshot.dvBefore) || !dv.endsWith(snapshot.feedback)) throw new Error("DV 没有完整保留原文");
if (archive.getRange("B9").values[0][0] !== "fire_scorch_field") throw new Error("档案行丢了");
if (archive.getRange("G9").values[0][0] || archive.getRange("H9").values[0][0]) throw new Error("档案被填了参数");
const archiveDv = String(archive.getRange("DV9").values[0][0]);
if (archiveDv.includes("[设计反馈") || archiveDv.includes("暂用复杂度")) throw new Error("档案写入了反馈");
if (making.getRange("B3").values[0][0] !== "fire_heat_up" || making.getRange("B5").values[0][0] !== "fire_cool") {
  throw new Error("相邻行变了");
}
workbook.recalculate();
const preview = await workbook.render({ sheetName: "制作中", range: "A6:H6", scale: 1, format: "png" });
await fs.writeFile(`${batch}/params-preview.png`, new Uint8Array(await preview.arrayBuffer()));
const artifact = await SpreadsheetFile.exportXlsx(workbook);
await artifact.save(`${batch}/候选/术式制作表-params-artifact.xlsx`);
console.log(JSON.stringify({ levelCap: 20, complexity: 0, name: making.getRange("A6").values[0][0] }));

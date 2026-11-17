import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const batch = "D:/alianximod/docs/开发记录/灼地产种设计入表-20261030";
const summary = JSON.parse(await fs.readFile(`${batch}/热波-summary.json`, "utf8"));
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(summary.candidate));
const pending = workbook.worksheets.getItem("待制作");
if (pending.getRange("A6").values[0][0] !== "热波" || pending.getRange("B6").values[0][0] !== "fire_shock_ring") {
  throw new Error("热波名称或编号不对");
}
if (pending.getRange("D6").values[0][0] !== "火" || Number(pending.getRange("E6").values[0][0]) !== 4 || Number(pending.getRange("F6").values[0][0]) !== 16) {
  throw new Error("热波属性、伤害或冷却被改");
}
if (pending.getRange("A7").values[0][0] !== "地表定位" || pending.getRange("D7").values[0][0] !== "无" || pending.getRange("C7").values[0][0] !== "通用") {
  throw new Error("地表定位属性不对");
}
for (const [row, code] of [[5, "fire_meteorite"], [10, "fire_hit_continue"], [12, "fire_kindle"], [13, "fire_detonate"]]) {
  const text = String(pending.getRange(`N${row}`).values[0][0]);
  if (pending.getRange(`B${row}`).values[0][0] !== code || text.includes("震环") || !text.includes("热波")) {
    throw new Error(`${code} 的名称引用未改完`);
  }
}
const note = String(pending.getRange("DV6").values[0][0]);
if (note.includes("震环") || !note.includes("热波")) throw new Error("热波备注未改名");
workbook.recalculate();
const preview = await workbook.render({ sheetName: "待制作", range: "A5:F7", scale: 1, format: "png" });
await fs.writeFile(`${batch}/热波-preview.png`, new Uint8Array(await preview.arrayBuffer()));
const artifact = await SpreadsheetFile.exportXlsx(workbook);
await artifact.save(`${batch}/热波-candidate-artifact.xlsx`);
console.log(JSON.stringify({ name: pending.getRange("A6").values[0][0], attribute: pending.getRange("D7").values[0][0] }));

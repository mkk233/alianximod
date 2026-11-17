import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const batch = "D:/alianximod/docs/开发记录/灼地产种设计入表-20261030";
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(`${batch}/火苗-candidate-preserved.xlsx`));
const pending = workbook.worksheets.getItem("待制作");
if (pending.getRange("A16").values[0][0] !== "火苗" || pending.getRange("B16").values[0][0] !== "fire_spark") {
  throw new Error("第16行不是火苗");
}
if (Number(pending.getRange("E16").values[0][0]) !== 1 || Number(pending.getRange("F16").values[0][0]) !== 5) {
  throw new Error("伤害或冷却不对");
}
if (Number(pending.getRange("EC16").values[0][0]) !== 2 || pending.getRange("EB16").values[0][0] !== "命中成功") {
  throw new Error("产种配置不对");
}
const note = String(pending.getRange("DV16").values[0][0]);
if (!note.includes("原版燃烧时长待定，建议20tick") || note.includes("时长确认为20")) throw new Error("时长备注不对");
if (pending.getRange("L16").values[0][0] || pending.getRange("U16").values[0][0]) throw new Error("未定数被填上");
if (pending.getRange("B11").values[0][0] !== "fire_seed_sow" || Number(pending.getRange("F11").values[0][0]) !== 5) {
  throw new Error("种火被改");
}
if (pending.getRange("B15").values[0][0] !== "earth_break_ground") throw new Error("崩地被改");
if (pending.getRange("A6").values[0][0] !== "热波") throw new Error("热波被改");
let seen = 0;
for (let row = 3; row <= 16; row += 1) {
  if (pending.getRange(`B${row}`).values[0][0] === "fire_spark") seen += 1;
}
if (seen !== 1) throw new Error("火苗编号重复");
workbook.recalculate();
const preview = await workbook.render({ sheetName: "待制作", range: "A15:F16", scale: 1, format: "png" });
await fs.writeFile(`${batch}/火苗-preview.png`, new Uint8Array(await preview.arrayBuffer()));
const artifact = await SpreadsheetFile.exportXlsx(workbook);
await artifact.save(`${batch}/火苗-candidate-artifact.xlsx`);
console.log(JSON.stringify({ row: 16, damage: 1, cooling: 5, seeds: 2 }));

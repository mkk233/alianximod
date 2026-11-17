import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const batch = "D:/alianximod/docs/开发记录/灼地产种设计入表-20261030";
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(`${batch}/编号-candidate-preserved.xlsx`));
const pending = workbook.worksheets.getItem("待制作");
const expect = [
  [7, "地表定位", "rule_surface_anchor", "无"],
  [9, "错时", "rule_stagger", "无"],
  [10, "命中接续", "rule_hit_continue", "无"],
  [15, "崩地", "earth_break_ground", "土"],
];
for (const [row, name, code, element] of expect) {
  if (pending.getRange(`A${row}`).values[0][0] !== name) throw new Error(`${name} 不在第${row}行`);
  if (pending.getRange(`B${row}`).values[0][0] !== code) throw new Error(`${name} 编号不是${code}`);
  if (pending.getRange(`D${row}`).values[0][0] !== element) throw new Error(`${name} 属性变了`);
  if (String(pending.getRange(`B${row}`).values[0][0]).startsWith("fire_")) throw new Error(`${name} 仍是火前缀`);
}
if (pending.getRange("B6").values[0][0] !== "fire_shock_ring" || pending.getRange("A6").values[0][0] !== "热波") {
  throw new Error("热波编号被改");
}
if (pending.getRange("B8").values[0][0] !== "fire_upwell") throw new Error("喷涌编号被改");
workbook.recalculate();
const preview = await workbook.render({ sheetName: "待制作", range: "A7:D15", scale: 1, format: "png" });
await fs.writeFile(`${batch}/编号-preview.png`, new Uint8Array(await preview.arrayBuffer()));
const artifact = await SpreadsheetFile.exportXlsx(workbook);
await artifact.save(`${batch}/编号-candidate-artifact.xlsx`);
console.log(JSON.stringify({ ids: expect.map((item) => item[2]) }));

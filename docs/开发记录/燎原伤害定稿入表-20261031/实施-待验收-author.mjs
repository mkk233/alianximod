import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const batch = "D:/alianximod/docs/开发记录/燎原伤害定稿入表-20261031";
const summary = JSON.parse(await fs.readFile(`${batch}/实施-待验收-summary.json`, "utf8"));
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(summary.candidate));
const making = workbook.worksheets.getItem("制作中");
const pending = workbook.worksheets.getItem("待验收");
const row = summary.targetRow;
if (pending.getRange(`B${row}`).values[0][0] !== "fire_liaoyuan") throw new Error("待验收目标行不是燎原");
if (making.getRange("B3").values[0][0] !== "fire_heat_up" || making.getRange("B5").values[0][0] !== "fire_cool") {
  throw new Error("炉心行被挪动");
}
if (making.getRange("B6").values[0][0]) throw new Error("制作中第6行还在");
const text = String(pending.getRange(`N${row}`).values[0][0]);
if (!text.startsWith("铺开半径10格的贴地火场")) throw new Error("游戏内文案不对");
const visual = String(pending.getRange(`O${row}`).values[0][0]);
for (const token of ["liaoyuan_v3/ground_ash.png", "ground_ash_d.png", "liaoyuan_v2/spread_wave.png", "liaoyuan_v2/ground_flame.png"]) {
  if (!visual.includes(token)) throw new Error(`资源缺失 ${token}`);
}
if (Number(pending.getRange(`E${row}`).values[0][0]) !== 8 || Number(pending.getRange(`AS${row}`).values[0][0]) !== 10) {
  throw new Error("伤害或半径变了");
}
if (Number(pending.getRange(`EH${row}`).values[0][0]) !== 0 || Number(pending.getRange(`EG${row}`).values[0][0]) !== 20) {
  throw new Error("燃烧时序变了");
}
if (Number(pending.getRange(`EC${row}`).values[0][0]) !== 10 || Number(pending.getRange(`BW${row}`).values[0][0]) !== 200) {
  throw new Error("燃料或寿命变了");
}
const note = String(pending.getRange(`DV${row}`).values[0][0]);
if (!note.includes("本阶段额度已用完，返修结果未追加复审") || !note.includes("BUILD SUCCESSFUL in 25s")) {
  throw new Error("编译或额度备注缺失");
}
const added = note.slice(note.lastIndexOf("[设计反馈 2026-10-31] 本次返修已按批准版写入"));
if (!added.includes("用户游戏实测仍待做") || added.includes("游戏实测已通过")) throw new Error("不能写成实测通过");
workbook.recalculate();
const preview = await workbook.render({ sheetName: "待验收", range: "A3:B6", scale: 1, format: "png" });
await fs.writeFile(`${batch}/实施-待验收-preview.png`, new Uint8Array(await preview.arrayBuffer()));
const artifact = await SpreadsheetFile.exportXlsx(workbook);
await artifact.save(`${batch}/实施-待验收-candidate-artifact.xlsx`);
console.log(JSON.stringify({ pendingRow: row }));

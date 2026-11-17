import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const batch = "D:/alianximod/docs/开发记录/燎原伤害定稿入表-20261031";
const summary = JSON.parse(await fs.readFile(`${batch}/实施-退回-summary.json`, "utf8"));
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(summary.candidate));
const making = workbook.worksheets.getItem("制作中");
const pending = workbook.worksheets.getItem("待验收");
if (making.getRange("B6").values[0][0] !== "fire_liaoyuan") throw new Error("制作中第6行不是燎原");
if (making.getRange("B3").values[0][0] !== "fire_heat_up" || making.getRange("B5").values[0][0] !== "fire_cool") {
  throw new Error("炉心行被挪动");
}
const text = String(making.getRange("N6").values[0][0]);
if (!text.includes("半径10") || !text.includes("基础火伤8点") || !text.includes("4×(1+0.1n+F)")) throw new Error("详细需求不完整");
if (!String(making.getRange("O6").values[0][0]).includes("灰烬")) throw new Error("视觉意图不是灰烬");
if (Number(making.getRange("E6").values[0][0]) !== 8 || Number(making.getRange("AS6").values[0][0]) !== 10) throw new Error("伤害或半径变了");
if (Number(making.getRange("EH6").values[0][0]) !== 0 || Number(making.getRange("EG6").values[0][0]) !== 20) throw new Error("燃烧时序变了");
if (pending.getRange("B3").values[0][0] !== "fire_fentian" || pending.getRange("B4").values[0][0] !== "fire_liaoli" || pending.getRange("B5").values[0][0] !== "fire_scorch_field") {
  throw new Error("待验收邻居错位");
}
if (pending.getRange("B6").values[0][0]) throw new Error("待验收第6行还在");
const note = String(making.getRange("DV6").values[0][0]);
if (!note.includes("按这个版本开始修") || !note.includes("不再是待决定项")) throw new Error("返修授权没有写入");
workbook.recalculate();
const preview = await workbook.render({ sheetName: "制作中", range: "A3:B6", scale: 1, format: "png" });
await fs.writeFile(`${batch}/实施-退回-preview.png`, new Uint8Array(await preview.arrayBuffer()));
const artifact = await SpreadsheetFile.exportXlsx(workbook);
await artifact.save(`${batch}/实施-退回-candidate-artifact.xlsx`);
console.log(JSON.stringify({ making: "B6", pendingCleared: true }));

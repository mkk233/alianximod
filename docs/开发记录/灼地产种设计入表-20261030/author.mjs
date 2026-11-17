import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const batch = "D:/alianximod/docs/开发记录/灼地产种设计入表-20261030";
const summary = JSON.parse(await fs.readFile(`${batch}/summary.json`, "utf8"));
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(summary.candidate));
const pending = workbook.worksheets.getItem("待制作");
const detail = workbook.worksheets.getItem("资源状态明细");
if (pending.getRange("B4").values[0][0] !== "fire_scorch_field") throw new Error("待制作第4行不是灼地");
if (pending.getRange("A4").values[0][0] !== "灼地") throw new Error("名称变了");
if (Number(pending.getRange("E4").values[0][0]) !== 1) throw new Error("基础伤害不是1");
if (Number(pending.getRange("AS4").values[0][0]) !== 2) throw new Error("半径不是2");
if (Number(pending.getRange("BW4").values[0][0]) !== 100) throw new Error("寿命不是100");
const period = pending.getRange("BV4").values[0][0];
if (period !== null && period !== "") throw new Error("场地维持周期还在");
const trigger = pending.getRange("EB4").values[0][0];
if (trigger !== null && trigger !== "") throw new Error("主行触发时机被填上");
if (Number(pending.getRange("EC4").values[0][0]) !== 20) throw new Error("首条不是20");
if (pending.getRange("DX4").values[0][0] !== "火种" || pending.getRange("DZ4").values[0][0] !== "增加") throw new Error("首条资源不对");
if (pending.getRange("EI4").values[0][0] !== "每目标每次施法") throw new Error("首条计量不对");
const text = String(pending.getRange("N4").values[0][0]);
if (!text.includes("基础20点火种") || !text.includes("基础15点火种") || text.includes("每20tick造成一次基础2点火伤")) {
  throw new Error("需求没有换成产种");
}
if (pending.getRange("B3").values[0][0] !== "fire_flame_trail") throw new Error("留焰被动过");
if (detail.getRange("A7").values[0][0] !== "fire_scorch_field") throw new Error("明细第7行不是灼地");
if (Number(detail.getRange("B7").values[0][0]) !== 2) throw new Error("明细序号不是2");
if (Number(detail.getRange("I7").values[0][0]) !== 15) throw new Error("周期产量不是15");
if (Number(detail.getRange("M7").values[0][0]) !== 20) throw new Error("产种周期不是20");
const detailTrigger = detail.getRange("H7").values[0][0];
if (detailTrigger !== null && detailTrigger !== "") throw new Error("明细触发时机被填上");
if (detail.getRange("A3").values[0][0] !== "water_mist") throw new Error("原明细被改");
let seen = 0;
for (let row = 3; row <= 16; row += 1) {
  if (pending.getRange(`B${row}`).values[0][0] === "fire_scorch_field") seen += 1;
}
if (seen !== 1) throw new Error(`待制作灼地出现${seen}次`);
workbook.recalculate();
const preview = await workbook.render({ sheetName: "资源状态明细", range: "A2:I7", scale: 1, format: "png" });
await fs.writeFile(`${batch}/preview.png`, new Uint8Array(await preview.arrayBuffer()));
const artifact = await SpreadsheetFile.exportXlsx(workbook);
await artifact.save(`${batch}/candidate-artifact.xlsx`);
console.log(JSON.stringify({ row: 4, detailRow: 7, damage: pending.getRange("E4").values[0][0] }));

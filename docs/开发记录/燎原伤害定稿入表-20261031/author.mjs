import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const batch = "D:/alianximod/docs/开发记录/燎原伤害定稿入表-20261031";
const summary = JSON.parse(await fs.readFile(`${batch}/summary.json`, "utf8"));
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(summary.candidate));
const pending = workbook.worksheets.getItem("待验收");
const row = summary.row;
if (pending.getRange(`B${row}`).values[0][0] !== "fire_liaoyuan") throw new Error("行号对不上燎原");
if (pending.getRange(`A${row}`).values[0][0] !== "燎原") throw new Error("名称变了");
if (Number(pending.getRange(`E${row}`).values[0][0]) !== 8) throw new Error("基础伤害不是8");
if (Number(pending.getRange(`BV${row}`).values[0][0]) !== 10) throw new Error("本体周期不是10");
if (Number(pending.getRange(`EG${row}`).values[0][0]) !== 20 || Number(pending.getRange(`EC${row}`).values[0][0]) !== 10) {
  throw new Error("火种周期被混填");
}
if (Number(pending.getRange(`BW${row}`).values[0][0]) !== 200) throw new Error("寿命变了");
const text = String(pending.getRange(`N${row}`).values[0][0]);
if (text !== "铺开地表火层。踩中后按段燃烧，每段消耗10点火种，燃料耗尽或到达期限就结束") throw new Error("展示句被改写");
const note = String(pending.getRange(`DV${row}`).values[0][0]);
if (!note.includes("2026-10-31已确认的新设计，待代码落实") || !note.includes("256+104＝360") || !note.includes("不是实测已通过")) {
  throw new Error("备注未写明待落实");
}
workbook.recalculate();
const preview = await workbook.render({ sheetName: "待验收", range: `A${row}:F${row}`, scale: 1, format: "png" });
await fs.writeFile(`${batch}/preview.png`, new Uint8Array(await preview.arrayBuffer()));
const artifact = await SpreadsheetFile.exportXlsx(workbook);
await artifact.save(`${batch}/candidate-artifact.xlsx`);
console.log(JSON.stringify({ row, damage: 8, bodyPeriod: 10, burnPeriod: 20 }));

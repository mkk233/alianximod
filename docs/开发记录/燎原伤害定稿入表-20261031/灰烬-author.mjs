import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const batch = "D:/alianximod/docs/开发记录/燎原伤害定稿入表-20261031";
const summary = JSON.parse(await fs.readFile(`${batch}/灰烬-summary.json`, "utf8"));
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(summary.candidate));
const pending = workbook.worksheets.getItem("待验收");
const row = summary.row;
if (pending.getRange(`B${row}`).values[0][0] !== "fire_liaoyuan") throw new Error("不是燎原");
if (Number(pending.getRange(`AS${row}`).values[0][0]) !== 10) throw new Error("半径不是10");
if (Number(pending.getRange(`E${row}`).values[0][0]) !== 8) throw new Error("本体伤害变了");
if (Number(pending.getRange(`BV${row}`).values[0][0]) !== 10) throw new Error("本体周期变了");
if (Number(pending.getRange(`EG${row}`).values[0][0]) !== 20 || Number(pending.getRange(`EC${row}`).values[0][0]) !== 10) {
  throw new Error("燃烧周期或燃料变了");
}
if (Number(pending.getRange(`EH${row}`).values[0][0]) !== 0) throw new Error("首段延迟不是0");
if (pending.getRange(`EB${row}`).values[0][0] !== "燃烧结算") throw new Error("扣款触发变了");
const note = String(pending.getRange(`DV${row}`).values[0][0]);
if (!note.includes("2026-10-31追加设计：半径10、灰烬覆盖、展开即伤害与首段燃烧，待美术与代码落实")) {
  throw new Error("追加标记缺失");
}
if (!note.includes("不能把360写成范围内每个目标必得")) throw new Error("360条件不完整");
const effect = String(pending.getRange(`O${row}`).values[0][0]);
if (!effect.includes("ground_flame.png") || effect.includes("burnt_grass")) throw new Error("特效路径被换成新素材");
workbook.recalculate();
const preview = await workbook.render({ sheetName: "待验收", range: `AS${row}:BW${row}`, scale: 1, format: "png" });
await fs.writeFile(`${batch}/灰烬-preview.png`, new Uint8Array(await preview.arrayBuffer()));
const artifact = await SpreadsheetFile.exportXlsx(workbook);
await artifact.save(`${batch}/灰烬-candidate-artifact.xlsx`);
console.log(JSON.stringify({ row, radius: 10, delay: 0, burnPeriod: 20 }));

import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const batch = "D:/alianximod/docs/开发记录/术式批量制作/灼地-20261030";
const snapshot = JSON.parse(await fs.readFile(`${batch}/claim-snapshot.json`, "utf8"));
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(snapshot.candidate));
const pending = workbook.worksheets.getItem("待制作");
const making = workbook.worksheets.getItem("制作中");
const archive = workbook.worksheets.getItem("待制作原始数据");
const detail = workbook.worksheets.getItem("资源状态明细");
const done = workbook.worksheets.getItem("已完成");
const accept = workbook.worksheets.getItem("待验收");

function columnValues(sheet, column, last) {
  const values = [];
  for (let row = 3; row <= last; row += 1) {
    const value = sheet.getRange(`${column}${row}`).values[0][0];
    if (value) values.push(String(value));
  }
  return values;
}

const makingIds = columnValues(making, "B", 6);
const pendingIds = columnValues(pending, "B", 15);
const doneIds = columnValues(done, "B", 50);
const acceptIds = columnValues(accept, "B", 10);
const archiveIds = columnValues(archive, "B", 9);
if (makingIds.join(",") !== snapshot.makingIds.join(",")) throw new Error(`制作中 ${makingIds}`);
if (pendingIds.join(",") !== snapshot.pendingIds.join(",")) throw new Error(`待制作 ${pendingIds}`);
if (archiveIds.join(",") !== snapshot.archiveIds.join(",")) throw new Error(`档案 ${archiveIds}`);
if (doneIds.length !== 40 || acceptIds.length !== 3) throw new Error("四页行数不对");
const all = [...doneIds, ...pendingIds, ...makingIds, ...acceptIds];
if (all.length !== 60 || new Set(all).size !== 60) throw new Error("编号不唯一");
if (pendingIds.includes("fire_scorch_field") || making.getRange("B6").values[0][0] !== "fire_scorch_field") {
  throw new Error("迁移动作不对");
}
if (making.getRange("A6").values[0][0] !== "灼地") throw new Error("名称不对");
if (making.getRange("N6").values[0][0] !== snapshot.n) throw new Error("制作中 N 变了");
if (making.getRange("O6").values[0][0] !== snapshot.o) throw new Error("制作中 O 变了");
if (making.getRange("DV6").values[0][0] !== snapshot.dvAfter) throw new Error("制作中 DV 变了");
if (archive.getRange("N9").values[0][0] !== snapshot.n) throw new Error("档案 N 变了");
if (archive.getRange("O9").values[0][0] !== snapshot.o) throw new Error("档案 O 变了");
if (archive.getRange("DV9").values[0][0] !== snapshot.dv) throw new Error("档案 DV 变了");
if (String(archive.getRange("DV9").values[0][0]).includes(snapshot.marker)) throw new Error("档案写入了流程标记");
if (archive.getRange("B9").values[0][0] !== "fire_scorch_field") throw new Error("档案编号不对");
if (Number(making.getRange("E6").values[0][0]) !== 1 || Number(making.getRange("F6").values[0][0]) !== 30) {
  throw new Error("伤害或冷却变了");
}
if (Number(making.getRange("AS6").values[0][0]) !== 2 || Number(making.getRange("BW6").values[0][0]) !== 100) {
  throw new Error("半径或寿命变了");
}
if (Number(making.getRange("EC6").values[0][0]) !== 20) throw new Error("首次产种变了");
if (making.getRange("R6").values[0][0]) throw new Error("弹道被填上");
if (making.getRange("B3").values[0][0] !== "fire_heat_up" || making.getRange("B5").values[0][0] !== "fire_cool") {
  throw new Error("制作中原行被改");
}
if (pending.getRange("B3").values[0][0] !== "fire_flame_trail" || pending.getRange("B15").values[0][0] !== "fire_spark") {
  throw new Error("待制作两端被改");
}
if (Number(pending.getRange("F15").values[0][0]) !== 5) throw new Error("火苗冷却被改");
if (detail.getRange("A7").values[0][0] !== "fire_scorch_field" || Number(detail.getRange("B7").values[0][0]) !== 2) {
  throw new Error("资源明细被改");
}
if (Number(detail.getRange("I7").values[0][0]) !== 15 || Number(detail.getRange("M7").values[0][0]) !== 20) {
  throw new Error("周期产种被改");
}
if (!String(archive.getRange("A10").values[0][0]).includes("使用说明")) throw new Error("档案说明被挪走");
workbook.recalculate();
const ranges = [
  ["制作中", "A1:D6", "claim-preview-making.png"],
  ["待制作", "A1:D6", "claim-preview-pending.png"],
  ["待制作原始数据", "A8:B10", "claim-preview-archive.png"],
];
for (const [sheetName, range, fileName] of ranges) {
  const preview = await workbook.render({ sheetName, range, scale: 1, format: "png" });
  await fs.writeFile(`${batch}/${fileName}`, new Uint8Array(await preview.arrayBuffer()));
}
const artifact = await SpreadsheetFile.exportXlsx(workbook);
await artifact.save(`${batch}/候选/术式制作表-artifact.xlsx`);
console.log(JSON.stringify({
  making: makingIds,
  pendingCount: pendingIds.length,
  archiveRow: archive.getRange("A9").values[0][0],
  detailSeq: detail.getRange("B7").values[0][0],
}));

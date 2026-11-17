import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const batch = "D:/alianximod/docs/开发记录/灼地产种设计入表-20261030";
const summary = JSON.parse(await fs.readFile(`${batch}/批注-summary.json`, "utf8"));
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(summary.candidate));
const pending = workbook.worksheets.getItem("待制作");
const expect = [
  [4, "fire_scorch_field", "火", 30],
  [5, "fire_meteorite", "火", 40],
  [6, "fire_shock_ring", "火", 16],
  [9, "fire_stagger", "无", null],
  [10, "fire_hit_continue", "无", null],
  [11, "fire_seed_sow", "火", 5],
  [15, "fire_break_ground", "土", null],
];
for (const [row, code, attribute, cooling] of expect) {
  if (pending.getRange(`B${row}`).values[0][0] !== code) throw new Error(`${code} 不在第${row}行`);
  if (pending.getRange(`D${row}`).values[0][0] !== attribute) throw new Error(`${code} 属性不对`);
  const actual = pending.getRange(`F${row}`).values[0][0];
  if (cooling === null) {
    if (actual !== null && actual !== "") throw new Error(`${code} 冷却被填上`);
  } else if (Number(actual) !== cooling) {
    throw new Error(`${code} 冷却不是${cooling}`);
  }
}
if (pending.getRange("C9").values[0][0] !== "通用" || pending.getRange("C10").values[0][0] !== "通用" || pending.getRange("C15").values[0][0] !== "通用") {
  throw new Error("通用类型被改掉");
}
const scorch = String(pending.getRange("N4").values[0][0]);
if (!scorch.includes("基础20点火种") || !scorch.includes("基础冷却30tick") || scorch.includes("费用、冷却")) {
  throw new Error("灼地需求不一致");
}
if (Number(pending.getRange("E4").values[0][0]) !== 1 || Number(pending.getRange("EC4").values[0][0]) !== 20) {
  throw new Error("灼地伤害或首条产种被改");
}
if (!String(pending.getRange("N15").values[0][0]).includes("其他属性术式仍可拼接使用")) {
  throw new Error("崩地拼接句缺失");
}
workbook.recalculate();
const preview = await workbook.render({ sheetName: "待制作", range: "A4:F15", scale: 1, format: "png" });
await fs.writeFile(`${batch}/批注-preview.png`, new Uint8Array(await preview.arrayBuffer()));
const artifact = await SpreadsheetFile.exportXlsx(workbook);
await artifact.save(`${batch}/批注-candidate-artifact.xlsx`);
console.log(JSON.stringify({ checked: expect.length }));

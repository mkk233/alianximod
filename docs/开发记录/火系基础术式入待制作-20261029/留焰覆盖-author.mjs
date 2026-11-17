import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const batch = "D:/alianximod/docs/开发记录/火系基础术式入待制作-20261029";
const summary = JSON.parse(await fs.readFile(`${batch}/留焰覆盖-summary.json`, "utf8"));
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(summary.candidate));
const sheet = workbook.worksheets.getItem("待制作");
const row = summary.row;
if (sheet.getRange(`B${row}`).values[0][0] !== "fire_flame_trail") throw new Error("id missing");
if (sheet.getRange(`A${row}`).values[0][0] !== "留焰") throw new Error("name missing");
if (sheet.getRange(`C${row}`).values[0][0] !== "通用") throw new Error("type missing");
if (sheet.getRange(`J${row}`).values[0][0] !== "是") throw new Error("enchant flag missing");
if (sheet.getRange(`P${row}`).values[0][0] !== "规则") throw new Error("release kind missing");
if (sheet.getRange(`E${row}`).values[0][0]) throw new Error("old damage remains");
if (sheet.getRange(`U${row}`).values[0][0]) throw new Error("old lifetime remains");
if (sheet.getRange("B3").values[0][0] === "fire_flame_flow") throw new Error("old id remains");
const feature = sheet.getRange(`N${row}`).values[0][0];
if (!feature.startsWith("留焰是火系附魔")) throw new Error("feature missing");
if (feature.includes("60tick") || feature.includes("基础2")) throw new Error("old attack numbers copied");
sheet.getRange(`A${row}`).values = [["留焰"]];
sheet.getRange(`B${row}`).values = [["fire_flame_trail"]];
sheet.getRange(`DW${row}`).values = [["否"]];
workbook.recalculate();
if (sheet.getRange(`B${row}`).values[0][0] !== "fire_flame_trail") throw new Error("id lost after recalculate");
const preview = await workbook.render({ sheetName: "待制作", range: "A1:D8", scale: 1, format: "png" });
const previewBytes = new Uint8Array(await preview.arrayBuffer());
await fs.writeFile(`${batch}/留焰覆盖-preview.png`, previewBytes);
const artifact = await SpreadsheetFile.exportXlsx(workbook);
await artifact.save(`${batch}/留焰覆盖-artifact.xlsx`);
console.log(JSON.stringify({
  previewBytes: previewBytes.length,
  name: sheet.getRange(`A${row}`).values[0][0],
  id: sheet.getRange(`B${row}`).values[0][0],
  kind: sheet.getRange(`P${row}`).values[0][0],
}));

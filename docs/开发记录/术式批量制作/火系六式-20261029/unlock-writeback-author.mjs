import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const root = "D:/alianximod/docs/开发记录/术式批量制作/火系六式-20261029";
const notes = JSON.parse(await fs.readFile(`${root}/unlock-writeback-notes.json`, "utf8"));
const artifactPath = `${root}/unlock-writeback-candidate-artifact.xlsx`;
const rows = [
  ["fire_liaoyuan", 3],
  ["fire_liaoli", 4],
  ["fire_fentian", 5],
];
const untouched = [
  ["fire_heat_up", 6],
  ["fire_burn", 7],
  ["fire_cool", 8],
];

const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(notes.backup));
const making = workbook.worksheets.getItem("制作中");
for (const [code, row] of untouched) {
  const actual = making.getRange(`B${row}`).values[0][0];
  const note = String(making.getRange(`DV${row}`).values[0][0] ?? "");
  if (actual !== code) throw new Error(`Id mismatch at row ${row}: ${actual}`);
  if (note.includes("刻印台实测反馈")) throw new Error(`Furnace DV${row} already has the feedback`);
}
for (const [code, row] of rows) {
  const actual = making.getRange(`B${row}`).values[0][0];
  const note = String(making.getRange(`DV${row}`).values[0][0] ?? "");
  if (actual !== code) throw new Error(`Id mismatch at row ${row}: ${actual}`);
  if (note.includes("刻印台实测反馈")) throw new Error(`DV${row} already has the feedback`);
  const feature = making.getRange(`N${row}`).values[0][0];
  const effect = making.getRange(`O${row}`).values[0][0];
  const addition = notes.additions[code];
  making.getRange(`DV${row}`).values = [[`${note}\n${addition}`]];
  if (making.getRange(`N${row}`).values[0][0] !== feature) throw new Error(`N changed for ${code}`);
  if (making.getRange(`O${row}`).values[0][0] !== effect) throw new Error(`O changed for ${code}`);
}
for (const [code, row] of untouched) {
  const note = String(making.getRange(`DV${row}`).values[0][0] ?? "");
  if (note.includes("刻印台实测反馈")) throw new Error(`Furnace DV${row} was edited`);
}
workbook.recalculate();
const output = await SpreadsheetFile.exportXlsx(workbook);
await output.save(artifactPath);
console.log(JSON.stringify({ artifact: artifactPath, rows: rows.map(([code, row]) => ({ code, row })) }));

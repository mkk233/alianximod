import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const root = "D:/alianximod/docs/开发记录/术式批量制作/火系六式-20261029";
const notes = JSON.parse(await fs.readFile(`${root}/seed-source-writeback-notes.json`, "utf8"));
const artifactPath = `${root}/seed-source-writeback-candidate-artifact.xlsx`;
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(notes.backup));
const done = workbook.worksheets.getItem("已完成");
for (const [code, addition] of Object.entries(notes.additions)) {
  const row = notes.rows[code];
  if (done.getRange(`B${row}`).values[0][0] !== code) throw new Error(`Id mismatch at row ${row}`);
  const feature = done.getRange(`N${row}`).values[0][0];
  const effect = done.getRange(`O${row}`).values[0][0];
  const current = done.getRange(`DV${row}`).values[0][0];
  if (current) throw new Error(`DV${row} was not empty`);
  done.getRange(`DV${row}`).values = [[addition]];
  if (done.getRange(`N${row}`).values[0][0] !== feature) throw new Error(`N changed for ${code}`);
  if (done.getRange(`O${row}`).values[0][0] !== effect) throw new Error(`O changed for ${code}`);
}
workbook.recalculate();
const output = await SpreadsheetFile.exportXlsx(workbook);
await output.save(artifactPath);
console.log(JSON.stringify({ artifact: artifactPath, rows: notes.rows }));

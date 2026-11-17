import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const root = "D:/alianximod/docs/开发记录/火种共享燃料改造-20261029";
const summary = JSON.parse(await fs.readFile(`${root}/docs-table-preserve-summary.json`, "utf8"));
const approved = JSON.parse(await fs.readFile(`${root}/approved-requirements.json`, "utf8"));
const current = JSON.parse(await fs.readFile(`${root}/current-input.json`, "utf8"));
const artifactPath = `${root}/docs-table-candidate-artifact.xlsx`;
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(summary.backup));
const sheet = workbook.worksheets.getItem("制作中");
const snapshot = Object.fromEntries(current.items.map((item) => [item.id, item]));
for (const [code, spec] of Object.entries(approved.items)) {
  const row = summary.rows[code];
  if (sheet.getRange(`B${row}`).values[0][0] !== code) throw new Error(`Id mismatch at row ${row}`);
  const before = snapshot[code];
  if (sheet.getRange(`N${row}`).values[0][0] !== before.n) throw new Error(`N snapshot mismatch for ${code}`);
  if (sheet.getRange(`O${row}`).values[0][0] !== before.o) throw new Error(`O snapshot mismatch for ${code}`);
  const nextFeature = spec.nMode === "replace" ? spec.n : `${before.n}\n${spec.nAppend}`;
  sheet.getRange(`N${row}`).values = [[nextFeature]];
  for (const [column, value] of Object.entries(spec.fields)) {
    if (sheet.getRange(`${column}${row}`).values[0][0] !== before.cells[column].text) {
      throw new Error(`${column}${row} snapshot mismatch`);
    }
    sheet.getRange(`${column}${row}`).values = [[value]];
  }
  const remark = sheet.getRange(`DV${row}`).values[0][0] ?? "";
  if (remark !== before.dv) throw new Error(`DV snapshot mismatch for ${code}`);
  sheet.getRange(`DV${row}`).values = [[`${remark}\n${approved.dvAppend[code]}`]];
  if (sheet.getRange(`O${row}`).values[0][0] !== before.o) throw new Error(`O changed for ${code}`);
  if (sheet.getRange(`B${row}`).values[0][0] !== code) throw new Error(`Id changed for ${code}`);
}
const output = await SpreadsheetFile.exportXlsx(workbook);
await output.save(artifactPath);
console.log(JSON.stringify({ artifact: artifactPath, rows: summary.rows, submitted: false }));

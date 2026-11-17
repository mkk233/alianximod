import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const candidate = process.argv[2];
const sheetName = process.argv[3];
const range = process.argv[4];
const png = process.argv[5];
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(candidate));
const names = ["已完成", "待制作", "制作中", "待验收"];
const counts = {};
const ids = [];
for (const name of names) {
  const values = workbook.worksheets.getItem(name).getRange("B3:B80").values.flat();
  const present = values.filter((value) => value !== null && value !== "");
  counts[name] = present.length;
  ids.push(...present);
}
const pending = workbook.worksheets.getItem("待制作");
const rows = [];
for (let row = 3; row <= 10; row += 1) {
  const values = pending.getRange(`A${row}:X${row}`).values[0];
  const note = pending.getRange(`DV${row}`).values[0][0];
  rows.push({
    row,
    name: values[0],
    id: values[1],
    cool: values[5],
    description: values[13],
    visual: values[14],
    role: values[18],
    pull: values[23],
    noteStarts: String(note).slice(0, 12),
    noteHasQuote: String(note).includes("帮我做几个术式"),
    noteHasReview: String(note).includes("已复审") || String(note).includes("已验收"),
  });
}
workbook.recalculate();
const scan = await workbook.inspect({
  kind: "match",
  searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#NUM!|#NULL!|#SPILL!|#CALC!",
  options: { useRegex: true, maxResults: 20 },
  maxChars: 1000,
});
const picture = await workbook.render({ sheetName, range, scale: 1, format: "png" });
await fs.writeFile(png, new Uint8Array(await picture.arrayBuffer()));
console.log(JSON.stringify({
  counts,
  total: ids.length,
  unique: new Set(ids).size,
  rows,
  scan: scan.ndjson,
  png,
}, null, 2));

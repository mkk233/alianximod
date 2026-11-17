import crypto from "node:crypto";
import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const root = "D:/alianximod/docs/开发记录/术式批量制作/火系六式-20261029";
const livePath = "D:/alianximod/docs/术式制作表.xlsx";
const backupPath = `${root}/备份/术式制作表-20261029-023143-1.xlsx`;
const artifactPath = `${root}/table-candidate-artifact.xlsx`;
const preservedPath = `${root}/table-candidate-preserved.xlsx`;
const baselineSha = "35CC4218A03152A4C9880A44EC20F99485E884141027909055603FB04D6E4803";
const marker = "制作中 | 火系六式-20261029 | docs/开发记录/术式批量制作/火系六式-20261029/记录.md";
const ids = [
  "fire_liaoyuan",
  "fire_liaoli",
  "fire_fentian",
  "fire_heat_up",
  "fire_burn",
  "fire_cool",
];

function sha256(bytes) {
  return crypto.createHash("sha256").update(bytes).digest("hex").toUpperCase();
}

function same(actual, expected) {
  if (actual == null && expected == null) return true;
  if (typeof expected === "number" || typeof actual === "number") {
    return Number(actual) === Number(expected);
  }
  return actual === expected;
}

function withMarker(value) {
  const text = value == null ? "" : String(value);
  if (text.includes(marker)) throw new Error("DV already contains the claim marker");
  return text === "" ? marker : `${text.endsWith("\n") ? text : `${text}\n`}${marker}`;
}

const mode = process.argv.includes("--reopen")
  ? "reopen"
  : process.argv.includes("--preview")
    ? "preview"
    : process.argv.includes("--apply")
      ? "apply"
      : "";
if (!mode) {
  console.error("Use exactly one of --apply, --reopen, or --preview");
  process.exit(2);
}

if (mode === "preview") {
  const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(preservedPath));
  const idsBlob = await workbook.render({
    sheetName: "制作中",
    range: "A3:B8",
    scale: 2,
    format: "png",
    autoCrop: "all",
  });
  const textBlob = await workbook.render({
    sheetName: "制作中",
    range: "N3:O8",
    scale: 1,
    format: "png",
    autoCrop: "all",
  });
  await fs.writeFile(`${root}/table-preview-ids.png`, new Uint8Array(await idsBlob.arrayBuffer()));
  await fs.writeFile(`${root}/table-preview-text.png`, new Uint8Array(await textBlob.arrayBuffer()));
  console.log(JSON.stringify({
    previewIds: `${root}/table-preview-ids.png`,
    previewText: `${root}/table-preview-text.png`,
  }));
  process.exit(0);
}

if (mode === "reopen") {
  const reopenPath = process.argv.includes("--live") ? livePath : preservedPath;
  const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(reopenPath));
  const counts = {};
  const found = [];
  for (const sheetName of ["已完成", "待制作", "制作中", "待验收"]) {
    const sheet = workbook.worksheets.getItem(sheetName);
    const used = sheet.getUsedRange(true);
    const values = used ? used.values : [];
    let count = 0;
    for (let rowIndex = 2; rowIndex < values.length; rowIndex += 1) {
      const code = values[rowIndex]?.[1];
      if (code != null && String(code).trim() !== "") {
        count += 1;
        found.push([String(code).trim(), sheetName, rowIndex + 1]);
      }
    }
    counts[sheetName] = count;
  }
  const codes = found.map((item) => item[0]);
  const unique = new Set(codes);
  const claimed = found.filter((item) => ids.includes(item[0]));
  const making = workbook.worksheets.getItem("制作中");
  const notes = making.getRange("DV3:DV8").values.map((row) => String(row[0] ?? ""));
  const pending = workbook.worksheets.getItem("待制作");
  const pendingCodes = pending.getRange("B3:B8").values.map((row) => row[0] ?? null);
  const errors = await workbook.inspect({
    kind: "match",
    range: "制作中!A3:EN8",
    searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A|#NUM!|#NULL!|#SPILL!|#CALC!",
    options: { useRegex: true, maxResults: 20 },
    summary: "claimed row formula error scan",
    maxChars: 800,
  });
  const failed = [];
  if (codes.length !== unique.size) failed.push("duplicate ids");
  if (claimed.length !== 6 || claimed.some((item) => item[1] !== "制作中")) failed.push("claim location");
  if (notes.some((note) => !note.endsWith(marker))) failed.push("marker missing");
  if (pendingCodes.some((code) => code != null && String(code).trim() !== "")) failed.push("pending still has ids");
  console.log(JSON.stringify({
    reopened: reopenPath,
    counts,
    total: codes.length,
    unique: unique.size,
    claimed,
    pendingCodes,
    markerOnAll: notes.every((note) => note.endsWith(marker)),
    errorScan: errors.ndjson,
    failed,
  }));
  if (failed.length) process.exit(1);
  process.exit(0);
}

const backupBytes = await fs.readFile(backupPath);
const backupHash = sha256(backupBytes);
if (backupHash !== baselineSha) {
  throw new Error(`Backup hash is not the baseline: ${backupHash}`);
}
let liveHash = null;
try {
  liveHash = sha256(await fs.readFile(livePath));
} catch (error) {
  liveHash = `unreadable:${error.code || error.message}`;
}
if (liveHash !== baselineSha) {
  throw new Error(`Live saved hash changed before authoring: ${liveHash}`);
}

const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(backupPath));
const pending = workbook.worksheets.getItem("待制作");
const making = workbook.worksheets.getItem("制作中");
const original = pending.getRange("A3:EN8").values;
if (original.length !== 6 || original.some((row) => row.length !== 144)) {
  throw new Error(`Unexpected source shape ${original.length} x ${original[0]?.length}`);
}
original.forEach((row, index) => {
  if (row[1] !== ids[index]) throw new Error(`Source id mismatch at row ${index + 3}: ${row[1]}`);
});
const moved = original.map((row) => row.slice());
moved.forEach((row) => {
  row[125] = withMarker(row[125]);
});
making.getRange("A3:EN8").values = moved;
const readBack = making.getRange("A3:EN8").values;
const mismatches = [];
for (let rowIndex = 0; rowIndex < 6; rowIndex += 1) {
  for (let columnIndex = 0; columnIndex < 144; columnIndex += 1) {
    const expected = columnIndex === 125 ? moved[rowIndex][columnIndex] : original[rowIndex][columnIndex];
    if (!same(readBack[rowIndex][columnIndex], expected)) {
      mismatches.push({ row: rowIndex + 3, column: columnIndex + 1 });
    }
  }
}
if (mismatches.length) {
  console.error(JSON.stringify(mismatches.slice(0, 12)));
  throw new Error(`Target mismatch count ${mismatches.length}`);
}
pending.getRange("A3:EN8").clear({ applyTo: "contents" });
const cleared = pending.getRange("B3:B8").values.map((row) => row[0] ?? null);
if (cleared.some((code) => code != null && String(code).trim() !== "")) {
  throw new Error(`Source ids remain after clear: ${JSON.stringify(cleared)}`);
}
workbook.recalculate();
const inspect = await workbook.inspect({
  kind: "table",
  range: "制作中!A3:D8",
  include: "values",
  tableMaxRows: 6,
  tableMaxCols: 4,
  maxChars: 2000,
});
console.log(inspect.ndjson);
const output = await SpreadsheetFile.exportXlsx(workbook);
await output.save(artifactPath);
console.log(JSON.stringify({
  artifact: artifactPath,
  liveHash,
  backupHash,
  mismatches: 0,
  cleared,
  ids,
}));

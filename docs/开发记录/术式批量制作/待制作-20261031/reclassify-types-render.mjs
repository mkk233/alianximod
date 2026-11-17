import { SpreadsheetFile } from "@oai/artifact-tool";
import { readFileSync, writeFileSync } from "node:fs";

const input = readFileSync(process.argv[2]);
const workbook = await SpreadsheetFile.importXlsx(
  input.buffer.slice(input.byteOffset, input.byteOffset + input.byteLength)
);
const preview = await workbook.render({
  sheetName: process.argv[3],
  range: process.argv[4],
  scale: 2,
  format: "png",
});
writeFileSync(process.argv[5], new Uint8Array(await preview.arrayBuffer()));

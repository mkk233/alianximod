import { SpreadsheetFile } from "@oai/artifact-tool";
import { readFileSync, writeFileSync } from "node:fs";

const input = readFileSync(process.argv[2]);
const workbook = await SpreadsheetFile.importXlsx(
  input.buffer.slice(input.byteOffset, input.byteOffset + input.byteLength)
);
const preview = await workbook.render({
  sheetName: "数据表",
  range: "Q4:S10",
  scale: 2,
  format: "png",
});
writeFileSync(process.argv[3], new Uint8Array(await preview.arrayBuffer()));

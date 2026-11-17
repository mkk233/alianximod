import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const batch = "D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031";
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(`${batch}/claim-prepare-candidate-preserved.xlsx`));
const making = workbook.worksheets.getItem("制作中");
const pending = workbook.worksheets.getItem("待制作");
if (making.getRange("B6").values[0][0] !== "fire_flame_trail") throw new Error("制作中第6行不是留焰");
if (making.getRange("B18").values[0][0] !== "fire_spark") throw new Error("制作中第18行不是火苗");
if (pending.getRange("B3").values[0][0]) throw new Error("待制作仍有数据");
const preview = await workbook.render({ sheetName: "制作中", range: "A3:B8", scale: 1, format: "png" });
await fs.writeFile(`${batch}/claim-prepare-preview.png`, new Uint8Array(await preview.arrayBuffer()));
console.log("preview-ok");

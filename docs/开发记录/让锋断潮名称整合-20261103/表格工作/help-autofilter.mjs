import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load("D:/alianximod/docs/术式制作表.xlsx"));
const help = workbook.help("autoFilter", { include: "index,examples,notes", maxChars: 3000 });
console.log(help.ndjson);

import {Client} from 'file:///C:/Users/baishang/AppData/Local/npm-cache/_npx/99e3e8e098e94b9f/node_modules/@modelcontextprotocol/sdk/dist/esm/client/index.js';
import {StdioClientTransport} from 'file:///C:/Users/baishang/AppData/Local/npm-cache/_npx/99e3e8e098e94b9f/node_modules/@modelcontextprotocol/sdk/dist/esm/client/stdio.js';
const client=new Client({name:'codex-ps-draw',version:'1.0.0'});
const transport=new StdioClientTransport({command:'D:/Program Files/nodejs/node.exe',args:['C:/Users/baishang/AppData/Local/npm-cache/_npx/99e3e8e098e94b9f/node_modules/@alisaitteke/photoshop-mcp/dist/index.js'],env:{...process.env,LOG_LEVEL:'1',ANALYTICS_DISABLED:'1',PHOTOSHOP_PATH:'D:\\Adobe\\Adobe Photoshop 2024\\Photoshop.exe'},stderr:'pipe'});
await client.connect(transport);
import fs from 'node:fs';
try {
await client.callTool({name:'photoshop_get_capabilities',arguments:{}});
console.log('STATE',JSON.stringify(await client.callTool({name:'photoshop_get_state',arguments:{}})));
const request=JSON.parse(fs.readFileSync(process.argv[2],'utf8').replace(/^\uFEFF/,''));
const result=await client.callTool(request,undefined,{timeout:180000});
for(const c of result.content||[]) {if(c.type==='image') {const p='D:/alianximod/docs/资源库/过程归档/水流长带-20261024/mcp-preview.jpg';fs.writeFileSync(p,Buffer.from(c.data,'base64'));console.log('PREVIEW '+p);}else if(c.type==='text')console.log(c.text);}
} finally {await client.close();}




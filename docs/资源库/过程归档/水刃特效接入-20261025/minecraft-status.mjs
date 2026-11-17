import {Client} from 'file:///C:/Users/baishang/AppData/Local/npm-cache/_npx/99e3e8e098e94b9f/node_modules/@modelcontextprotocol/sdk/dist/esm/client/index.js';
import {StdioClientTransport} from 'file:///C:/Users/baishang/AppData/Local/npm-cache/_npx/99e3e8e098e94b9f/node_modules/@modelcontextprotocol/sdk/dist/esm/client/stdio.js';
import fs from 'node:fs';
const transport=new StdioClientTransport({command:'C:/Windows/System32/cmd.exe',args:['/d','/s','/c','npx -y minecraft-mod-mcp'],stderr:'pipe'});
const client=new Client({name:'water-fx-status-check',version:'1.0.0'});
try{await client.connect(transport,{timeout:30000});const list=await client.listTools();const t=list.tools.find(x=>x.name==='ping')||list.tools.find(x=>/get_minecraft_status$/.test(x.name));if(!t)throw Error('No status tool');const result=await client.callTool({name:t.name,arguments:{}},undefined,{timeout:30000});fs.writeFileSync(new URL('./minecraft-status.json',import.meta.url),JSON.stringify({tool:t.name,result},null,2));console.log(JSON.stringify({tool:t.name,result}));}catch(e){console.log(JSON.stringify({error:String(e)}));}finally{await client.close().catch(()=>{});await transport.close().catch(()=>{});}

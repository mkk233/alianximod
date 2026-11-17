# 焚天自然收束同步：美术完成回执

batchId=`待制作-20261031`；taskId=`fire_kindle_v2_art`（关联fire_fentian）；attemptId=`20261116-art-fentian-burnout-sync-06`。
直接父级主会话为任务包标识 `01a1ce06-b681-7c90-b11d-260d3c169911`；作者沿用可观察会话 `01a1e08a-817e-7b60-b465-7a8baaca1f4b`，不编造未知父代理UUID。
美术角色 `gpt-6-astra max`；沿用本批408/431行Carbon、Argon明确Unknown model后合法降级，不称Google产出，未重新探测或换路由，实际服务后端未独立核实。
设计2/2、实现3/3。本阶段额度已用完，返修结果未追加复审。

用户原话：“这个版本很好，但是做背景的老特效没有自然熄灭动画而是直接消失和全身的燃烧效果不同步，把这个小瑕疵修一下”。
制作完成；修复已授权，当前新尾段状态 `candidate_pending_user_confirmation`，不冒称用户已经看过或确认新系数。不是等待开工授权，不新做正式复审。

## 实际改动

仅自然结束做alpha过渡：保留最后实际持续帧，9tick内alpha为 `[1,.94,.82,.66,.48,.30,.15,.05,0]`，原余烬alpha为 `[0,.18,.36,.55,.72,.82,.65,.35,0]`，原余烬索引 `[40,41,42,44,45,46,48,50,51]`。前景索引仍 `[10,11,12,13,14,15,16,17,18]`。tick8两层全透明，tick9统一清理；背景系数允许partialTick线性插值，前景取帧不改。

使用普通source-over，同一背景几何先满火样本后余烬、再原前景；不加新空间片、不改RGB/染色，不改变满火、尺寸、锚点、CAMERA_PLANE或伤害/燃料。演示heldFrame=39/F40；运行应捕获真正最后显示的持续帧及结束前实际scale，防止进入尾段跳帧或缩放。引燃不动。

批准的52/19帧PNG/.aseprite全部保持原字节，持续0..39不动。新增九帧PNG只是本目录内预览烘焙候选，不作为新运行资源。详细时间轴与接入边界见过程目录 `timeline.json`、`visual-handoff.md`。

## 主要产物

过程根：`D:/alianximod/docs/资源库/过程归档/焚天_双层收束同步_v4_1_20261116/`。
- `source/background_burnout_alpha_preview.aseprite`：九帧、满火/余烬两个可编辑预览层。
- `source/dual_layer_burnout_preview.aseprite`：带正常目标的二维演示。
- `textures/background_burnout_9tick_preview.png`：128×1296，9帧128×144透明预览条。
- `previews/burnout_9tick.gif`：576×648，9×50ms，实底最近邻效果；450ms仅演示自然结束，不是伤害时长。
- `previews/burnout_t0.png`、`burnout_t3.png`、`burnout_t6.png`、`burnout_t8.png`，另有原尺寸t0参照。
- `previews/gui_burnout_t0.png`、`gui_burnout_t3.png`、`gui_burnout_t6.png`、`gui_burnout_t8.png`：四张实际GUI主视口。
- `blockbench/fentian_dual_layer_burnout_v4_1.bbmodel`：v4副本，最终SHA256 `3D9DEB29986BF7A814031BDD84F373FCC524AB5B0EAF453427AA1A924319ED87`。
- `manifest.json`：文件大小/哈希、工具证据和新尾段状态；`evidence/`保留原工程保护与保存复开事实。

## 工具与实际限制

Aseprite1.3.17 batch ready/live=false，真实隔离batch复开两源并导出，未保存批准源；本轮Lua调用成功。Blockbench Desktop5.2.2/plugin1.10.1实际创建v4副本、贴九帧预览、截图、保存与复开。

初始v4 UUID `fe507dca-652f-f434-b477-f92726a559a7` 保留。副本d9c43eb2-d302-684c-1927-59016d61f4f9，首次复开7fa628fc-3a07-adca-a574-ce45af530859，最终复开2ddaf358-56ef-cd78-263a-4e6afb143ef0。11cubes/3groups/3textures不变，3/3贴图loaded/error=0，12个原项目可观察状态一致，0文档关闭；每次修改有本项UUID守卫，无抢占。

一次视口自检发现BB emissive分支强制alpha=1，修正为本副本默认透明材质、关闭面向明暗以保持fullbright，保留深度测试及不写深度。最终四图已重取并view_image检查，不把错误视口当交付；这不是运行参数变更。四张二维关键帧也已view_image检查。

GUI PNG保留透明alpha，部分查看器若忽略alpha会误显示满色；最终同一背景点alpha为255/168/38/0，实底二维GIF可以直接看渐隐。未做离线截图拼图、桌面控制、headless或imagegen。BB内嵌纹理重编码不是运行源，禁止反导替换批准PNG。没有自称Forge已验证或用户已确认新版本。

## 收束

只读实现者已收束回执，未读/复审Java。仅写本新过程目录及本回执；未写旧源、正式资源、src、表、总索引/批次记录/实测清单。未编译、未打包、未运行游戏。无下属、无后台制作进程，GUI工具占用与文件写入最终收束。

下一步由父级按已有修复授权交Grok落实此最小过渡并编译，展示此版；不再次询问开工，也不以本美术回执冒充接入、游戏验收或迁页完成。

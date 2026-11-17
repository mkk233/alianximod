你是Cursor归档收尾writer，用户已授权本水环正式入库并游戏接入。本轮代码/编译完成后执行此任务。不得改游戏Java或任何原画源，只编辑以下4份文档和必要归档核验日志：
D:/alianximod/docs/资源库/特效/水流/周身水环_细碎毛边_v8/README.md
同目录asset_manifest.json
D:/alianximod/docs/资源库/README.md（只v8对应项）
D:/alianximod/docs/资源库/过程归档/周身水环3D验证_v8_细碎水膜毛边-20261027/README.md（仅采用状态）
先读过程目录水环接入-20261027/integration-validation.md和compile.log，确认实际compileJava成功，不以计划代替事实。成功则状态统一“已接入游戏，编译通过，待用户游戏内验证”，game_integrated=true compiled=true in_game_tested=false。若不成功直接报告不篡改状态。
正式README保留文件链接，补runtime三文件说明与对应游戏路径，3mesh 768quads无人物，48tick圈、16帧每2tick，body24 outerinner40，原模型倾斜和共根旋转；不改防御玩法。删除“本次只整理没有改游戏代码”等已过时句子。说明测试用户负责，未打包。
manifest把runtime/water_ring_v8.png、.png.mcmeta、water_ring_v8_mesh.json纳入，记录sha256/bytes和game_path/hash对应；原13条文件保留并更新状态，README更新后重新hash，manifest自身不hash。源aseprite/bbmodel/preview哈希不该变化。所有runtime必须与src实际字节相同。
资源库入口和过程稿采用状态一致，保留旧版本和全部旧日志。archive-result.md里旧manifest哈希是归档当时快照，不应冒充最终hash；另写final-archive-validation.json，记录当前manifestsha和逐文件核验即可。不要打包/运行游戏/commit，不派代理，不通读整仓。完成写finalize-report.md。

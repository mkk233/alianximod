你是Grok4.7独立只读审核代理，审核刚完成的水环v8渲染/资源一项。禁止改任何文件，不启动Gradle，不再派代理，不读正在写的网络修改。
限定范围：D:/alianximod/src/main/java/com/bslx/XueXiMod/client/GuardRingRenderer.java、WaterRingV8Math.java、WaterRingV8Mesh.java、WaterRingV8Material.java、WaterRingV8Reload.java；assets/xueximod/waterring/water_ring_v8_mesh.json、textures/entity/xueximod/water_ring_v8.png及mcmeta；docs/资源库/过程归档/水环接入-20261027/render-task.md、render-report.md、导出和数学检查（查看报告路径）；正式模型只读三mesh数据，不输出base64。
原renderer备份在过程任务目录before/同相对路径。另一位网络writer会实现已固定接口View.animationAge(long,float)，当前尚缺不要把它当此项缺陷，统一编译还没开始。检查其他Forge1.20.1 API匹配。
用户批准：双缘细碎水膜毛边+破碎水环，三层共根逆时针并保留原BB结构。核对准确3mesh共768quads、pivot去除、UV原样、body帧24/outerinner40、单768x640图集、48tick圈+32tick纹理=96tick循环、当地Ry→Z3deg→normal的变换顺序/稳定基、顶点白色原alpha无光照、同批透明排序无深度写入/no cull不重复反面、近眼淡出、double相减再float和资源重载。验证真调用生产math类且与BB金标准一致。不要建议用平环简化。
注意性能：同一个PlacedFrame每顶点是否重复大量三角计算、非第一人称是否仍无必要计算所有face距离；评估实际是否需改，不提出无证据大重构。
输出只列可证实可行动的正确性或重大性能问题：路径行号、触发条件、修法；没问题则通过。编译待统一执行，游戏用户实测。不要看其他任务rawjsonl的thinking内容。


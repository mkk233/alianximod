# 雾雨领域环状雾幕接入裁决

2026-10-26；用户指定 Cursor grok-4.7-high-fast 修改并编译。我方只读核对。不得打包、启动或操控游戏。

## 当前事实与粉色根因

DomainWorldRenderer 当前对非PRESSURE视觉绘制六面 water_still 盒子，顶点色(90,170,255,70)，overlayCoords(0)。DomainMistRainRenderer 使用已归档 mist_rain_80.png，顶点色白色，但同样 overlayCoords(0)。两者通过 DomainSheetMesh 使用 ENTITY_TRANSLUCENT shader，并开启 OVERLAY。

已用本机1.20.1映射Minecraft JAR的javap核对 OverlayTexture：NO_OVERLAY 实际为 pack(0,10)，并非0；overlay纹理前8行填充受伤红色，pack(0,0)选中了红色覆盖区。因此“雾和白色顶点雨丝一起变粉”有明确共同代码原因：错误的overlay坐标。应改本次雾、雨顶点为 OverlayTexture.NO_OVERLAY，不靠调整青色或关闭全部overlay抵消。

游戏雨PNG与归档哈希一致，均为4D578C45469F3F9265AE254BD07A4BE8AA09B36B17D5C6512604DA2B544A66C4。当前latest.log定向检索未发现这两个纹理加载失败，只出现无关skill_storage模型缺纹理警告。不能据此声称运行时已经验证，但不应把缺失纹理作为已有证实的根因。

## 实施范围

1. 仅 DomainVisual.VOLUME（当前雾雨领域）替换旧雾盒；PRESSURE及其他视觉分支保持原行为。VOLUME不再提交地板、天花板或旧四面water_still。
2. 复制确认的 mist_panel.png 到 assets/xueximod/textures/domain/mist_panel.png，验证哈希与128×2048尺寸，32帧128×64。不复制预览底色。保留细雨贴图与布局。
3. 可新增 DomainMistRingRenderer，入口由DomainWorldRenderer按VOLUME调用。DomainSheetMesh保留COLOR_WRITE、LEQUAL、NO_CULL、透明混合、最近邻/无mipmap；顶点白色，不再乘旧雾盒蓝色与70/255透明度。原图自身alpha已很低。
4. DomainMistRainRenderer仅修正NO_OVERLAY及必要统一排序接口，不改玩法、雨丝角度、动画速度或密度。其余领域的地板天花板不能随此次删除。
5. 不改DomainDefinition、DomainBoundary、碰撞、领域范围或持续时间。环形只是视觉，现有领域逻辑仍是盒状；不能为了画圆偷偷把范围变成圆。

## 16面环与连续UV

- 采用16个首尾共边的竖直侧面，中心为bounds水平中心，水平半径分别取bounds半宽和半深，构成内接椭圆环；上下取bounds.minY/maxY。明确这是内接的视觉环，不再表示方形四角边界。
- 相邻面共享精确端点，最后一面回到首点，不重叠加宽、不加上下盖。只提交一份NO_CULL面，不能另画反向面加深颜色。
- 按累计边长生成连续周向U，不在每个面重置整张图；所有面共用同一帧和动画相位，不能每面随机错帧。
- 整圈选整数个128px宽纹理重复，使首尾U闭合。推荐根据周长/(2*高度)取最近正整数，至少1；默认7×7×8时可用1圈纹理。由于周长/高度通常不恰为2，允许为闭环做小幅横向比例适配，不能同时承诺任意尺寸下严格2:1和整数闭环。
- 纹理本身横向64px周期，128px一帧内有两周期。U跨整数时按重复采样，不对每面单独fract导致插值折返；如手动取模应先在接缝处拆分几何。
- 纵向只映射一次localV=0..1，顶部0、底部1，上下透明边缘保留，绝不纵向平铺整张序列。v=(frame+localV)/32，采样边界作必要保护防串帧。
- frame=floorMod(gameTime-visualOrigin+稳定实例相位,32)，每tick一帧。环和细雨均遵守visualOrigin<=gameTime<visualEnd，移除快照自然停止；不新建客户端余留。

## 透明排序与材质检查

只写颜色保证雾不把内部雨幕深度挡掉，但不同纹理先全部雾后全部雨仍不是正确跨材质远近排序。建议将本任务VOLUME环面与雨格收集成有限数量的绘制面，按相机距离由远到近统一排序，纹理切换时结束前一批，连续同纹理合并提交。不要无参数endBatch刷新其他系统。若保持分批近似，必须在报告中明确远侧雾/雨交错的排序限制，不能宣称已修全部透明问题。

材质类型应稳定缓存/静态持有，不每面每帧重新create。继续保留场景深度测试，不穿墙；不要用全局depthMask临时开关绕开RenderType状态。

检查ENTITY_TRANSLUCENT片元shader的低alpha丢弃阈值：原图含alpha10/18/27/37/48/60，若shader丢弃alpha<0.1，前两档会消失。这不同于粉色根因。不要未经说明把整体alpha加倍；若要求完整保留全部低alpha档，应为该雾层使用只丢弃零alpha的兼容专用shader，而非修改全局原版shader。最小版可先保留现有shader并如实记录低alpha档限制。

## 验证与交付

- 静态确认仅VOLUME无顶底面，16侧面共享边并闭合，周向UV不逐面重置，环内无重复反面；雨雾全部NO_OVERLAY。
- 核对素材哈希、尺寸、32帧和生命周期边界；资源路径可加载的静态检查，不把编译成功等同运行时验证。
- Cursor使用明确复用的Gradle缓存运行compileJava --offline --no-daemon；必要时只补依赖，不改无关构建配置。不build、不打jar、不游戏测试。
- 更新资源README、总索引与实施结果：采用版本、范围仍为盒状、编译结果、游戏内粉色修复与环形观感尚未实测。保留PSD、历史素材。

允许的主要代码范围：DomainWorldRenderer、DomainSheetMesh、DomainMistRainRenderer及新增的雾环/有限面排序辅助类；若增加专用shader只限本雾层资源。共享文件写前重新读，避免覆盖并发改动。

## 补充裁决：必须保留已接受素材的低alpha
实际导出alpha为0/8/18/26/36。要求为雾幕使用专用shader保留所有非零alpha，不能以丢弃8/18两档作为交付限制。以本机Minecraft1.20.1实际实体透明shader为基底，保留光照、雾化、采样等逻辑，仅将alpha<0.1丢弃改为alpha<=0.0。客户端RegisterShadersEvent注册、NEW_ENTITY格式、雾幕专用RenderType；其他材质及雨继续原shader。验证源文件差异仅必要命名引用和丢弃阈值，确认资源注册与RenderType引用。compileJava不能验证GLSL运行时加载，交付明确此项游戏内未测。不要把alpha加倍冒充保留。

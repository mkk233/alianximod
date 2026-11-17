# 环状雾幕定向审查

2026-10-26。仅阅读实现、三份既有Java文件的baseline差异和本机原版shader；没有修改游戏代码、没有重复编译或游戏测试。

最终结论：主要裁决已落实，远坐标精度P2已关闭。本轮定向审查无待修复项；游戏内观感和GLSL实际加载仍未验证。

## 修复复核：P2关闭

已重新读取最终DomainMistRingLayout与DomainMistRingRenderer。水平顶点现保存为相对中心的double局部坐标，世界中心及上下高度保留double；周长、边长和法线先在局部double中计算。renderer传入double相机坐标，通过relativeX/Z及relativeLowerY/UpperY先求相对坐标再转float，原来的大世界坐标提前量化路径已经消除。

实施结果记录新增±3000万及混合正负坐标平移一致性检查，并报告修复后compileJava离线成功22秒。本次仅核对源码与记录，没有重复编译或测试。以下保留初审问题说明供追溯，不再作为开放问题。

## [P2] 世界坐标提前转float，远坐标雾环失真或消失

位置：DomainMistRingLayout.tryBuild 中把世界中心、上下高度、世界X/Z顶点保存为float；DomainMistRingRenderer.collect又先把camera.x/z转float，再相减。

这不是相机相对坐标转换：在百万级坐标已经会出现分级抖动，在千万级坐标float间距达到1格，7格宽16边形的一些相邻顶点会量化到同一点；tryBuild的edgeLength检查可能直接返回null，整圈不显示。雨幕当前采用(double世界坐标-double相机坐标)后转float，不存在同样的精度损失，因此二者也可能错位。

最小修正：布局存相对中心的局部顶点与局部高度，或将世界顶点/高度保留为double；renderer始终先以double减camera，再转float提交。累计边长与法线也用未量化的局部/double差值计算。不要通过放宽零边长阈值掩盖退化。补充原点与平移到千万级坐标的相同盒子布局对比，要求相机相对形状及16条非零边一致。

## 已通过的检查

- DomainWorldRenderer对VOLUME提前分派，只收雾环与雨；旧六面水纹仍保留给其他非PRESSURE视觉。重压入口保持原分支；未发现取消其他领域顶底面的改动。
- 环布局确实16面、首尾取同一顶点索引，无顶底面、无额外反向面。U按累计周长连续，终点整数重复，不按面重置或逐点fract。
- 帧号按visualOrigin和animSeed取32周期，环和雨均在出生前、visualEnd及之后不提交。V半纹素内收位于当前帧内，垂直不平铺序列。
- 所有本批环/雨面经DomainVolumeBatch提交白色顶点与OverlayTexture.NO_OVERLAY；雨PNG不受无关改色。只写颜色、保留LEQUAL、无剔除。
- DomainVolumeBatch对所有VOLUME面统一按中心距离降序，材质切换结束上一批，每个四边形仅提交4顶点，未发现重复绘制或无参endBatch。
- 雾环静态MESH绑定mistCurtain，其ShaderStateShard通过DomainMistShaders::curtain取得RegisterShadersEvent加载对象；订阅为客户端MOD总线，格式NEW_ENTITY，资源路径匹配。雨及旧材质仍引用原版shader。
- 独立读取本机client-extra.jar逐字比对：vsh完全一致；fsh仅color.a<0.1改为color.a<=0.0；json仅顶点/片元程序名改到模组命名空间。没有额外修改光照、overlay、雾化或采样。

## 保留限制

中心距离排序不是任意交叉透明多边形的精确排序，大环面与雨格局部穿插仍可能有观感偏差；报告已准确注明，不追加无关大规模透明渲染改造。领域玩法仍是原盒状范围、环为内接视觉。

实施报告记录初次compileJava离线成功27秒、精度修复后成功22秒，本审查未重跑；这些结果不能验证GLSL加载或游戏内透明观感。仍不打包、不游戏测试。

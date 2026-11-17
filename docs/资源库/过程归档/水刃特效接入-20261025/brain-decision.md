# GPT-6-astra 最终裁决：水刃视觉接入

已只读核对 WaterArcRenderer、TechniqueHitRenderer、ArcBandSnapshot 及素材制作脚本。选择甲：沿用6tick攻击+4tick消退；不增加客户端余留，不改战斗/动作生命周期。

## 时间

继续以 level.gameTime - birthTick + partialTicks 为age。birthTick<0或age<0不画。

- 0<=age<=active：frame=floor(6*age/active)。
- active<age<life：frame=min(15,6+floor(10*(age-active)/tail))。
- age>=life不画。frame15透明，不取模循环。

第6帧峰值从age=6开始保留约0.4tick，末尾约0.4tick透明。不要age=6直接跳帧7。可保留旧淡出alpha，但不得把旧程序色带与新贴图双层绘制。

## 几何

三拍真实扫角为90/-90/140度，素材峰值约217度，不能直接画完整方形billboard，也不能普通矩形UV二次弯曲。

沿用现有弧带几何，加素材极坐标UV映射：

1. 世界顶点由snapshot forwardAxis/crossAxis、startAngle/sweepAngle、radius/halfWidth决定。负扫角自然反向，不读取相机朝向；不重复平移，不重复乘尺寸倍率。
2. 源图椭圆中心(32,34)，半轴(25.5,24)。沿原图弧面采样后映射到实际弧带，避免整张弧图再弯一次。
3. 源图前7帧浪头ends=[-199,-187,-171,-142,-96,-43,4]，源起点-213度。活动阶段将当前帧的源角区间[-213,ends[frame]]映射到世界进度[0,shown]，确保浪头在真实扫描前沿。帧号线性映射本身不保证对齐，必须处理此角度差异。
4. 消退阶段世界进度固定1，保持角度映射连续并让尾部按素材消散。素材后期超出峰值角度的装饰可裁，不扩大攻击扇区、不修改snapshot。
5. 可让装饰浪纹/水滴略超窄碰撞带；主体中心线、起终方向和最终尺寸须一致。明确半径方向的取样覆盖与UV帧内边界，禁止采到邻帧。

## 验证及所有权

只做该视觉接入；改动范围优先WaterArcRenderer、必要的独立客户端采样帮助类、新PNG与针对性CPU检查/文档。TechniqueHitRenderer只有确需才小改。ArcBandSnapshot、ProgressArcBand、WaterTechniques及所有伤害/动作系统保持不改。

已保存接入前基线：docs/资源库/过程归档/水刃特效接入-20261025/baseline/，审查可比较本批真实增量，别把全工作区既有改动算本次。

检查三拍正负扫角、age0/6/10边界、末帧、UV不串帧、无双重平移与尺度。完成运行compileJava --offline --no-daemon。游戏桥主控刚检测连接阶段Connection closed；允许再做只读ping确认，不启动第二客户端。无法游戏验证必须明确报告。

用户已批准此次模型替换为Cursor Grok4.7 High Fast。直接完成实现，不再请求模型确认，不委派；主控另派独立审查。禁止提交、推送。保存本批修改清单及测试结果到同一过程目录 implementation-result.md。

# 燎里命中火环、常驻火种火苗与高速跟随：实施

执行会话交接：原源码作者1f4155a4-e537-4518-a3f7-893f09c5daff在只读定位后继承ask模式，首次实施未写任何源码并已最终返回停止。现由新建可写会话23ef4660-da4b-47a8-9341-8ec236ec5cc3作为本单唯一源码作者继续，同为固定grok-4.7-high-fast。这是执行模式恢复，不是替换模型、复审或重置额度。已完成定位见liaoli-hit-seed-locate-cursor-final.md，只将其作输入，不追加独立复审；需要引用时定向读取真实代码完成实现。

固定 Cursor CLI --model grok-4.7-high-fast。你是本次唯一源码与运行时资源作者。此单是用户授权的实现与必要实现检查，不是独立复审；设计2/2、实现3/3承接，本阶段额度已用完，返修结果未追加复审。不派测试、不增加/运行测试任务、不重新申请Astra、不重审无关功能。默认不编译，完成后待指定统一编译者执行compileJava；不打包、不启动游戏、不提交，保留所有已有未提交改动。

先读现行AGENTS与统一流程。输入为本批liaoli-hit-seed-intake.json、最新liaoli-pose-transition-snapshot.json、liaoli-pose-intake.json及liaoli-hit-seed-art-approval.md。fire_liaoli已实际从待验收5迁至制作中6，59个唯一编号守恒；提交后表SHA ECE5C0FAFAF440A70481463AC790F0E03BF83B33433AFF2A2495E78108EF78D3，LastWriteTime 2026-10-30 01:29:38.543。表格作者已停止写入。不得使用旧test-stage-input或初版档案回滚共享火种。

用户明确批准范围：
1. 燎里现有三道后扩火焰环仅在有效命中、目标扣除前共享火种S严格>50时触发。空挥、未获伤害权限、S<=50均不播。读取应与实际成功扣种所使用的S一致，避免先清零再判定；不改变物理6、消耗全部当时火种、额外火伤0.36*S*(1+F)、每目标一次命中和其他既有玩法。保留rear_ring.png与三道同时扩大向后扩散的形态。按命中事件触发一个现有周期的三环效果，后续没有合格命中就不自动续播；多次合格目标命中可以重新触发，避免同一次命中因逐帧数据/重复监听无限重启。保持三环清晰可见，收尾不得影响伤害命中体寿命。
2. 所有带火种实体常驻显示floor(当前S/10)朵新火苗，现有cap100对应最多10朵。用户已确认二维图，详见下文。火苗小而分散围绕实体，跟随实体插值位置，不遮挡整个实体。显示基于当前余额，包括其他术式/武器施加的火种；扣除/变化/死亡/实体卸载/换世界时正确减少或消失。新跟踪者/迟加入玩家也能收到当前数量。客户端只读显示数据，不得变成火种权威来源或改变生产/消耗/伤害公式。用既有网络约定最小同步必要信息，校验实体标识避免实体ID复用串目标；避免每帧广播全部实体或全世界扫描。
3. 高速移动中燎里特效连续跟随：依据你上单定位，显式同步命中体所属施法者身份，客户端按该身份绑定玩家，用玩家每帧插值位置/眼睛和转角计算渲染；不要扩大0.75格猜测范围或在多人间串绑，不用滞后命中体的包位置代替已绑定玩家当前位置。保留用户确认v7剑阵几何、站立尖朝下/柄近尖远、横身移腿并稍前移，以及现有六张v6贴图、刺击剑/火流/后环相对朝向。尽可能将修复限于燎里渲染和通用可复用身份同步，不改其他命中体运动插值。确有必要的最小共享代码变化可做，并在报告中说明影响边界。

已确认新图：docs/资源库/特效/火焰/火种常驻火苗_v1/fire_seed_flame.png，SHA256 106DD7C62A1E360C859FB598B320EBCF10A40568555C763465023380E5DE800B，原尺寸32x256 RGBA，8帧每帧32x32，100ms/帧、全循环0.8秒。配套aseprite/json/gif已归档。2026-10-30用户回复“确认，使用这版火苗”。由你复制原图到适当assets/xueximod/textures/effect路径并接入引用；图集若手动UV选帧，不要再加重复动画导致二次切帧。不得改已确认PNG像素或其余v6纹理。美术作者已停止图像写入，主会话只在最终报告后维护资源README采用状态。

文件所有权：你唯一可写以下现有文件及新文件必要范围：
- client/LiaoliV6Renderer.java，visual/liaoli/LiaoliV6Layout.java，shu/technique/effects/LiaoliEffect.java。
- shu/technique/release/entity/TechniqueHitEntity.java：最小施法者身份与命中视觉数据支持；ClientTrack.java与release/motion/FollowCasterMotion.java仅在上述范围无法实现时才最小修改，不做通用插值重构。
- status/fire/seed/FireSeedState.java及status/AttributeStateData.java仅同步接线必要改动；不改账本/燃料算式。
- client/FireVisualClient.java、network/FireVisualPacket.java、network/ModNetwork.java，以及新增的火种状态同步/渲染或命中火环事件类，按现有注册/事件方式最小接线，不替换公共架构。
- src/main/resources/assets/xueximod/下新增火种火苗运行资源；如需确实相应语言文案，可仅修改燎里/火种说明，不改他项。
- 本批liaoli-hit-seed-implement-report.md、liaoli-hit-seed-implement-evidence.json、必要检查日志。
以上Java路径均以src/main/java/com/bslx/XueXiMod/为根。其他作者不写这些源码。若必须新增外部现有共享文件改动，先限定必要接线，不扩大改动；真实需玩法选择的阻塞交回。

禁止写制作表、记录.md、资源库索引、其他任务范围。做必要实现检查并执行powershell -NoProfile -File tools/check-name-gate.ps1，仅修本次文件命名问题；已知OriginalHumanoid.java的i/to历史问题不要扩修。不得启动Gradle。最后交回确切修改文件、最后写入时间与SHA256、所有相关写入停止声明、实际资源映射、触发阈值/余额变化/高速多人跟随的用户实测四行、完整需求与拟显示N/O。本单结尾写待统一编译、未游戏实测、本阶段额度已用完返修结果未追加复审；不得写成已新增审核通过。

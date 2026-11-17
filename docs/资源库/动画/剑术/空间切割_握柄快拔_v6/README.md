# 空间切割 · 握柄快拔 v6

用户游戏反馈授权修复版，已接入并于2026-11-02 22:42:01–22:42:39通过最终离线编译，待用户游戏复测。保留v5左腰右手拔斩姿态，改为真实刀柄握点与0.5秒动作。

- [原版铁剑动画](iron-grip-side.gif) / [正面](iron-grip-front.gif)
- [原姿态提速预览](iaido-fast-side.gif)
- [真实握点Blockbench模型](iron-sword-grip-check-v6.bbmodel) / [提速模型](iaido-grip-fast-v6.bbmodel)
- [交接参数](visual-handoff.md) / [原生读数](native-grip-proof.json)

GIF末尾0.5秒停顿仅用于观看，不是游戏施法时长。游戏仍使用实际剑和玩家皮肤。

## 游戏接入

真实握点由模型中心改到缠把中心(3.5,3.5,8)/16，刀轴按原版铁剑的(1,1,0)对角线处理；第一/第三人称共用。动作会话、clip、状态和九骨关键帧同步为0.5秒，0.15秒拔出、0.1875秒挥斩末态，保留原1秒斩击特效及伤害/冷却。

运行资源为 `src/main/resources/assets/xueximod/actions/space_cut_grip_fast_v6.json`。原v5资源保留。示意假人、示意剑和原版参考图不作为游戏人物或新武器加载。

- [运行映射](runtime-mapping.md)
- [最终编译报告](../../../../开发记录/空间切割拔刀动作接入-20261102/游戏反馈-握柄与提速/compile-feedback-report.md)
- [本次实测清单](../../../../开发记录/空间切割拔刀动作接入-20261102/游戏反馈-握柄与提速/用户实测清单.md)

compileJava实际执行、退出码0；1263个src文件前后哈希差异0。第3/3轮集中实现复核已通过；未打包、未启动游戏。自定义handheld变换或非标准刀柄布局仍需单独适配。

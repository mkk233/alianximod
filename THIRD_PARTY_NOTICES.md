# 第三方声明

本模组包含 Epic Fight 动作计算核心的移植代码。移植源码按 GNU GPLv3 分发。许可文本是根目录 `LICENSE`，与上游原文一致，没有改写。`gradle.properties` 的 `mod_license` 为 `GPL-3.0-only`，不包含 “or later”。

Forge、Forge Mod Loader 的 LGPL-2.1 说明仍在 `docs/LICENSE.txt`。那份文件不是本模组代码的许可证，也不被 GPLv3 替换。

## Epic Fight

- 版权：Epic Fight Team / Yesman。本仓库没有改写这一归属。
- 上游：https://github.com/Antikythera-Studios/epicfight
- 固定提交：`9d25f19c121c5e51b1db9efe01833a563706eeee`
- 版本：Minecraft 1.20.1，Epic Fight 20.14.17
- 许可证：GNU General Public License v3.0 only
- 上游根目录 `LICENSE` 是 GPLv3 原文。上游 `README` 与 `LICENSE-ASSETS` 把资源权利和代码许可分开。资产不因 GPL 代码许可而可以再分发，也不打进默认 jar 或源码包。
- 资源、模型和动画数据不是 GPL 代码。GPL 只覆盖移植代码。没有公共授权可以把这些资产打进 jar 或源码包，也不把它们写成可以再分发。用户书面本地授权涵盖剑、刀和翻滚动作、biped 骨架，以及玩家网格和蒙皮权重。这些素材只在 `dev-authorized/epicfight` 可选加载。其中动作数据是 117 条剪辑、118 个授权 ID 和 20 条骨骼元数据；35 条剪辑带 Coord 或 IK，属于降阶。贴图、shader、Epic 护甲和其他武器资源没有导入。纹理和护甲几何用原版。该目录继续排除在默认 jar 和源码分发清单之外，不重新贴成 GPL，也不写成 Epic Fight 作者给出了公开再分发许可。`import-manifest` 的阶段记录没有改写。

## 本批纳入的源码

包名 `com.bslx.XueXiMod.animation.core`。本地原创辅助类同样标 `GPL-3.0-only`，与这份移植兼容。它们不是 Epic Fight 的文件。

| 本地路径 | 上游路径 | 处理 |
| --- | --- | --- |
| `animation/core/Keyframe.java` | `api/animation/Keyframe.java` | 原样算法，拒绝非有限时间 |
| `animation/core/Joint.java` | `api/animation/Joint.java` | 原样层级与路径；绑定矩阵必须可逆；深度有上限 |
| `animation/core/JointTransform.java` | `api/animation/JointTransform.java` | 原样插值与矩阵合成 |
| `animation/core/Pose.java` | `api/animation/Pose.java` | 原样姿势混合 |
| `animation/core/TransformSheet.java` | `api/animation/TransformSheet.java` | 采样夹到首尾帧；时间必须有限；结果交副本；去掉 `LivingEntityPatch` |
| `animation/core/AnimationClip.java` | `api/animation/AnimationClip.java` | 烘焙与取样同一夹取；时间必须有限；单帧不除零；加入时深拷贝轨道 |
| `animation/core/model/Armature.java` | `api/model/Armature.java` | 原样父子矩阵；去掉日志和 JSON 导出 |
| `animation/core/math/OpenMatrix4f.java` | `api/utils/math/OpenMatrix4f.java` | 原样矩阵公式；去掉 Minecraft `Vec3` |
| `animation/core/math/Vec3f.java` | `api/utils/math/Vec3f.java`，并入 `Vec2f.java` | 原样向量算法 |
| `animation/core/math/Vec4f.java` | `api/utils/math/Vec4f.java` | `OpenMatrix4f.transform` 所需，不在最初清单里，但没有它变换算法不完整 |
| `animation/core/math/MatrixOperation.java` | `api/utils/math/MatrixOperation.java` | 原样函数接口 |
| `animation/core/math/AnimationTransformEntry.java` | `api/utils/math/AnimationTransformEntry.java` | 原样绑定顺序；`Pair` 改为本地记录 |
| `animation/core/math/MathUtils.java` | `api/utils/math/MathUtils.java` 约 312–317、349–363、425–465 行 | 只留向量插值、四元数插值和 `Vec3f` 夹角 |
| `animation/core/math/FiniteChecks.java` | 无 | 本地数值门禁 |
| `animation/core/SampleSearch.java` | 无 | 本地二分，避免非有限时间死循环 |

采样和烘焙不是上游原样。时间低于首帧取首帧，高于末帧取末帧；烘焙和未烘焙同一含义。负时间不再先加上末帧时间或剪辑时长。采样时间、关键帧时间和变换分量必须有限。`Keyframe` 不交出内部数组。变换、插值、`getJointTransform`、`getJointTransforms` 和 `getPoseInTime` 交回副本。剪辑在 `addJointTransform` 时复制轨道。外部改副本不会留下旧的烘焙缓存。

本地加载器在 `com.bslx.XueXiMod.animation.runtime.loader`，只读 `dev-authorized` 里的开发目录，不是上游加载器。水刃三拍没有可采样的开发姿态时用原创兜底，兜底不是 Epic Fight 动作。玩家网格和蒙皮权重属于同一份书面本地授权，也只从该目录可选加载。贴图、shader、Epic 护甲和其他武器资源没有导入。

没有移植 `AttackAnimation`、`ActionAnimation`，也没有移植伤害、击退位移结算或无敌帧。

## 构建入口

`jar` 会把 `LICENSE`、本文件、`docs/LICENSE.txt` 和 `docs/动作系统移植与扩展说明-2026-10-24.md` 放进 `META-INF`。`sourceDistributionZip` 按允许清单打包可构建源码，并排除 `dev-authorized`。这两项都已写进 `build.gradle`。主控已经打出 jar 和源码包，并做过解包再编译；条目数、警告、哈希和「游戏没有启动」见 [docs/动作构建验证说明-2026-10-24.md](docs/动作构建验证说明-2026-10-24.md)。这次文档同步会改本文件和移植说明的正文。Java 与 `build.gradle` 不改。主控会再打包比对。

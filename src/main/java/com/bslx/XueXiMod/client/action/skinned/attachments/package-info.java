/**
 * 原版装备与附着。几何来自原版立方体，权重和握点校正是本模组的，不复制未授权护甲网格、着色器或纹理。
 * <p>
 * 护甲在源空间蒙皮，绘制是 Pvanilla * A * H * sum(w * S * p)。S 只属于护甲顶点。
 * 手持、头、披风、鞘翅、箭和蜂刺用骨全局 G，不用 S。绘制局部是 A * sourceToView * G * K，
 * K = inverse(Gbind) * inverse(sourceToView) * Trest。Gbind 来自已发布骨架的 toOrigin，不写死授权矩阵。
 * <p>
 * 权重：头单骨 Head。躯干沿 Torso 到 Chest 平滑过渡。手臂沿 Arm 到 Hand 平滑过渡，
 * Elbow 只在投影到这段骨骼附近、宽度为该部件参数长度 0.2 的带内参与，峰值 0.35，不把 Elbow 串进骨干。
 * 腿对 Thigh、Leg、Knee 使用同一规则。权重非负且和为 1。开口不补面。
 * <p>
 * 变换链：护甲在 HumanoidArmorLayer.render 的四次 renderArmorPiece 上逐件 try/finally。
 * 染色、overlay、trim、foil 和 Forge 纹理仍走原方法，三个 Model.renderToBuffer 叶替换几何。
 * 手持 Redirect 两次 renderArmWithItem。未蒙皮时原方法一次；已蒙皮时用骨骼矩阵替换 translateToHand，
 * 随后仍做 -90°、180°、左右平移，并调用 ItemInHandRenderer.renderItem 一次。
 * 头饰只替换头部 translateAndRotate。披风和鞘翅在各自唯一的 0.125 平移之前乘一次 Chest。
 * 箭和蜂刺的选骨留在层内，LivingEntityRenderer 只包 StuckInBodyLayer 的一次 render。
 * 这些仿射走 AffinePose，同时右乘 pose 和法线逆转置。
 * <p>
 * 主路要登记的 mixin 类名见 {@link com.bslx.XueXiMod.client.action.skinned.attachments.SkinAttachmentBridge#MIXIN_CLASS_NAMES}。
 * 本包不修改 mixins.json。
 */
package com.bslx.XueXiMod.client.action.skinned.attachments;

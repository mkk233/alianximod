我先通读这份返修说明，再按里面写明的范围和约束执行。返修只动了渲染器里三处。我对照当前调用链，并用作者给出的 JOML jar 核对 API。前两项已闭合。第 301 行覆层是 `OverlayTexture.NO_OVERLAY`。`pose.translate(-camera)` 已去掉。第 298 行顶点只把世界点减去相机一次，再乘平移前复制的 `rotation`。中心是相机加上逆旋转后的偏移，提交时再减回相机，矩阵里没有第二段平移。UV 仍是 `0/1` 与 `frame/16` 到 `(frame+1)/16`，边长仍是 `hypot(宽×96/70, 高×96/74)×1.04`。

API 问题已闭合。已用作者给出的 `joml-1.10.5.jar` 做 `javap`：`Matrix4f.transformTranspose` 只有三个 `Vector4f` 重载，没有 `Vector3f`。`Matrix3f(Matrix4fc)` 只拷左上 3×3，`transpose()` 就地转置并返回自身，`transform(Vector3f)` 会写回同一向量。第 264 行 `new Matrix3f(rotation).transpose().transform(worldOffset)` 能对上这些签名，得到的是旋转逆。范围内没有新的实质问题。

这三处可以进入统一编译。本次没有编译，也没有进游戏。
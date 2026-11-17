我先读完整份返修说明，再按其中的范围和约束改。审查要求两处绘制修复，并要我用项目里的 JOML jar 核对 `transformTranspose`。我先定位那个 jar，再只改渲染器。JOML 1.10.5 的 `Matrix4f` 没有 `transformTranspose(Vector3f)`。我按审查结论改覆层、相机相对顶点和逆旋转调用。只改了 `PrisonShellRenderer.java`，贴图未动。

**改动**
- 覆层改为 `overlayCoords(OverlayTexture.NO_OVERLAY)`，与 `StillSheetPlayback` 同一材质先例一致，避免 `(0,0)` 红覆层染蓝。
- 去掉 `pose.translate(-camera)`。顶点先用 double 减去相机，再转 float，只乘平移前复制的 `rotation`。投影、正方形边长、聚能 `bounds` 和 UV 未改。
- `Matrix4f` 没有 `transformTranspose(Vector3f)`。逆旋转改为 `new Matrix3f(rotation).transpose().transform(worldOffset)`，只用 3×3 旋转。

**API 证据**  
jar：`C:\Users\baishang\.gradle\caches\modules-2\files-2.1\org.joml\joml\1.10.5\22566d58af70ad3d72308bab63b8339906deb649\joml-1.10.5.jar`  
`javap`：`Matrix4f.transformTranspose` 只有 `Vector4f`、`(Vector4fc, Vector4f)`、四个 float 加 `Vector4f`。`Matrix3f` 有 `Matrix3f(Matrix4fc)`、`transpose()`、`transform(Vector3f)`。

**门禁**  
`check-name-gate.ps1` 退出码 1。本文件无新问题。旧问题仍是 `OriginalHumanoid.java` 第 165 行 `i`、第 202 行 `to`，未改。

**剩余**  
未编译，未进游戏。贴墙 0.08 格、多牢不排序、并集不缩小、实体缺失不跟姿势仍在。

写入结束，待统一编译。
# 协同回旋斩击：编译可见性修正

针对 `compile-failed-01` 的唯一 javac 错误。没有重新编译，没有游戏验证，没有追加实现复审。确认关键帧、坐标、贴图和水幻形绘制顺序没有改。

## 错误

`CooperativePoseApplier.java` 第 4 行导入 `com.bslx.XueXiMod.client.action.MatrixFrames`。该类是包内 `final class`，子包 `client.action.cooperative` 不能访问。导入实际用在根偏航矩阵相乘，不是多余导入。

## 更改

`MatrixFrames.product` 在两个参数都不是空时，就是 `OpenMatrix4f.mul(outer, inner, null)`。这里的根偏航和骨骼局部旋转都由 `VanillaEuler.zyx` 新建，不会是空。

`CooperativePoseApplier.java` 第 70 行改为直接调用公开的 `OpenMatrix4f.mul`，并删除那行导入。`client/action/MatrixFrames.java` 没有改，空间切割仍用原来的包内方法。

## 自检与未跑

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 0。

未统一编译。未游戏验证。本阶段实现额度已用完，这次返修没有追加复审。

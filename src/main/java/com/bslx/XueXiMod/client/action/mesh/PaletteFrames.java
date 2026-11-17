package com.bslx.XueXiMod.client.action.mesh;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;

/**
 * 后批冻结的共享读取口。
 * 全局架 G 没有乘 inverse bind，物品和头、胸附着用它。
 * skin 矩阵只给绑定空间顶点，不能拿去摆物品。
 */
public interface PaletteFrames {
    int jointCount();

    OpenMatrix4f copyGlobalFrame(int jointId);

    OpenMatrix4f copyGlobalFrame(String jointName);

    OpenMatrix4f copySkinMatrix(int jointId);

    OpenMatrix4f copyToolRightFrame();

    OpenMatrix4f copyToolLeftFrame();

    OpenMatrix4f copyHandRightFrame();

    OpenMatrix4f copyHandLeftFrame();

    OpenMatrix4f copyHeadFrame();

    OpenMatrix4f copyChestFrame();
}

# 燎里剑方向预览

草稿，只用来看八柄背剑的朝向。不是正式资源，没有接进游戏。

先打开 `liaoli-hilt-near-tip-far-continuous.bbmodel`（Blockbench，free 模型）。这是连续剑身：每柄是深红剑柄、护手、扁平金刃和收尖，不再用断开的小方块。`liaoli-hilt-near-tip-far.bbmodel` 仍留着，不覆盖。

玩家面向 -Z，背后是 +Z。眼睛和胸口标记在 -Z 一侧，背脊在 +Z 一侧。

八柄剑的位置没变，左右镜像。柄在半径 7、z=5.5，尖在半径 18、z=17.5。从身体右侧看（从 +X 朝 -X）最容易分清柄近、尖远。

分组：`Player`、`BackFan`，以及 `sword_01_right_level` 到 `sword_08_left_level`。`right` 是面向 -Z 时玩家的右侧（-X）。

现有 `LiaoliV6Layout` 的扇角与这次相同，但深度相反（柄的深度更靠后）。这份预览按「柄靠近背、尖离背更远」摆，没有改代码。没有火焰、手持剑和火环。

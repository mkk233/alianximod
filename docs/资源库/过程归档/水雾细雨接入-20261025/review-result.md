深度遮挡这一项按当前源码通过：先画雾盒不会把盒内细雨从外面整层剔掉。时间、32 帧 UV、正方形分格、边缘裁剪、原雾盒几何、PRESSURE 分支、透明混合和分批也都对得上。没有另列实质缺陷。

## 先画雾盒不会挡住盒内雨

雾和雨都走 `DomainSheetMesh`，写掩码是 `COLOR_WRITE`，深度测试仍是 LEQUAL。

```27:36:src/main/java/com/bslx/XueXiMod/client/DomainSheetMesh.java
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(true));
```

Forge 1.20.1 里 `COLOR_WRITE` 是 `WriteMaskStateShard(true, false)`（`RenderStateShard.java` 第 150 行）。绘制前 `depthMask(false)`，画完恢复 `depthMask(true)`（同文件第 557–570 行）。它不会把雾面写进深度缓冲。

调度是雾批次画完并结束之后才取雨的缓冲：

```69:82:src/main/java/com/bslx/XueXiMod/client/DomainWorldRenderer.java
        if (fogConsumer != null) {
            buffers.endBatch(FOG_MESH);
        }
        boolean drewRain = false;
        for (DomainSightStore.Sight sight : DomainSightStore.all()) {
            if (sight.visual() != DomainVisual.VOLUME) {
                continue;
            }
            if (DomainMistRainRenderer.draw(sight, pose, camera, buffers, gameTime)) {
                drewRain = true;
            }
        }
```

雨幕在盒内：四周内收 0.08，完整面落在 28%、50%、72%（`DomainMistRainRenderer.java` 第 23–28、90–93 行）。六个雾面铺满 AABB。相机在盒外时，近侧雾面更近，但那一面没有留下深度。雨只和场景里已经写过的深度做 LEQUAL，不会被这层半透明雾壳拒绝。雨自己也是 `COLOR_WRITE`（第 21 行），雨丝之间靠 `sortOnUpload=true` 做混合顺序，不靠后写的深度去挡。

`implementation-result.md` 第 43 和 75 行仍写 `entityTranslucent` 会写深度、并把遮挡留成实测。那是过时描述。baseline 的雾盒确实是会写深度的 `entityTranslucent`；当前雾盒改成只写颜色，几何和顶点色没改。这个差别是雨能从外面看见的原因，不是漏改。

顶点色 alpha 仍是 70（`DomainWorldRenderer.java` 第 108 行）。原版 `rendertype_entity_translucent` 只在乘完后的 alpha 小于 0.1 时丢弃片元。70/255 约 0.275，丢弃与否都不写深度，所以不影响上面的结论。

## 其余核对

- 起止：`gameTime < visualOrigin` 或 `gameTime >= visualEnd` 不画（`DomainMistRainRenderer.java` 第 45–46 行）。`visualEnd` 仍是起点加持续时间，没有新计时。
- 帧：`floorMod(gameTime - visualOrigin + phase, 32)`，相位是 `floorMod(animSeed + (group + slot) * 5, 32)`（第 143–148 行）。48 格外只留槽位 1，中间那面相位不变。
- UV：`v = (frame + localV) / 32`，世界上方对应较小的 localV（第 110–111、151–152、131–134 行）。完整边收半个纹素 `0.5/64`，第 31 帧下沿停在本帧最后一个纹素中心，不会采到下一帧。
- 贴图：游戏副本和归档 `mist_rain_80.png` 都是 532257 字节，SHA-256 都是 `4d578c45469f3f9265ae254bd07a4be8aa09b36b17d5c6512604da2b544a66c4`，IHDR 64×2048、8 位 RGBA。整张 alpha 只有 0、46、99、168。46/255 约 0.180，三档都高于着色器的 0.1 丢弃线。mcmeta 只有 `blur: false`、`clamp: false`，没有动画段。渲染状态另把 blur 和 mipmap 都设为 false。
- 分格：宽高都用 `CELL_SIZE` 2。边缘格几何收到剩余长度，UV 用剩余/2，不拉满到 1（第 106–111 行）。完整格世界宽高相等，80° 斜向留在贴图里。
- 雾盒：六面坐标和 `(90, 170, 255, 70)` 与 baseline 一致，贴图仍是 `water_still`。非 PRESSURE 才画盒；雨只在 `VOLUME`。
- PRESSURE：仍跳过雾盒，仍在雨批次结束之后调用 `DomainPressureRenderer.draw` / `end`（第 84–91 行）。该类源码不在本次改动里，仍用原来的 `entityTranslucent`。
- 批次：雾、雨各自 `getBuffer` 后 `endBatch(具体 RenderType)`。没有无参 `endBatch`。雨的 `VertexConsumer` 不跨到下一次 `getBuffer`。混合是 `TRANSLUCENT_TRANSPARENCY`（源 alpha、一减源 alpha），无剔除，每个格子一个四边形。

侧面法线 `(0,0,1)` 和 `(1,0,0)` 会进这个实体着色器的漫反射，明暗可以和雾盒的向上法线不一样，alpha 不参与这次缩放，也不产生深度剔除。不列成缺陷。

未编译，未启动游戏。

package com.bslx.XueXiMod.status.wood.resource;

import java.util.UUID;

/** 地面片段的当前身份。修订号不匹配则整次操作拒绝。 */
public record FragmentHandle(UUID fragmentId, int revision) {
}

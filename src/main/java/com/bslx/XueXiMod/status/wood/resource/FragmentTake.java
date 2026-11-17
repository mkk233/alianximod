package com.bslx.XueXiMod.status.wood.resource;

import java.util.UUID;

/**
 * 一次消费申请。units 是有效数量单位，从该片段区间的低端往前取。
 * 修订号必须是查询或上一次收据里的当前值。
 */
public record FragmentTake(UUID fragmentId, int revision, int units) {
}

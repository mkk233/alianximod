/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Local original helper for the Epic Fight action-core port.
 * Copyright (C) 2026 XueXi Mod contributors.
 * Modified: 2026-10-24
 *
 * 与 GPLv3 兼容。保留上游二分找帧的区间收缩，遇到非有限时间或区间不推进时直接失败，
 * 避免 NaN 比较让循环停不下来。单帧不除以零，进度取 0，采样结果保持该帧。
 */
package com.bslx.XueXiMod.animation.core;

public final class SampleSearch {
    private SampleSearch() {
    }

    @FunctionalInterface
    public interface TimeColumn {
        float timeAt(int columnIndex);
    }

    public record Result(int begin, int end, float progression) {
    }

    public static Result bracket(int size, TimeColumn column, float currentTime) {
        if (column == null) {
            throw new IllegalArgumentException("sample column is missing");
        }
        if (!Float.isFinite(currentTime)) {
            throw new IllegalArgumentException("sample time must be finite");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("sample column is empty");
        }
        if (size == 1) {
            float onlyTime = column.timeAt(0);
            if (!Float.isFinite(onlyTime)) {
                throw new IllegalArgumentException("sample time column must be finite");
            }
            return new Result(0, 0, 0.0F);
        }

        int begin = 0;
        int end = size - 1;
        int steps = 0;
        while (end - begin > 1) {
            if (steps > size) {
                throw new IllegalStateException("sample search did not converge");
            }
            steps++;
            int middle = begin + (end - begin) / 2;
            float middleTime = column.timeAt(middle);
            float nextTime = column.timeAt(middle + 1);
            if (!Float.isFinite(middleTime) || !Float.isFinite(nextTime)) {
                throw new IllegalArgumentException("sample time column must be finite");
            }
            if (middleTime <= currentTime && nextTime > currentTime) {
                begin = middle;
                end = middle + 1;
                break;
            }
            if (middleTime > currentTime) {
                if (end == middle) {
                    throw new IllegalStateException("sample search did not converge");
                }
                end = middle;
            } else if (nextTime <= currentTime) {
                if (begin == middle) {
                    throw new IllegalStateException("sample search did not converge");
                }
                begin = middle;
            } else {
                throw new IllegalStateException("sample times are not strictly increasing");
            }
        }

        float startTime = column.timeAt(begin);
        float endTime = column.timeAt(end);
        if (!Float.isFinite(startTime) || !Float.isFinite(endTime)) {
            throw new IllegalArgumentException("sample time column must be finite");
        }
        float span = endTime - startTime;
        float progression = 0.0F;
        if (span > 0.0F && Float.isFinite(span)) {
            progression = (currentTime - startTime) / span;
            if (!Float.isFinite(progression) || progression < 0.0F) {
                progression = 0.0F;
            } else if (progression > 1.0F) {
                progression = 1.0F;
            }
        }
        return new Result(begin, end, progression);
    }
}

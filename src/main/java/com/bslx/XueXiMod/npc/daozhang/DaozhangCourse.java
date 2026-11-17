package com.bslx.XueXiMod.npc.daozhang;

import com.bslx.XueXiMod.player.LearningGraph;
import com.bslx.XueXiMod.shu.ShuAttribute;

/** 服务端生成的一门课程。客户端只显示，不据此扣材料。 */
public record DaozhangCourse(
        String techniqueId,
        String name,
        Kind kind,
        LearningGraph.Stage stage,
        String parents,
        boolean parentsSatisfied,
        boolean learned,
        boolean canStudy,
        boolean unlocked,
        ShuAttribute embryo,
        String slipName) {

    public enum Kind {
        SWORD,
        SPELL
    }
}

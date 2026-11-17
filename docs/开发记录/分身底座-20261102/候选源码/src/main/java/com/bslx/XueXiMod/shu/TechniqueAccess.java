package com.bslx.XueXiMod.shu;

import com.bslx.XueXiMod.skill.Skill;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家入口只看注册时的能力。未登记段保持原样，不在这里删掉已登记段。
 */
public final class TechniqueAccess {
    public static final String DEVELOPMENT_ENTRY_ONLY = "DEVELOPMENT_ENTRY_ONLY";
    public static final String DENIAL_TEXT = "该术式只属于开发入口";

    private TechniqueAccess() {
    }

    public static boolean allowsPlayerUse(Technique technique) {
        return technique != null && technique.availability() != TechniqueAvailability.DEVELOPMENT_ONLY;
    }

    public static List<Technique> playerCatalog() {
        List<Technique> catalog = new ArrayList<>();
        for (Technique technique : TechniqueManager.getAll()) {
            if (allowsPlayerUse(technique)) {
                catalog.add(technique);
            }
        }
        return catalog;
    }

    public static boolean skillAllowed(Skill skill) {
        if (skill == null || skill.getTechniques() == null) {
            return true;
        }
        for (Technique technique : skill.getTechniques()) {
            if (technique == null || technique.getId() == null) {
                continue;
            }
            Technique registered = TechniqueManager.getById(technique.getId());
            if (registered != null && registered.availability() == TechniqueAvailability.DEVELOPMENT_ONLY) {
                return false;
            }
        }
        return true;
    }
}

package com.fluidcombat.combat;

import java.util.List;

public record Moveset(List<AttackMove> combo, AttackMove heavy, AttackMove dash) {
    public AttackMove light(int index) {
        return combo.get(Math.floorMod(index, combo.size()));
    }

    public AttackMove get(AttackType type, int comboIndex) {
        return switch (type) {
            case LIGHT -> light(comboIndex);
            case HEAVY -> heavy;
            case DASH -> dash;
        };
    }
}

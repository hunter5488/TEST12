package com.fluidcombat.combat;

public enum AttackType {
    /** Plain attack input; advances the combo chain. */
    LIGHT,
    /** Sneak + attack. Slow, hard hitting, staggers and breaks guards. */
    HEAVY,
    /** Sprint + attack. Lunging gap closer. */
    DASH;

    public static AttackType byId(int id) {
        AttackType[] v = values();
        return id >= 0 && id < v.length ? v[id] : LIGHT;
    }
}

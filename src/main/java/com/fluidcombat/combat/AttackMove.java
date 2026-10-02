package com.fluidcombat.combat;

/**
 * Timing and hit data of a single attack. All times are in ticks at 1x attack speed.
 *
 * @param id          unique id, also the id of the {@link com.fluidcombat.anim.Animation} that visualises it
 * @param windup      ticks before the hit becomes active
 * @param active      ticks during which the swing sweeps from {@code arcFrom} to {@code arcTo}
 * @param recovery    ticks after the active frames
 * @param chainAt     time from which the next move of the combo may start (buffered inputs fire here)
 * @param damage      multiplier applied to the attacker's attack damage
 * @param reach       multiplier applied to the attacker's reach
 * @param arcFrom     sweep start angle in degrees relative to facing (positive = attacker's right)
 * @param arcTo       sweep end angle in degrees
 * @param lunge       forward impulse applied when the active frames begin
 * @param stamina     stamina cost
 * @param knockback   extra knockback strength
 * @param stagger     ticks the victim is staggered on hit (0 = flinch only)
 * @param guardBreak  breaks guards instead of being blocked
 * @param hyperArmor  the attacker cannot be interrupted during the windup
 * @param slash       draws sweep particles on hit (false for thrusts / smashes)
 */
public record AttackMove(
        String id,
        int windup,
        int active,
        int recovery,
        int chainAt,
        float damage,
        float reach,
        float arcFrom,
        float arcTo,
        float lunge,
        float stamina,
        float knockback,
        int stagger,
        boolean guardBreak,
        boolean hyperArmor,
        boolean slash
) {
    public int length() {
        return windup + active + recovery;
    }

    public int activeEnd() {
        return windup + active;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private int windup = 5, active = 3, recovery = 8, chainAt = -1, stagger;
        private float damage = 1, reach = 1, arcFrom = 60, arcTo = -60, lunge = 0.2f, stamina = 12, knockback;
        private boolean guardBreak, hyperArmor, slash = true;

        private Builder(String id) {
            this.id = id;
        }

        public Builder timing(int windup, int active, int recovery) {
            this.windup = windup;
            this.active = active;
            this.recovery = recovery;
            return this;
        }

        /** Ticks after the active frames end at which the next combo move may begin. */
        public Builder chainDelay(int ticksAfterActive) {
            this.chainAt = ticksAfterActive;
            return this;
        }

        public Builder damage(float damage) {
            this.damage = damage;
            return this;
        }

        public Builder reach(float reach) {
            this.reach = reach;
            return this;
        }

        public Builder arc(float from, float to) {
            this.arcFrom = from;
            this.arcTo = to;
            return this;
        }

        public Builder lunge(float lunge) {
            this.lunge = lunge;
            return this;
        }

        public Builder stamina(float stamina) {
            this.stamina = stamina;
            return this;
        }

        public Builder knockback(float knockback) {
            this.knockback = knockback;
            return this;
        }

        public Builder stagger(int ticks) {
            this.stagger = ticks;
            return this;
        }

        public Builder guardBreak() {
            this.guardBreak = true;
            return this;
        }

        public Builder hyperArmor() {
            this.hyperArmor = true;
            return this;
        }

        public Builder thrust() {
            this.slash = false;
            return this;
        }

        public AttackMove build() {
            int chain = windup + active + (chainAt < 0 ? Math.max(2, recovery / 2) : chainAt);
            return new AttackMove(id, windup, active, recovery, Math.min(chain, windup + active + recovery),
                    damage, reach, arcFrom, arcTo, lunge, stamina, knockback, stagger, guardBreak, hyperArmor, slash);
        }
    }
}

package com.fluidcombat.anim;

/** Easing curves used for keyframe interpolation. Input and output are in [0, 1] (output may overshoot). */
public enum Easing {
    LINEAR {
        @Override public float apply(float t) { return t; }
    },
    IN_QUAD {
        @Override public float apply(float t) { return t * t; }
    },
    OUT_QUAD {
        @Override public float apply(float t) { return 1 - (1 - t) * (1 - t); }
    },
    IN_OUT_QUAD {
        @Override public float apply(float t) { return t < 0.5f ? 2 * t * t : 1 - (float) Math.pow(-2 * t + 2, 2) / 2; }
    },
    IN_CUBIC {
        @Override public float apply(float t) { return t * t * t; }
    },
    OUT_CUBIC {
        @Override public float apply(float t) { float u = 1 - t; return 1 - u * u * u; }
    },
    IN_OUT_CUBIC {
        @Override public float apply(float t) { return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2; }
    },
    OUT_QUART {
        @Override public float apply(float t) { float u = 1 - t; return 1 - u * u * u * u; }
    },
    IN_EXPO {
        @Override public float apply(float t) { return t <= 0 ? 0 : (float) Math.pow(2, 10 * t - 10); }
    },
    OUT_EXPO {
        @Override public float apply(float t) { return t >= 1 ? 1 : 1 - (float) Math.pow(2, -10 * t); }
    },
    /** Slight overshoot past the target, then settle. Great for snappy strike follow-throughs. */
    OUT_BACK {
        @Override public float apply(float t) {
            float c1 = 1.70158f, c3 = c1 + 1, u = t - 1;
            return 1 + c3 * u * u * u + c1 * u * u;
        }
    },
    /** Pull back slightly before moving. Good for anticipation in wind-ups. */
    IN_BACK {
        @Override public float apply(float t) {
            float c1 = 1.70158f, c3 = c1 + 1;
            return c3 * t * t * t - c1 * t * t;
        }
    },
    OUT_ELASTIC {
        @Override public float apply(float t) {
            if (t <= 0) return 0;
            if (t >= 1) return 1;
            double c4 = (2 * Math.PI) / 3;
            return (float) (Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * c4) + 1);
        }
    },
    /** Smooth hermite step. */
    SMOOTH {
        @Override public float apply(float t) { return t * t * (3 - 2 * t); }
    };

    public abstract float apply(float t);
}

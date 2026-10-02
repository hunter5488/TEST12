package com.fluidcombat.anim;

/**
 * Reusable first-person hand curves. First-person motion is kept mostly in the screen plane: the hand travels
 * across the view while the blade rolls through the cut, so the weapon never swings out of sight.
 * <p>
 * Times: {@code w} = end of windup, {@code a} = end of the active frames, {@code len} = animation length.
 */
public record FirstPersonCurves(String rot, String pos) {
    public void applyTo(Animation.Builder b) {
        b.track(Bone.FP_ROT, rot).track(Bone.FP_POS, pos);
    }

    private static String f(float v) {
        return Float.toString(v);
    }

    /** Horizontal cut from the upper right through the centre to the left. */
    public static FirstPersonCurves cutLeft(float w, float a, float len) {
        String mid = f((w + a) / 2), end = f(a), settle = f(Math.min(a + 3, len - 1));
        return new FirstPersonCurves(
                "0:0 | " + w + ":0,40,-10 OC | " + mid + ":0,15,45 IQ | " + end + ":-5,0,90 O4 | " + settle + ":-5,0,85 S | " + len + ":0",
                "0:0 | " + w + ":0.05,0.12,0.05 OC | " + mid + ":-0.1,0.1,-0.05 IQ | " + end + ":-0.2,0.15,-0.05 O4 | " + settle + ":-0.18,0.12,0 S | " + len + ":0");
    }

    /** Rising backhand from the lower left to the upper right. */
    public static FirstPersonCurves cutRight(float w, float a, float len) {
        String mid = f((w + a) / 2), end = f(a), settle = f(Math.min(a + 3, len - 1));
        return new FirstPersonCurves(
                "0:0 | " + w + ":0,0,90 OC | " + mid + ":0,15,45 IQ | " + end + ":5,45,-10 O4 | " + settle + ":5,40,-8 S | " + len + ":0",
                "0:0 | " + w + ":-0.2,0.15,0 OC | " + mid + ":-0.05,0.1,-0.05 IQ | " + end + ":0.05,0.15,-0.05 O4 | " + settle + ":0.04,0.12,0 S | " + len + ":0");
    }

    /** Raised high, then brought straight down through the centre. */
    public static FirstPersonCurves overhead(float w, float a, float len) {
        String mid = f((w + a) / 2), end = f(a), settle = f(Math.min(a + 3, len - 1));
        return new FirstPersonCurves(
                "0:0 | " + w + ":35,0,15 OC | " + mid + ":0,0,15 IC | " + end + ":-55,0,20 O4 | " + settle + ":-50,0,18 S | " + len + ":0",
                "0:0 | " + w + ":-0.15,0.3,0.12 OC | " + mid + ":-0.18,0.05,0 IC | " + end + ":-0.2,-0.35,-0.12 O4 | " + settle + ":-0.2,-0.32,-0.1 S | " + len + ":0");
    }

    /** Pulled back, then driven forward into the centre of the screen. */
    public static FirstPersonCurves thrust(float w, float a, float len) {
        String hit = f(Math.min(w + 1.5f, a)), hold = f(Math.min(a + 3, len - 1));
        return new FirstPersonCurves(
                "0:0 | " + w + ":-20,5,10 OC | " + hit + ":-45,10,20 OE | " + hold + ":-42,10,18 S | " + len + ":0",
                "0:0 | " + w + ":0.05,-0.08,0.2 OC | " + hit + ":-0.3,0.1,-0.35 OE | " + hold + ":-0.28,0.1,-0.3 S | " + len + ":0");
    }
}

package com.fluidcombat.anim;

import static com.fluidcombat.anim.Bone.BODY;
import static com.fluidcombat.anim.Bone.FP_POS;
import static com.fluidcombat.anim.Bone.FP_ROT;
import static com.fluidcombat.anim.Bone.HEAD;
import static com.fluidcombat.anim.Bone.LEFT_ARM;
import static com.fluidcombat.anim.Bone.LEFT_LEG;
import static com.fluidcombat.anim.Bone.RIGHT_ARM;
import static com.fluidcombat.anim.Bone.RIGHT_LEG;
import static com.fluidcombat.anim.Bone.ROOT;
import static com.fluidcombat.anim.Bone.ROOT_POS;

import com.fluidcombat.combat.WeaponCategory;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * The built-in animation library. Attack animations share their id with the {@link com.fluidcombat.combat.AttackMove}
 * they visualise and their timeline lines up with its windup / active / recovery phases.
 * <p>
 * Track spec format: {@code "time:x,y,z EASE | ..."} where EASE is the easing used to arrive at that key.
 * Arms are (pitch, yaw, twist); see {@link Bone}.
 */
public final class Animations {
    private static final Map<String, Animation> ANIMATIONS = new HashMap<>();

    public static final String DODGE_BACK = "dodge_back";
    public static final String DODGE_LEFT = "dodge_left";
    public static final String DODGE_RIGHT = "dodge_right";
    public static final String ROLL_FORWARD = "roll_forward";
    public static final String FLINCH = "flinch";
    public static final String STAGGER = "stagger";
    public static final String GUARD_HIT = "guard_hit";
    public static final String GUARD_BREAK = "guard_break";
    public static final String PARRY = "parry";

    private Animations() {
    }

    @Nullable
    public static Animation get(String id) {
        return ANIMATIONS.get(id);
    }

    public static Animation guardFor(WeaponCategory category) {
        return ANIMATIONS.get("guard_" + category.name().toLowerCase(Locale.ROOT));
    }

    public static void register(Animation animation) {
        ANIMATIONS.put(animation.id(), animation);
    }

    private static Animation.Builder a(String id, float length) {
        return Animation.builder(id, length);
    }

    static {
        // ------------------------------------------------------------------ SWORD
        // Diagonal forehand: cocked high on the right, cuts across to the lower left.
        register(a("sword_slash_1", 15)
                .track(RIGHT_ARM, "0:-40,20,0 | 4:-115,105,-30 OC | 5.5:-80,25,0 IQ | 7:-50,-75,30 O4 | 10:-45,-65,25 S | 15:-25,-10,0")
                .track(LEFT_ARM, "0:-20,0,0 | 4:-45,-30,0 OC | 7:15,10,0 O4 | 15:-10,0,0")
                .track(BODY, "0:0 | 4:0,35,0 OC | 5.5:0,5,0 IQ | 7:0,-35,0 O4 | 10:0,-30,0 S | 15:0")
                .track(ROOT, "0:0 | 4:-3,12,0 OC | 7:8,-12,0 O4 | 15:0")
                .track(ROOT_POS, "0:0 | 4:0,0.5,1 OC | 7:0,1.2,-1.5 O4 | 15:0")
                .track(RIGHT_LEG, "0:0 | 4:10,0,0 | 7:-25,0,0 O4 | 15:0")
                .track(LEFT_LEG, "0:0 | 4:-10,0,0 | 7:18,0,0 O4 | 15:0")
                .track(HEAD, "0:0 | 4:0,-12,0 | 7:0,12,0 | 15:0")
                
                .apply(fp -> FirstPersonCurves.cutLeft(4, 7, 15).applyTo(fp)).build());

        // Rising backhand from the lower left to the upper right.
        register(a("sword_slash_2", 14)
                .track(RIGHT_ARM, "0:-50,-75,30 | 3:-40,-95,60 OC | 4.5:-90,0,20 IQ | 6:-130,85,-20 O4 | 9:-120,75,-15 S | 14:-30,10,0")
                .track(LEFT_ARM, "0:15,10,0 | 3:10,10,0 | 6:-40,-30,0 O4 | 14:-10,0,0")
                .track(BODY, "0:0,-30,0 | 3:0,-40,0 OC | 4.5:0 IQ | 6:0,40,0 O4 | 9:0,35,0 S | 14:0")
                .track(ROOT, "0:5,-10,0 | 3:5,-14,0 | 6:-4,14,0 O4 | 14:0")
                .track(ROOT_POS, "0:0,1,-1 | 3:0,1.5,0 | 6:0,-0.5,-1.5 O4 | 14:0")
                .track(RIGHT_LEG, "0:-15,0,0 | 3:-15,0,0 | 6:15,0,0 O4 | 14:0")
                .track(LEFT_LEG, "0:10,0,0 | 3:10,0,0 | 6:-20,0,0 O4 | 14:0")
                .track(HEAD, "0:0,10,0 | 3:0,14,0 | 6:0,-14,0 | 14:0")
                
                .apply(fp -> FirstPersonCurves.cutRight(3, 6, 14).applyTo(fp)).build());

        // Overhead finisher: rears back with both hands and cleaves straight down.
        register(a("sword_slash_3", 20)
                .track(RIGHT_ARM, "0:-60,20,0 | 6:-175,15,0 OC | 7.5:-90,5,0 IC | 9:-15,0,0 O4 | 12:-20,0,0 S | 20:-25,5,0")
                .track(LEFT_ARM, "0:-20,0,0 | 6:-165,-15,0 OC | 7.5:-90,15,0 IC | 9:-25,20,0 O4 | 20:-10,0,0")
                .track(BODY, "0:0 | 6:-6,0,0 OC | 9:10,0,0 O4 | 12:8,0,0 | 20:0")
                .track(ROOT, "0:0 | 6:-8,0,0 OC | 9:14,0,0 O4 | 12:12,0,0 | 20:0")
                .track(ROOT_POS, "0:0 | 6:0,-0.5,1 OC | 9:0,2,-2 O4 | 12:0,2,-2 | 20:0")
                .track(RIGHT_LEG, "0:0 | 6:5,0,0 | 9:-30,0,0 O4 | 12:-30,0,0 | 20:0")
                .track(LEFT_LEG, "0:0 | 6:-5,0,0 | 9:20,0,0 O4 | 12:20,0,0 | 20:0")
                .track(HEAD, "0:0 | 6:6,0,0 | 9:-12,0,0 | 20:0")
                
                .apply(fp -> FirstPersonCurves.overhead(6, 9, 20).applyTo(fp)).build());

        // Heavy: crouching 360 degree spin slash.
        register(a("sword_heavy", 26)
                .track(RIGHT_ARM, "0:-40,20,0 | 9:-75,95,-50 OC | 14:-80,90,-60 | 18:-75,80,-40 | 26:-25,5,0")
                .track(LEFT_ARM, "0:-20,0,0 | 9:-60,-80,0 OC | 14:-60,-80,0 | 18:-50,-60,0 | 26:-10,0,0")
                .track(ROOT, "0:0 | 9:5,85,0 OC | 14:0,-275,0 OQ | 19:0,-350,0 OC | 26:0,-360,0")
                .track(ROOT_POS, "0:0 | 9:0,3,0 OC | 14:0,1.5,0 | 19:0,1,0 | 26:0")
                .track(RIGHT_LEG, "0:0 | 9:-25,0,0 OC | 19:-20,0,0 | 26:0")
                .track(LEFT_LEG, "0:0 | 9:20,0,0 OC | 19:15,0,0 | 26:0")
                
                .apply(fp -> FirstPersonCurves.cutLeft(9, 14, 26).applyTo(fp)).build());

        register(thrust("sword_dash", 3, 7, 17, 1f));

        // ------------------------------------------------------------------ AXE
        register(a("axe_chop_1", 19)
                .track(RIGHT_ARM, "0:-40,15,0 | 7:-165,45,0 OC | 8.5:-100,10,0 IC | 10:-20,-35,0 O4 | 13:-25,-30,0 S | 19:-25,5,0")
                .track(LEFT_ARM, "0:-20,0,0 | 7:-60,-30,0 OC | 10:20,0,0 O4 | 19:-10,0,0")
                .track(BODY, "0:0 | 7:0,30,0 OC | 10:0,-25,0 O4 | 13:0,-20,0 | 19:0")
                .track(ROOT, "0:0 | 7:-6,10,0 OC | 10:14,-8,0 O4 | 13:12,-8,0 | 19:0")
                .track(ROOT_POS, "0:0 | 7:0,-0.5,1 OC | 10:0,2,-1.5 O4 | 13:0,2,-1.5 | 19:0")
                .track(RIGHT_LEG, "0:0 | 10:-25,0,0 O4 | 13:-25,0,0 | 19:0")
                .track(LEFT_LEG, "0:0 | 10:18,0,0 O4 | 13:18,0,0 | 19:0")
                .track(HEAD, "0:0 | 7:6,-10,0 | 10:-12,8,0 | 19:0")
                
                .apply(fp -> FirstPersonCurves.overhead(7, 10, 19).applyTo(fp)).build());

        register(a("axe_sweep_2", 20)
                .track(RIGHT_ARM, "0:-20,-35,0 | 6:-60,-100,40 OC | 8:-70,0,20 IC | 10:-65,95,-20 O4 | 13:-60,85,-15 S | 20:-25,5,0")
                .track(LEFT_ARM, "0:20,0,0 | 6:-30,30,0 OC | 10:-50,-40,0 O4 | 20:-10,0,0")
                .track(BODY, "0:0 | 6:0,-40,0 OC | 8:0 IC | 10:0,40,0 O4 | 13:0,35,0 S | 20:0")
                .track(ROOT, "0:0 | 6:4,-15,0 OC | 10:4,15,0 O4 | 20:0")
                .track(ROOT_POS, "0:0 | 6:0,1.5,0.5 OC | 10:0,1,-1.5 O4 | 20:0")
                .track(RIGHT_LEG, "0:0 | 6:10,0,0 | 10:-15,0,0 O4 | 20:0")
                .track(LEFT_LEG, "0:0 | 6:-10,0,0 | 10:15,0,0 O4 | 20:0")
                .track(HEAD, "0:0 | 6:0,15,0 | 10:0,-15,0 | 20:0")
                
                .apply(fp -> FirstPersonCurves.cutRight(6, 10, 20).applyTo(fp)).build());

        // Heavy: two handed overhead slam.
        register(a("axe_heavy", 30)
                .track(RIGHT_ARM, "0:-40,10,0 | 10:-185,-10,0 OC | 13:-190,-10,0 | 14.5:-100,-10,0 IC | 16:-10,-15,0 O4 | 22:-15,-15,0 | 30:-25,5,0")
                .track(LEFT_ARM, "0:-20,0,0 | 10:-185,15,0 OC | 13:-190,15,0 | 14.5:-100,15,0 IC | 16:-15,25,0 O4 | 22:-20,25,0 | 30:-10,0,0")
                .track(ROOT, "0:0 | 10:-12,0,0 OC | 13:-14,0,0 | 16:22,0,0 O4 | 22:20,0,0 | 30:0")
                .track(ROOT_POS, "0:0 | 10:0,-1,1.5 OC | 13:0,-1.2,1.5 | 16:0,3.5,-2 O4 | 22:0,3.5,-2 | 30:0")
                .track(RIGHT_LEG, "0:0 | 10:5,0,0 | 16:-30,0,0 O4 | 22:-30,0,0 | 30:0")
                .track(LEFT_LEG, "0:0 | 10:-5,0,0 | 16:25,0,0 O4 | 22:25,0,0 | 30:0")
                .track(HEAD, "0:0 | 10:10,0,0 | 16:-18,0,0 | 22:-16,0,0 | 30:0")
                
                .apply(fp -> FirstPersonCurves.overhead(13, 16, 30).applyTo(fp)).build());

        // Dash: leaping overhead chop.
        register(a("axe_dash", 20)
                .track(RIGHT_ARM, "0:-40,10,0 | 5:-175,20,0 OC | 6.5:-100,5,0 IC | 8:-15,-10,0 O4 | 12:-20,-10,0 S | 20:-25,5,0")
                .track(LEFT_ARM, "0:-20,0,0 | 5:-120,-30,0 OC | 8:25,10,0 O4 | 20:-10,0,0")
                .track(ROOT, "0:0 | 5:-10,0,0 OC | 8:20,0,0 O4 | 12:18,0,0 | 20:0")
                .track(ROOT_POS, "0:0 | 5:0,-1.5,0 OC | 8:0,3,-2.5 O4 | 12:0,3,-2 | 20:0")
                .track(RIGHT_LEG, "0:0 | 5:-30,0,0 OC | 8:-30,0,0 | 12:-25,0,0 | 20:0")
                .track(LEFT_LEG, "0:0 | 5:20,0,0 OC | 8:25,0,0 | 12:20,0,0 | 20:0")
                .track(HEAD, "0:0 | 5:8,0,0 | 8:-15,0,0 | 20:0")
                
                .apply(fp -> FirstPersonCurves.overhead(5, 8, 20).applyTo(fp)).build());

        // ------------------------------------------------------------------ HEAVY (mace)
        register(a("mace_smash_1", 22)
                .track(RIGHT_ARM, "0:-40,10,0 | 8:-170,10,0 OC | 9.5:-100,5,0 IC | 11:-10,0,0 O4 | 15:-15,0,0 S | 22:-25,5,0")
                .track(LEFT_ARM, "0:-20,0,0 | 8:-50,-35,0 OC | 11:25,10,0 O4 | 22:-10,0,0")
                .track(BODY, "0:0 | 8:-6,15,0 OC | 11:8,-10,0 O4 | 15:6,-8,0 | 22:0")
                .track(ROOT, "0:0 | 8:-8,8,0 OC | 11:16,-5,0 O4 | 15:15,-5,0 | 22:0")
                .track(ROOT_POS, "0:0 | 8:0,-0.5,1 OC | 11:0,2.5,-1.5 O4 | 15:0,2.5,-1.5 | 22:0")
                .track(RIGHT_LEG, "0:0 | 11:-25,0,0 O4 | 15:-25,0,0 | 22:0")
                .track(LEFT_LEG, "0:0 | 11:20,0,0 O4 | 15:20,0,0 | 22:0")
                .track(HEAD, "0:0 | 8:8,-8,0 | 11:-15,5,0 | 22:0")
                
                .apply(fp -> FirstPersonCurves.overhead(8, 11, 22).applyTo(fp)).build());

        register(a("mace_swing_2", 24)
                .track(RIGHT_ARM, "0:-15,0,0 | 8:-70,110,-30 OC | 10:-65,10,0 IC | 12:-55,-85,30 O4 | 16:-50,-75,25 S | 24:-25,5,0")
                .track(LEFT_ARM, "0:-10,0,0 | 8:-40,-25,0 OC | 12:20,10,0 O4 | 24:-10,0,0")
                .track(BODY, "0:0 | 8:0,40,0 OC | 10:0,5,0 IC | 12:0,-40,0 O4 | 16:0,-35,0 S | 24:0")
                .track(ROOT, "0:0 | 8:-3,15,0 OC | 12:8,-15,0 O4 | 24:0")
                .track(ROOT_POS, "0:0 | 8:0,1,1 OC | 12:0,1.5,-1.5 O4 | 24:0")
                .track(RIGHT_LEG, "0:0 | 8:10,0,0 | 12:-25,0,0 O4 | 24:0")
                .track(LEFT_LEG, "0:0 | 8:-10,0,0 | 12:18,0,0 O4 | 24:0")
                .track(HEAD, "0:0 | 8:0,-15,0 | 12:0,15,0 | 24:0")
                
                .apply(fp -> FirstPersonCurves.cutLeft(8, 12, 24).applyTo(fp)).build());

        // Heavy: crouch, rise, and slam down with everything.
        register(a("mace_heavy", 33)
                .track(RIGHT_ARM, "0:-40,10,0 | 11:-190,0,0 OC | 14:-195,0,0 | 15.5:-100,0,0 IC | 17:-5,0,0 O4 | 24:-10,0,0 | 33:-25,5,0")
                .track(LEFT_ARM, "0:-20,0,0 | 11:-190,10,0 OC | 14:-195,10,0 | 15.5:-100,15,0 IC | 17:-10,20,0 O4 | 24:-15,20,0 | 33:-10,0,0")
                .track(ROOT, "0:0 | 11:-15,0,0 OC | 14:-16,0,0 | 17:25,0,0 O4 | 24:22,0,0 | 33:0")
                .track(ROOT_POS, "0:0 | 6:0,2.5,0 | 11:0,-2,1.5 OC | 14:0,-2.2,1.5 | 17:0,4,-2.5 O4 | 24:0,4,-2.5 | 33:0")
                .track(RIGHT_LEG, "0:0 | 6:-15,0,0 | 11:5,0,0 | 17:-35,0,0 O4 | 24:-35,0,0 | 33:0")
                .track(LEFT_LEG, "0:0 | 6:-15,0,0 | 11:5,0,0 | 17:25,0,0 O4 | 24:25,0,0 | 33:0")
                .track(HEAD, "0:0 | 11:12,0,0 | 17:-20,0,0 | 24:-18,0,0 | 33:0")
                
                .apply(fp -> FirstPersonCurves.overhead(14, 17, 33).applyTo(fp)).build());

        // Dash: rising shoulder swing from low left to high right.
        register(a("mace_dash", 21)
                .track(RIGHT_ARM, "0:-20,0,0 | 5:10,-60,30 OC | 7:-60,0,0 IC | 9:-140,70,-20 O4 | 13:-130,65,-15 S | 21:-25,5,0")
                .track(LEFT_ARM, "0:-10,0,0 | 5:-30,20,0 OC | 9:-40,-40,0 O4 | 21:-10,0,0")
                .track(BODY, "0:0 | 5:6,-35,0 OC | 9:-4,35,0 O4 | 13:-4,30,0 | 21:0")
                .track(ROOT, "0:0 | 5:10,-12,0 OC | 9:-4,15,0 O4 | 21:0")
                .track(ROOT_POS, "0:0 | 5:0,2,0 OC | 9:0,0,-2 O4 | 21:0")
                .track(RIGHT_LEG, "0:0 | 5:-25,0,0 | 9:-10,0,0 | 21:0")
                .track(LEFT_LEG, "0:0 | 5:20,0,0 | 9:10,0,0 | 21:0")
                .track(HEAD, "0:0 | 5:-8,12,0 | 9:4,-14,0 | 21:0")
                
                .apply(fp -> FirstPersonCurves.cutRight(5, 9, 21).applyTo(fp)).build());

        // ------------------------------------------------------------------ SPEAR (trident)
        register(a("spear_thrust_1", 15)
                .track(RIGHT_ARM, "0:-25,5,0 | 4:30,10,0 OC | 5.5:-35,0,0 OE | 7:-35,0,0 | 10:-30,0,0 S | 15:-20,5,0")
                .track(LEFT_ARM, "0:-35,25,0 | 4:-15,20,0 OC | 5.5:-60,20,0 OE | 15:-20,10,0")
                .track(BODY, "0:0 | 4:0,20,0 OC | 5.5:0,-10,0 OE | 15:0")
                .track(ROOT, "0:0 | 4:-3,12,0 OC | 5.5:10,-5,0 OE | 10:8,-4,0 | 15:0")
                .track(ROOT_POS, "0:0 | 4:0,0.5,1.5 OC | 5.5:0,1.5,-2.5 OE | 10:0,1.5,-2 | 15:0")
                .track(RIGHT_LEG, "0:0 | 5.5:-25,0,0 OE | 10:-22,0,0 | 15:0")
                .track(LEFT_LEG, "0:0 | 5.5:18,0,0 OE | 10:15,0,0 | 15:0")
                .track(HEAD, "0:0 | 4:3,-12,0 | 5.5:-10,5,0 | 15:0")
                
                .apply(fp -> FirstPersonCurves.thrust(4, 7, 15).applyTo(fp)).build());

        register(a("spear_thrust_2", 14)
                .track(RIGHT_ARM, "0:-30,0,0 | 3:25,-5,0 OC | 4.5:-50,-5,0 OE | 6:-50,-5,0 | 9:-45,0,0 S | 14:-20,5,0")
                .track(LEFT_ARM, "0:-60,20,0 | 3:-20,20,0 OC | 4.5:-70,15,0 OE | 14:-20,10,0")
                .track(BODY, "0:0 | 3:0,15,0 OC | 4.5:0,-15,0 OE | 14:0")
                .track(ROOT, "0:0 | 3:0,10,0 OC | 4.5:8,-8,0 OE | 9:6,-6,0 | 14:0")
                .track(ROOT_POS, "0:0 | 3:0,1,1 OC | 4.5:0,1,-2.5 OE | 9:0,1,-2 | 14:0")
                .track(RIGHT_LEG, "0:0 | 4.5:18,0,0 OE | 9:15,0,0 | 14:0")
                .track(LEFT_LEG, "0:0 | 4.5:-25,0,0 OE | 9:-22,0,0 | 14:0")
                .track(HEAD, "0:0 | 3:0,-10,0 | 4.5:-5,8,0 | 14:0")
                
                .apply(fp -> FirstPersonCurves.thrust(3, 6, 14).applyTo(fp)).build());

        register(a("spear_sweep_3", 20)
                .track(RIGHT_ARM, "0:-35,0,0 | 6:-70,110,-30 OC | 8:-70,10,0 IC | 10:-60,-90,30 O4 | 13:-55,-80,25 S | 20:-20,5,0")
                .track(LEFT_ARM, "0:-20,10,0 | 6:-40,-25,0 OC | 10:20,10,0 O4 | 20:-20,10,0")
                .track(BODY, "0:0 | 6:0,40,0 OC | 8:0,5,0 IC | 10:0,-40,0 O4 | 13:0,-35,0 S | 20:0")
                .track(ROOT, "0:0 | 6:-3,14,0 OC | 10:6,-14,0 O4 | 20:0")
                .track(ROOT_POS, "0:0 | 6:0,1,1 OC | 10:0,1.5,-1.5 O4 | 20:0")
                .track(RIGHT_LEG, "0:0 | 6:10,0,0 | 10:-25,0,0 O4 | 20:0")
                .track(LEFT_LEG, "0:0 | 6:-10,0,0 | 10:18,0,0 O4 | 20:0")
                .track(HEAD, "0:0 | 6:0,-14,0 | 10:0,14,0 | 20:0")
                
                .apply(fp -> FirstPersonCurves.cutLeft(6, 10, 20).applyTo(fp)).build());

        // Heavy: deep coil, then an explosive long thrust.
        register(a("spear_heavy", 27)
                .track(RIGHT_ARM, "0:-25,5,0 | 11:45,25,0 OC | 12.5:-40,0,0 OE | 15:-40,0,0 | 20:-35,0,0 S | 27:-20,5,0")
                .track(LEFT_ARM, "0:-35,25,0 | 11:-70,-10,0 OC | 12.5:30,0,0 OE | 27:-20,10,0")
                .track(BODY, "0:0 | 11:-5,30,0 OC | 12.5:8,-20,0 OE | 20:6,-15,0 | 27:0")
                .track(ROOT, "0:0 | 11:-8,20,0 OC | 12.5:18,-8,0 OE | 20:15,-6,0 | 27:0")
                .track(ROOT_POS, "0:0 | 11:0,1.5,3 OC | 12.5:0,2.5,-4 OE | 20:0,2.5,-3.5 | 27:0")
                .track(RIGHT_LEG, "0:0 | 11:15,0,0 OC | 12.5:-40,0,0 OE | 20:-38,0,0 | 27:0")
                .track(LEFT_LEG, "0:0 | 11:-20,0,0 OC | 12.5:30,0,0 OE | 20:28,0,0 | 27:0")
                .track(HEAD, "0:0 | 11:8,-20,0 | 12.5:-18,8,0 | 27:0")
                
                .apply(fp -> FirstPersonCurves.thrust(11, 15, 27).applyTo(fp)).build());

        register(thrust("spear_dash", 3, 7, 17, 1.2f));

        // ------------------------------------------------------------------ FIST
        String guardR = "-75,-20,0", guardL = "-70,15,0";
        register(a("fist_jab", 9)
                .track(LEFT_ARM, "0:" + guardL + " | 2:-60,10,0 OC | 3:-95,8,0 OE | 4:-95,8,0 | 9:" + guardL)
                .track(RIGHT_ARM, "0:" + guardR + " | 2:-80,-20,0 | 4:-70,-20,0 | 9:" + guardR)
                .track(BODY, "0:0 | 2:0,-10,0 OC | 3:0,20,0 OE | 9:0")
                .track(ROOT, "0:0 | 3:5,8,0 OE | 9:0")
                .track(ROOT_POS, "0:0 | 3:0,0.5,-1.5 OE | 9:0")
                .track(HEAD, "0:0 | 3:0,-8,0 | 9:0")
                .track(FP_ROT, "0:0 | 3:5,0,0 OE | 9:0")
                .track(FP_POS, "0:0 | 3:0.05,-0.05,0.08 OE | 9:0")
                .build());

        register(a("fist_cross", 10)
                .track(RIGHT_ARM, "0:" + guardR + " | 2:-65,-10,0 OC | 3:-95,-8,0 OE | 4:-95,-8,0 | 10:" + guardR)
                .track(LEFT_ARM, "0:" + guardL + " | 3:-60,20,0 | 10:" + guardL)
                .track(BODY, "0:0 | 2:0,10,0 OC | 3:0,-25,0 OE | 10:0")
                .track(ROOT, "0:0 | 3:6,-8,0 OE | 10:0")
                .track(ROOT_POS, "0:0 | 3:0,0.5,-2 OE | 10:0")
                .track(HEAD, "0:0 | 3:0,8,0 | 10:0")
                .track(FP_ROT, "0:0 | 2:0,-5,0 OC | 3:5,5,0 OE | 4:5,5,0 | 10:0")
                .track(FP_POS, "0:0 | 2:0.05,-0.05,0.15 OC | 3:-0.15,0.1,-0.4 OE | 4:-0.15,0.1,-0.4 | 10:0")
                .build());

        register(a("fist_hook", 12)
                .track(LEFT_ARM, "0:" + guardL + " | 3:-80,-60,0 OC | 4.5:-90,0,0 IC | 6:-85,45,0 O4 | 12:" + guardL)
                .track(RIGHT_ARM, "0:" + guardR + " | 12:" + guardR)
                .track(BODY, "0:0 | 3:0,-25,0 OC | 6:0,30,0 O4 | 12:0")
                .track(ROOT, "0:0 | 3:0,-8,0 OC | 6:6,12,0 O4 | 12:0")
                .track(ROOT_POS, "0:0 | 6:0,1,-1 O4 | 12:0")
                .track(RIGHT_LEG, "0:0 | 6:10,0,0 | 12:0")
                .track(LEFT_LEG, "0:0 | 6:-15,0,0 | 12:0")
                .track(HEAD, "0:0 | 3:0,8,0 | 6:0,-12,0 | 12:0")
                .track(FP_ROT, "0:0 | 3:0,-10,-5 OC | 6:5,10,5 O4 | 12:0")
                .track(FP_POS, "0:0 | 3:0.1,0,0.1 OC | 6:0,-0.05,0 O4 | 12:0")
                .build());

        register(a("fist_uppercut", 20)
                .track(RIGHT_ARM, "0:" + guardR + " | 7:5,-10,0 OC | 8.5:-90,-5,0 IC | 10:-160,-5,0 O4 | 14:-150,-5,0 S | 20:" + guardR)
                .track(LEFT_ARM, "0:" + guardL + " | 7:-80,20,0 | 10:-40,10,0 | 20:" + guardL)
                .track(BODY, "0:0 | 7:8,15,0 OC | 10:-6,-15,0 O4 | 20:0")
                .track(ROOT, "0:0 | 7:12,8,0 OC | 10:-8,-6,0 O4 | 14:-6,-5,0 | 20:0")
                .track(ROOT_POS, "0:0 | 7:0,3,0 OC | 10:0,-1.5,-1.5 O4 | 14:0,-1,-1 | 20:0")
                .track(RIGHT_LEG, "0:0 | 7:-20,0,0 OC | 10:-5,0,0 | 20:0")
                .track(LEFT_LEG, "0:0 | 7:-20,0,0 OC | 10:10,0,0 | 20:0")
                .track(HEAD, "0:0 | 7:-10,0,0 | 10:8,0,0 | 20:0")
                .track(FP_ROT, "0:0 | 7:-20,0,0 OC | 10:60,0,5 O4 | 14:55,0,5 S | 20:0")
                .track(FP_POS, "0:0 | 7:0,-0.3,0.1 OC | 10:-0.05,0.35,-0.2 O4 | 14:-0.05,0.3,-0.15 S | 20:0")
                .build());

        register(a("fist_dash", 16)
                .track(RIGHT_ARM, "0:" + guardR + " | 3:10,-10,0 OC | 4.5:-100,-5,0 OE | 6:-100,-5,0 | 10:-95,-5,0 S | 16:" + guardR)
                .track(LEFT_ARM, "0:" + guardL + " | 3:-90,20,0 OC | 4.5:10,10,0 OE | 16:" + guardL)
                .track(BODY, "0:0 | 3:0,20,0 OC | 4.5:0,-20,0 OE | 16:0")
                .track(ROOT, "0:0 | 3:-5,10,0 OC | 4.5:15,-8,0 OE | 10:12,-6,0 | 16:0")
                .track(ROOT_POS, "0:0 | 3:0,-1,1 OC | 4.5:0,1,-3 OE | 10:0,1,-2.5 | 16:0")
                .track(RIGHT_LEG, "0:0 | 3:-40,0,0 OC | 4.5:20,0,0 OE | 16:0")
                .track(LEFT_LEG, "0:0 | 3:20,0,0 OC | 4.5:-30,0,0 OE | 16:0")
                .track(HEAD, "0:0 | 4.5:-12,6,0 | 16:0")
                .track(FP_ROT, "0:0 | 3:-10,-5,0 OC | 4.5:5,5,0 OE | 10:5,5,0 S | 16:0")
                .track(FP_POS, "0:0 | 3:0.1,0,0.25 OC | 4.5:-0.15,0.1,-0.55 OE | 10:-0.15,0.1,-0.5 S | 16:0")
                .build());

        // ------------------------------------------------------------------ MOVEMENT
        register(a(DODGE_BACK, 10)
                .additive(RIGHT_ARM, LEFT_ARM)
                .track(ROOT, "0:0 | 2:-15,0,0 OC | 7:-8,0,0 | 10:0")
                .track(ROOT_POS, "0:0 | 2:0,1.5,0 OC | 5:0 | 8:0,1.5,0 | 10:0")
                .track(RIGHT_LEG, "0:0 | 2:-30,0,0 OC | 6:10,0,0 | 10:0")
                .track(LEFT_LEG, "0:0 | 2:-15,0,0 OC | 6:20,0,0 | 10:0")
                .track(RIGHT_ARM, "0:0 | 2:-35,0,15 OC | 7:-10,0,5 | 10:0")
                .track(LEFT_ARM, "0:0 | 2:-35,0,-15 OC | 7:-10,0,-5 | 10:0")
                .track(FP_ROT, "0:0 | 2:10,0,0 OC | 10:0")
                .track(FP_POS, "0:0 | 2:0,-0.08,0.1 OC | 10:0")
                .build());

        register(a(DODGE_LEFT, 10)
                .additive(RIGHT_ARM, LEFT_ARM)
                .track(ROOT, "0:0 | 2:0,0,12 OC | 7:0,0,6 | 10:0")
                .track(ROOT_POS, "0:0 | 2:0,1.5,0 OC | 5:0 | 8:0,1.2,0 | 10:0")
                .track(RIGHT_LEG, "0:0 | 2:0,0,10 OC | 6:0,0,-5 | 10:0")
                .track(LEFT_LEG, "0:0 | 2:0,0,-20 OC | 6:0,0,-5 | 10:0")
                .track(RIGHT_ARM, "0:0 | 2:0,0,25 OC | 7:0,0,10 | 10:0")
                .track(LEFT_ARM, "0:0 | 2:0,0,-35 OC | 7:0,0,-10 | 10:0")
                .track(FP_ROT, "0:0 | 2:0,0,-8 OC | 10:0")
                .track(FP_POS, "0:0 | 2:0.1,-0.05,0 OC | 10:0")
                .build());

        register(a(DODGE_RIGHT, 10)
                .additive(RIGHT_ARM, LEFT_ARM)
                .track(ROOT, "0:0 | 2:0,0,-12 OC | 7:0,0,-6 | 10:0")
                .track(ROOT_POS, "0:0 | 2:0,1.5,0 OC | 5:0 | 8:0,1.2,0 | 10:0")
                .track(LEFT_LEG, "0:0 | 2:0,0,-10 OC | 6:0,0,5 | 10:0")
                .track(RIGHT_LEG, "0:0 | 2:0,0,20 OC | 6:0,0,5 | 10:0")
                .track(LEFT_ARM, "0:0 | 2:0,0,-25 OC | 7:0,0,-10 | 10:0")
                .track(RIGHT_ARM, "0:0 | 2:0,0,35 OC | 7:0,0,10 | 10:0")
                .track(FP_ROT, "0:0 | 2:0,0,8 OC | 10:0")
                .track(FP_POS, "0:0 | 2:-0.1,-0.05,0 OC | 10:0")
                .build());

        register(a(ROLL_FORWARD, 12)
                .override(RIGHT_LEG, LEFT_LEG)
                .track(ROOT, "0:0 | 10:360,0,0 IO | 12:360,0,0")
                .track(ROOT_POS, "0:0 | 2:0,7,-2 OC | 8:0,7,-4 | 12:0")
                .track(RIGHT_LEG, "0:0 | 2:-80,0,0 OC | 9:-80,0,0 | 12:0")
                .track(LEFT_LEG, "0:0 | 2:-80,0,0 OC | 9:-80,0,0 | 12:0")
                .track(RIGHT_ARM, "0:-20,0,0 | 2:-60,-10,0 OC | 9:-60,-10,0 | 12:-20,0,0")
                .track(LEFT_ARM, "0:-20,0,0 | 2:-60,10,0 OC | 9:-60,10,0 | 12:-20,0,0")
                .track(HEAD, "0:0 | 2:20,0,0 | 9:20,0,0 | 12:0")
                .track(FP_ROT, "0:0 | 2:15,0,0 OC | 9:15,0,0 | 12:0")
                .track(FP_POS, "0:0 | 2:0,-0.25,0.1 OC | 9:0,-0.25,0.1 | 12:0")
                .build());

        // ------------------------------------------------------------------ REACTIONS (additive, layer over guard)
        register(a(FLINCH, 8)
                .additive(RIGHT_ARM, LEFT_ARM)
                .track(ROOT, "0:0 | 1.5:-10,0,4 OE | 8:0")
                .track(ROOT_POS, "0:0 | 1.5:0,0.5,1 OE | 8:0")
                .track(HEAD, "0:0 | 1.5:-15,0,0 OE | 8:0")
                .track(RIGHT_ARM, "0:0 | 1.5:-15,0,15 OE | 8:0")
                .track(LEFT_ARM, "0:0 | 1.5:-15,0,-15 OE | 8:0")
                .track(FP_ROT, "0:0 | 1.5:8,0,4 OE | 8:0")
                .track(FP_POS, "0:0 | 1.5:0,0.03,0.08 OE | 8:0")
                .build());

        register(a(STAGGER, 20)
                .additive(RIGHT_ARM, LEFT_ARM)
                .track(ROOT, "0:0 | 2:-18,0,6 OE | 6:-12,0,-4 | 11:-6,0,3 | 20:0")
                .track(ROOT_POS, "0:0 | 2:0,1,2 OE | 8:0,1.5,2.5 | 20:0")
                .track(HEAD, "0:0 | 2:-20,10,0 OE | 8:-10,-8,0 | 20:0")
                .track(RIGHT_ARM, "0:0 | 2:-30,0,30 OE | 10:-10,0,10 | 20:0")
                .track(LEFT_ARM, "0:0 | 2:-30,0,-30 OE | 10:-10,0,-10 | 20:0")
                .track(RIGHT_LEG, "0:0 | 3:20,0,0 OE | 10:10,0,0 | 20:0")
                .track(LEFT_LEG, "0:0 | 3:-10,0,0 OE | 10:-5,0,0 | 20:0")
                .track(FP_ROT, "0:0 | 2:20,0,10 OE | 8:10,0,-5 | 20:0")
                .track(FP_POS, "0:0 | 2:0,-0.1,0.15 OE | 20:0")
                .build());

        register(a(GUARD_HIT, 6)
                .track(ROOT, "0:0 | 1:-6,0,0 OE | 6:0")
                .track(ROOT_POS, "0:0 | 1:0,0.3,1 OE | 6:0")
                .track(FP_POS, "0:0 | 1:0,-0.04,0.08 OE | 6:0")
                .track(FP_ROT, "0:0 | 1:6,0,0 OE | 6:0")
                .build());

        register(a(GUARD_BREAK, 24)
                .track(RIGHT_ARM, "0:-60,-30,60 | 3:-150,60,0 OE | 12:-110,50,0 | 24:-20,5,0")
                .track(LEFT_ARM, "0:-50,30,0 | 3:-140,-60,0 OE | 12:-100,-50,0 | 24:-10,0,0")
                .track(ROOT, "0:0 | 3:-16,0,0 OE | 12:-8,0,0 | 24:0")
                .track(ROOT_POS, "0:0 | 3:0,0.5,2.5 OE | 12:0,1,2 | 24:0")
                .track(HEAD, "0:0 | 3:-20,0,0 OE | 12:-10,0,0 | 24:0")
                .track(FP_ROT, "0:0,0,90 | 3:30,30,-20 OE | 12:15,20,-10 | 24:0")
                .track(FP_POS, "0:-0.05,0.22,0 | 3:0.2,0.25,0.1 OE | 12:0.15,0.15,0.1 | 24:0")
                .build());

        register(a(PARRY, 10)
                .track(RIGHT_ARM, "0:-60,-35,70 | 2:-110,40,20 OE | 5:-90,30,15 | 10:-40,10,0")
                .track(LEFT_ARM, "0:-50,35,0 | 2:-20,-10,0 OE | 10:-15,0,0")
                .track(BODY, "0:0 | 2:0,25,0 OE | 10:0")
                .track(ROOT_POS, "0:0 | 2:0,0.5,0.5 OE | 10:0")
                .track(FP_ROT, "0:0,0,90 | 2:0,45,-10 OE | 5:0,35,-5 | 10:0")
                .track(FP_POS, "0:-0.05,0.22,0 | 2:0.05,0.12,-0.05 OE | 5:0.1,0.18,0 | 10:0")
                .build());

        // ------------------------------------------------------------------ GUARD STANCES (looping)
        register(guard("guard_sword", "-55,-35,70", "-50,35,0", "0,0,90", "-0.05,0.22,0"));
        register(guard("guard_axe", "-60,-40,80", "-55,35,0", "0,0,90", "-0.05,0.22,0"));
        register(guard("guard_heavy", "-60,-40,80", "-60,35,0", "0,0,90", "-0.05,0.22,0"));
        register(guard("guard_spear", "-40,-20,80", "-60,25,0", "0,0,90", "-0.05,0.22,0"));
        register(guard("guard_fist", "-120,-25,0", "-120,25,0", "10,5,10", "-0.15,0.2,0.05"));
    }

    /** Lunging thrust used by the sword and spear dash attacks. */
    private static Animation thrust(String id, int windupEnd, int activeEnd, int length, float depth) {
        float w = windupEnd, hit = windupEnd + 2, hold = activeEnd + 3;
        return a(id, length)
                .track(RIGHT_ARM, "0:-30,10,0 | " + w + ":35,10,0 OC | " + hit + ":-30,0,0 OE | " + hold + ":-25,0,0 S | " + length + ":-20,5,0")
                .track(LEFT_ARM, "0:-20,0,0 | " + w + ":-50,-20,0 OC | " + hit + ":30,0,0 OE | " + length + ":-10,0,0")
                .track(BODY, "0:0 | " + w + ":0,25,0 OC | " + hit + ":0,-15,0 OE | " + length + ":0")
                .track(ROOT, "0:0 | " + w + ":-5,15,0 OC | " + hit + ":15,-5,0 OE | " + hold + ":12,-5,0 | " + length + ":0")
                .track(ROOT_POS, "0:0 | " + w + ":0,0.5,2 OC | " + hit + ":0,2," + (-3 * depth) + " OE | " + hold + ":0,2," + (-3 * depth) + " | " + length + ":0")
                .track(RIGHT_LEG, "0:0 | " + hit + ":-35,0,0 OE | " + hold + ":-30,0,0 | " + length + ":0")
                .track(LEFT_LEG, "0:0 | " + hit + ":25,0,0 OE | " + hold + ":22,0,0 | " + length + ":0")
                .track(HEAD, "0:0 | " + w + ":5,-15,0 | " + hit + ":-15,5,0 | " + length + ":0")
                .apply(fp -> FirstPersonCurves.thrust(windupEnd, activeEnd, length).applyTo(fp))
                .build();
    }

    /** A looping guard stance with a subtle breathing motion. */
    private static Animation guard(String id, String rightArm, String leftArm, String fpRot, String fpPos) {
        return a(id, 40).loop()
                .track(RIGHT_ARM, "0:" + rightArm + " | 40:" + rightArm)
                .track(LEFT_ARM, "0:" + leftArm + " | 40:" + leftArm)
                .track(ROOT_POS, "0:0,0.8,0 | 20:0,1.2,0 S | 40:0,0.8,0 S")
                .track(RIGHT_LEG, "0:-8,0,0 | 40:-8,0,0")
                .track(LEFT_LEG, "0:8,0,0 | 40:8,0,0")
                .track(FP_ROT, "0:" + fpRot + " | 40:" + fpRot)
                .track(FP_POS, "0:" + fpPos + " | 40:" + fpPos)
                .build();
    }
}

package com.fluidcombat.anim;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A keyframed pose animation. Time is measured in ticks (fractional), at an attack speed of 1.0.
 * <p>
 * For each bone a track of keyframes is stored. Every keyframe owns the easing curve used for the
 * segment arriving at it, which gives animators per-segment control over anticipation, snap and settle.
 * <p>
 * Bones listed in {@link #overrides()} replace the vanilla pose. All other keyed bones are added on top of
 * the vanilla pose, so walking, head-tracking and sneaking keep working underneath an attack.
 */
public final class Animation {
    public record Key(float time, float x, float y, float z, Easing easing) {
    }

    private final String id;
    private final float length;
    private final boolean loop;
    private final Map<Bone, Key[]> tracks;
    private final Set<Bone> overrides;

    private Animation(String id, float length, boolean loop, Map<Bone, Key[]> tracks, Set<Bone> overrides) {
        this.id = id;
        this.length = length;
        this.loop = loop;
        this.tracks = tracks;
        this.overrides = overrides;
    }

    public String id() {
        return id;
    }

    public float length() {
        return length;
    }

    public boolean loop() {
        return loop;
    }

    public Set<Bone> overrides() {
        return overrides;
    }

    public boolean hasTrack(Bone bone) {
        return tracks.containsKey(bone);
    }

    public Set<Bone> keyedBones() {
        return tracks.keySet();
    }

    /** Samples every keyed bone into {@code out}. Unkeyed bones are left untouched. */
    public void sample(float time, Pose out) {
        if (loop && length > 0) {
            time = time % length;
            if (time < 0) time += length;
        }
        for (Map.Entry<Bone, Key[]> e : tracks.entrySet()) {
            Key[] keys = e.getValue();
            Bone bone = e.getKey();
            if (time <= keys[0].time) {
                out.set(bone, keys[0].x, keys[0].y, keys[0].z);
                continue;
            }
            Key last = keys[keys.length - 1];
            if (time >= last.time) {
                out.set(bone, last.x, last.y, last.z);
                continue;
            }
            for (int i = 1; i < keys.length; i++) {
                Key b = keys[i];
                if (time <= b.time) {
                    Key a = keys[i - 1];
                    float span = b.time - a.time;
                    float t = span <= 0 ? 1 : (time - a.time) / span;
                    float k = b.easing.apply(t);
                    out.set(bone, a.x + (b.x - a.x) * k, a.y + (b.y - a.y) * k, a.z + (b.z - a.z) * k);
                    break;
                }
            }
        }
    }

    public static Builder builder(String id, float length) {
        return new Builder(id, length);
    }

    public static final class Builder {
        private final String id;
        private final float length;
        private boolean loop;
        private final Map<Bone, List<Key>> tracks = new EnumMap<>(Bone.class);
        private final Set<Bone> overrides = EnumSet.of(Bone.RIGHT_ARM, Bone.LEFT_ARM);

        private Builder(String id, float length) {
            this.id = id;
            this.length = length;
        }

        public Builder loop() {
            this.loop = true;
            return this;
        }

        /** Keyed bones in this set replace the vanilla pose rather than adding to it. */
        public Builder override(Bone... bones) {
            overrides.addAll(List.of(bones));
            return this;
        }

        /** Keyed bones in this set are added on top of the vanilla pose. */
        public Builder additive(Bone... bones) {
            for (Bone b : bones) overrides.remove(b);
            return this;
        }

        public Builder key(Bone bone, float time, float x, float y, float z, Easing easing) {
            tracks.computeIfAbsent(bone, b -> new ArrayList<>()).add(new Key(time, x, y, z, easing));
            return this;
        }

        public Builder key(Bone bone, float time, float x, float y, float z) {
            return key(bone, time, x, y, z, Easing.IN_OUT_QUAD);
        }

        /**
         * Adds a whole track from a compact spec: {@code "t:x,y,z EASE | t:x,y,z EASE | ..."}.
         * A single value ({@code "t:0"}) means all three channels are zero. Easing codes: see {@link #easing(String)}.
         */
        public Builder track(Bone bone, String spec) {
            for (String part : spec.split("\\|")) {
                part = part.trim();
                if (part.isEmpty()) continue;
                int colon = part.indexOf(':');
                float time = Float.parseFloat(part.substring(0, colon).trim());
                String rest = part.substring(colon + 1).trim();
                Easing easing = Easing.IN_OUT_QUAD;
                int space = rest.indexOf(' ');
                if (space > 0) {
                    easing = easing(rest.substring(space + 1).trim());
                    rest = rest.substring(0, space);
                }
                String[] v = rest.split(",");
                float x = Float.parseFloat(v[0]);
                float y = v.length > 1 ? Float.parseFloat(v[1]) : 0;
                float z = v.length > 2 ? Float.parseFloat(v[2]) : 0;
                key(bone, time, x, y, z, easing);
            }
            return this;
        }

        private static Easing easing(String code) {
            return switch (code) {
                case "L" -> Easing.LINEAR;
                case "IQ" -> Easing.IN_QUAD;
                case "OQ" -> Easing.OUT_QUAD;
                case "IO" -> Easing.IN_OUT_QUAD;
                case "IC" -> Easing.IN_CUBIC;
                case "OC" -> Easing.OUT_CUBIC;
                case "IOC" -> Easing.IN_OUT_CUBIC;
                case "O4" -> Easing.OUT_QUART;
                case "IE" -> Easing.IN_EXPO;
                case "OE" -> Easing.OUT_EXPO;
                case "OB" -> Easing.OUT_BACK;
                case "IB" -> Easing.IN_BACK;
                case "EL" -> Easing.OUT_ELASTIC;
                case "S" -> Easing.SMOOTH;
                default -> throw new IllegalArgumentException("Unknown easing " + code);
            };
        }

        /** Applies a reusable fragment (e.g. a shared first-person curve) to this builder. */
        public Builder apply(java.util.function.Consumer<Builder> fragment) {
            fragment.accept(this);
            return this;
        }

        public Animation build() {
            Map<Bone, Key[]> out = new EnumMap<>(Bone.class);
            tracks.forEach((bone, keys) -> {
                keys.sort((a, b) -> Float.compare(a.time, b.time));
                out.put(bone, keys.toArray(Key[]::new));
            });
            return new Animation(id, length, loop, out, EnumSet.copyOf(overrides));
        }
    }
}

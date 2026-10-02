package com.fluidcombat.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;

/** Animation controllers for every animated entity in the client level. */
public final class ClientAnims {
    private static final Int2ObjectMap<AnimController> CONTROLLERS = new Int2ObjectOpenHashMap<>();
    static int ticks;

    private ClientAnims() {
    }

    @Nullable
    public static AnimController get(int entityId) {
        return CONTROLLERS.get(entityId);
    }

    public static AnimController getOrCreate(int entityId) {
        return CONTROLLERS.computeIfAbsent(entityId, id -> new AnimController());
    }

    static void tick(ClientLevel level) {
        ticks++;
        ObjectIterator<Int2ObjectMap.Entry<AnimController>> it = CONTROLLERS.int2ObjectEntrySet().iterator();
        while (it.hasNext()) {
            Int2ObjectMap.Entry<AnimController> e = it.next();
            AnimController c = e.getValue();
            c.tick();
            if (c.idleTicks > 100 || level.getEntity(e.getIntKey()) == null) it.remove();
        }
    }

    static void clear() {
        CONTROLLERS.clear();
    }
}

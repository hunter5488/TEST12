package com.fluidcombat.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class Keybinds {
    public static final String CATEGORY = "key.categories.fluidcombat";
    public static final KeyMapping DODGE = new KeyMapping("key.fluidcombat.dodge", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY);
    public static final KeyMapping GUARD = new KeyMapping("key.fluidcombat.guard", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    private Keybinds() {
    }

    static void register(RegisterKeyMappingsEvent event) {
        event.register(DODGE);
        event.register(GUARD);
    }
}

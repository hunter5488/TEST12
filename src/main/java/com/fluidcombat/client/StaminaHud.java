package com.fluidcombat.client;

import com.fluidcombat.combat.CombatLogic;
import com.fluidcombat.config.FCConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/** A slim stamina bar under the crosshair that fades out while full. */
public final class StaminaHud {
    private static float shown = -1;
    private static float alpha;
    private static long lastTick = -1;

    private StaminaHud() {
    }

    static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.options.hideGui || p.isSpectator() || !FCConfig.STAMINA_HUD.get() || !FCConfig.STAMINA.get()
                || !FCConfig.PLAYERS.get()) return;
        float max = CombatLogic.maxStamina();
        float stamina = ClientCombat.stamina < 0 ? max : ClientCombat.stamina;

        long tick = p.tickCount;
        if (tick != lastTick) {
            lastTick = tick;
            shown = shown < 0 ? stamina : Mth.lerp(0.45f, shown, stamina);
            boolean visible = stamina < max - 0.01f || ClientCombat.serverGuarding;
            alpha = Mth.clamp(alpha + (visible ? 0.25f : -0.08f), 0, 1);
        }
        if (alpha <= 0) return;

        int w = 40, h = 2;
        int x = g.guiWidth() / 2 - w / 2;
        int y = g.guiHeight() / 2 + 10;
        float ratio = Mth.clamp(shown / max, 0, 1);
        int a = (int) (alpha * 255);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, FastColor.ARGB32.color((int) (a * 0.55f), 0, 0, 0));

        int color;
        if (ratio < 0.25f) {
            boolean flash = (tick / 3) % 2 == 0;
            color = flash ? FastColor.ARGB32.color(a, 255, 70, 60) : FastColor.ARGB32.color(a, 200, 40, 30);
        } else if (ClientCombat.serverGuarding) {
            color = FastColor.ARGB32.color(a, 120, 190, 255);
        } else {
            float t = (ratio - 0.25f) / 0.75f;
            color = FastColor.ARGB32.color(a, (int) Mth.lerp(t, 255, 140), (int) Mth.lerp(t, 200, 230), (int) Mth.lerp(t, 60, 90));
        }
        g.fill(x, y, x + Math.round(w * ratio), y + h, color);
    }
}

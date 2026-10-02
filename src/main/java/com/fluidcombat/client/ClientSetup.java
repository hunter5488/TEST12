package com.fluidcombat.client;

import com.fluidcombat.FluidCombat;
import com.fluidcombat.config.FCConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

public final class ClientSetup {
    private ClientSetup() {
    }

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, FCConfig.CLIENT_SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modBus.addListener(Keybinds::register);
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAbove(VanillaGuiLayers.CROSSHAIR, FluidCombat.id("stamina"), StaminaHud::render));

        IEventBus bus = NeoForge.EVENT_BUS;
        bus.addListener(ClientCombat::onClientTick);
        bus.addListener(ClientCombat::onInteraction);
        bus.addListener(ClientCombat::onMovementInput);
        bus.addListener(FirstPerson::onRenderHand);
        bus.addListener(FirstPerson::onCameraAngles);
    }
}

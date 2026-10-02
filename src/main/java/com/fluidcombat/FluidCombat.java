package com.fluidcombat;

import com.fluidcombat.client.ClientSetup;
import com.fluidcombat.combat.CombatEvents;
import com.fluidcombat.config.FCConfig;
import com.fluidcombat.network.Network;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(FluidCombat.MODID)
public class FluidCombat {
    public static final String MODID = "fluidcombat";

    public FluidCombat(IEventBus modBus, ModContainer container) {
        FCAttachments.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, FCConfig.SPEC);
        modBus.addListener(Network::register);
        CombatEvents.register(NeoForge.EVENT_BUS);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init(modBus, container);
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}

package com.fluidcombat;

import com.fluidcombat.combat.CombatState;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class FCAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, FluidCombat.MODID);

    /** Transient per-entity combat state (never saved). */
    public static final Supplier<AttachmentType<CombatState>> STATE =
            ATTACHMENTS.register("combat_state", () -> AttachmentType.builder(CombatState::new).build());

    private FCAttachments() {
    }

    static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }
}

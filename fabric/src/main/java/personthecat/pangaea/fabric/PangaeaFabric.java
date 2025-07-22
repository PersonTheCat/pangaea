package personthecat.pangaea.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.impl.registry.sync.DynamicRegistriesImpl;
import personthecat.catlib.event.lifecycle.GameReadyEvent;
import personthecat.pangaea.Pangaea;
import personthecat.pangaea.registry.PgRegistries;

public class PangaeaFabric extends Pangaea implements ModInitializer {

    @Override
    public void onInitialize() {
        this.init();
        this.commonSetup();
        ServerLifecycleEvents.SERVER_STARTING.register(this::startup);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::shutdown);
        GameReadyEvent.COMMON.register(this::manuallyRegisterEarlyDynamicRegistry);
    }

    @SuppressWarnings("UnstableApiUsage") // temporary
    private void manuallyRegisterEarlyDynamicRegistry() {
        DynamicRegistriesImpl.FABRIC_DYNAMIC_REGISTRY_KEYS.add(PgRegistries.Keys.DENSITY_TEMPLATE);
    }
}

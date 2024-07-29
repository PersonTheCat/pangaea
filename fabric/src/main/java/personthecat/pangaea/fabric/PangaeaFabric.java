package personthecat.pangaea.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import personthecat.pangaea.Pangaea;

public class PangaeaFabric extends Pangaea implements ModInitializer {

    @Override
    public void onInitialize() {
        this.init();
        this.commonSetup();
        ServerLifecycleEvents.SERVER_STOPPING.register(this::shutdown);
    }
}

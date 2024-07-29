package personthecat.pangaea.neo;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import personthecat.pangaea.Pangaea;

@Mod(Pangaea.ID)
public class PangaeaNeo extends Pangaea {

    public PangaeaNeo(final IEventBus modBus) {
        this.init();
        modBus.addListener(this::initCommon);
        modBus.addListener((ServerStoppingEvent e) -> this.shutdown(e.getServer()));
    }

    private void initCommon(final FMLCommonSetupEvent event) {
        this.commonSetup();
    }
}

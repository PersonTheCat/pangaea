package personthecat.pangaea.neo;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import personthecat.pangaea.Pangaea;

@Mod(Pangaea.ID)
public class PangaeaNeo extends Pangaea {

    public PangaeaNeo(final IEventBus modBus) {
        this.init();
        modBus.addListener(this::initCommon);
    }

    private void initCommon(final FMLCommonSetupEvent event) {
        this.commonSetup();
    }
}

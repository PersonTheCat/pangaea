package personthecat.pangaea.neo;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.util.Reference;

@Mod(Reference.MOD_ID)
public class Pangaea {

    public Pangaea(final IEventBus modBus) {
        Cfg.register();
        modBus.addListener(this::initCommon);
    }

    private void initCommon(final FMLCommonSetupEvent event) {
    }
}

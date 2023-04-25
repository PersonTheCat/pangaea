package personthecat.pangaea.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.util.Reference;

@Mod(Reference.MOD_ID)
public class Pangaea {

    public Pangaea() {
        Cfg.register();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::initCommon);
    }

    private void initCommon(final FMLCommonSetupEvent event) {
    }
}

package personthecat.pangaea.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import personthecat.pangaea.Pangaea;

@Mod(Pangaea.ID)
public class PangaeaForge extends Pangaea {

    public PangaeaForge() {
        this.init();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::initCommon);
    }

    private void initCommon(final FMLCommonSetupEvent event) {
        this.commonSetup();
    }
}

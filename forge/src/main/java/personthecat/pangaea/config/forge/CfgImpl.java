package personthecat.pangaea.config.forge;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import personthecat.catlib.config.forge.CustomModConfig;
import personthecat.catlib.config.forge.DjsFileConfig;
import personthecat.catlib.util.forge.McUtilsImpl;
import personthecat.pangaea.util.Reference;

public final class CfgImpl {

    private static final ForgeConfigSpec.Builder COMMON = new ForgeConfigSpec.Builder();
    private static final String FILENAME = McUtilsImpl.getConfigDir() + "/" + Reference.MOD_ID;
    private static final DjsFileConfig COMMON_CFG = new DjsFileConfig(FILENAME + ".djs");

    public static void register() {
        final ModContainer ctx = ModLoadingContext.get().getActiveContainer();
        ctx.addConfig(new CustomModConfig(ModConfig.Type.COMMON, COMMON.build(), ctx, COMMON_CFG));
    }
}

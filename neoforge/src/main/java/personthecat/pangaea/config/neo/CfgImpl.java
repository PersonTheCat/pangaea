package personthecat.pangaea.config.neo;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import personthecat.catlib.util.neo.McUtilsImpl;
import personthecat.pangaea.util.Reference;

public final class CfgImpl {

    private static final ModConfigSpec.Builder COMMON = new ModConfigSpec.Builder();

    public static void register() {
        final ModContainer ctx = ModLoadingContext.get().getActiveContainer();
        final String filename = McUtilsImpl.getConfigDir() + "/" + Reference.MOD_ID + ".djs";
        ctx.addConfig(new ModConfig(ModConfig.Type.COMMON, COMMON.build(), ctx, filename));
    }
}

package personthecat.pangaea.config.fabric;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import personthecat.catlib.config.fabric.DjsConfigSerializer;
import personthecat.pangaea.util.Reference;

@Config(name = Reference.MOD_ID)
public final class CfgImpl implements ConfigData {

    private static final CfgImpl CONFIG;

    public static void register() {}

    static {
        AutoConfig.register(CfgImpl.class, DjsConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(CfgImpl.class).getConfig();
    }
}

package personthecat.pangaea.config;

import dev.architectury.injectables.annotations.ExpectPlatform;
import personthecat.catlib.exception.MissingOverrideException;

public final class Cfg {

    private Cfg() {}

    @ExpectPlatform
    public static void register() {
        throw new MissingOverrideException();
    }
}

package personthecat.pangaea.test;

import personthecat.pangaea.Pangaea;

public final class PangaeaTest extends Pangaea {
    private static final PangaeaTest INSTANCE = new PangaeaTest();

    private PangaeaTest() {}

    public static PangaeaTest getInstance() {
        return INSTANCE;
    }

    public void bootstrap() {
        this.init();
    }
}

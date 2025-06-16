package personthecat.pangaea.util;

import com.mojang.serialization.DataResult;
import personthecat.catlib.event.lifecycle.ServerEvents;
import personthecat.catlib.serialization.codec.capture.Captor;
import personthecat.catlib.serialization.codec.capture.Captures;
import personthecat.catlib.serialization.codec.capture.Key;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public final class SeedSupport {
    private static final AtomicLong SEED = new AtomicLong(1337L);

    private SeedSupport() {}

    public static void setup() {
        ServerEvents.UNLOAD.register(server -> SEED.set(1337L));
    }

    public static long next() {
        return SEED.getAndIncrement();
    }

    public static int nextAsInt() {
        return new Random(next()).nextInt();
    }

    public static Captor<Integer> captureAsInt(Key<Integer> key) {
        return new IntCaptor(key);
    }

    private record IntCaptor(Key<Integer> key) implements Captor<Integer> {
        @Override
        public void capture(Captures captures) {
            captures.put(this.key, () -> DataResult.success(nextAsInt()));
        }
    }
}

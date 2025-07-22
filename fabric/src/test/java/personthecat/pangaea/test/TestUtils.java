package personthecat.pangaea.test;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JavaOps;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.platform.commons.util.ExceptionUtils;
import personthecat.catlib.serialization.codec.XjsOps;
import xjs.data.Json;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class TestUtils {
    private static final Map<Class<?>, Object> RELOADED_INSTANCE_MAP = new ConcurrentHashMap<>();

    private TestUtils() {}

    public static <A> DataResult<A> parse(Codec<A> subject, String json) {
        return subject.parse(XjsOps.INSTANCE, Json.parse(json));
    }

    public static <T> DataResult<Object> encode(Codec<T> codec, T value) {
        return codec.encodeStart(JavaOps.INSTANCE, value);
    }

    public static <T> void assertSuccess(T expected, DataResult<T> actual) {
        if (!actual.isSuccess()) {
            assertionFailure().message(getMessage(actual)).actual("error").expected("success").buildAndThrow();
        }
        assertEquals(expected, actual.getOrThrow());
    }

    public static <T> void assertError(DataResult<T> actual, String partialMessage) {
        assertError(actual);
        assertContains(getMessage(actual), partialMessage);
    }

    public static <T> void assertError(DataResult<T> actual) {
        if (!actual.isError()) {
            assertionFailure().message("parsed: " + actual.getOrThrow()).actual("success").expected("error").buildAndThrow();
        }
    }

    public static void assertContains(String s, String contains) {
        if (!s.contains(contains)) {
            assertionFailure().message("actual does not contain expected").actual(s).expected(contains).buildAndThrow();
        }
    }

    public static String getMessage(DataResult<?> result) {
        return result.error().orElseThrow().message();
    }

    public static Method getMethod(Class<?> clazz, String name, Class<?>... args) {
        try {
            return clazz.getDeclaredMethod(name, args);
        } catch (final NoSuchMethodException e) {
            throw ExceptionUtils.throwAsUncheckedException(e);
        }
    }

    public static void runFromMixinEnabledClassLoader(
            Class<?> clazz, String name) throws Throwable {
        runFromMixinEnabledClassLoader(getMethod(clazz, name));
    }

    public static void runFromMixinEnabledClassLoader(
            Method method, Object... args) throws Throwable {
        runFromMixinEnabledClassLoader(() -> null, method, args);
    }

    public static void runFromMixinEnabledClassLoader(
            InvocationInterceptor.Invocation<?> invocation, Method method, Object... args) throws Throwable {
        invocation.skip();
        getRemappedInvocationTarget(method).invoke(args);
    }

    public static void disposeRemappedInstances() {
        RELOADED_INSTANCE_MAP.clear();
    }

    private static RemappedMethod getRemappedInvocationTarget(Method original) {
        final Object instance = RELOADED_INSTANCE_MAP.computeIfAbsent(original.getDeclaringClass(),
            clazz -> newInstance(getRemappedClass(clazz)));
        final Method m = getMethod(instance.getClass(), original.getName(), original.getParameterTypes());
        return new RemappedMethod(instance, m);
    }

    private static Class<?> getRemappedClass(Class<?> clazz) {
        try {
            return FabricLauncherBase.getLauncher().loadIntoTarget(clazz.getName());
        } catch (final ClassNotFoundException e) {
            throw ExceptionUtils.throwAsUncheckedException(e);
        }
    }

    private static Object newInstance(Class<?> clazz) {
        final Constructor<?>[] constructors = clazz.getConstructors();
        if (constructors.length != 1) {
            throw new IllegalStateException("Expected exactly one constructor: " + clazz);
        }
        try {
            return constructors[0].newInstance();
        } catch (final ReflectiveOperationException e) {
            throw ExceptionUtils.throwAsUncheckedException(e);
        }
    }

    private record RemappedMethod(Object instance, Method method) {
        void invoke(final Object... args) throws Throwable {
            try {
                this.method.invoke(this.instance, args);
            } catch (final InvocationTargetException e) {
                throw ExceptionUtils.throwAsUncheckedException(e.getTargetException());
            }
        }
    }
}

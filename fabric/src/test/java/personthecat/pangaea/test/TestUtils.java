package personthecat.pangaea.test;

import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.platform.commons.util.ExceptionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TestUtils {
    private static final Map<Class<?>, Object> RELOADED_INSTANCE_MAP = new ConcurrentHashMap<>();

    private TestUtils() {}

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

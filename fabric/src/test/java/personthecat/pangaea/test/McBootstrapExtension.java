package personthecat.pangaea.test;

import lombok.extern.log4j.Log4j2;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.knot.Knot;
import net.fabricmc.loader.impl.util.SystemProperties;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;

@Log4j2
public class McBootstrapExtension implements Extension, InvocationInterceptor, AfterAllCallback {

    static {
        log.info("Launching game environment");
        final StopWatch sw = StopWatch.createStarted();
        System.setProperty(SystemProperties.UNIT_TEST, "true");
        System.setProperty(SystemProperties.DEVELOPMENT, "true");
        System.setProperty(SystemProperties.SIDE, "server");
        Knot.launch(new String[0], EnvType.SERVER);
        sw.stop();
        log.info("Environment launched in {}", sw);
    }

    @Override
    public void interceptBeforeAllMethod(
            Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        TestUtils.runFromMixinEnabledClassLoader(invocation, invocationContext.getExecutable());
    }

    @Override
    public void interceptBeforeEachMethod(
            Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        TestUtils.runFromMixinEnabledClassLoader(invocation, invocationContext.getExecutable());
    }

    @Override
    public void interceptAfterAllMethod(
            Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        TestUtils.runFromMixinEnabledClassLoader(invocation, invocationContext.getExecutable());
    }

    @Override
    public void interceptAfterEachMethod(
            Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        TestUtils.runFromMixinEnabledClassLoader(invocation, invocationContext.getExecutable());
    }

    @Override
    public void interceptTestMethod(
            Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        TestUtils.runFromMixinEnabledClassLoader(invocation, invocationContext.getExecutable());
    }

    @Override
    public void afterAll(ExtensionContext context) {
        TestUtils.disposeRemappedInstances();
    }
}

package dev.lumenlang.lumen.api.bus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When placed on a {@link Subscribe} method, the handler is invoked
 * asynchronously on a background thread instead of on the posting thread.
 *
 * <pre>{@code
 * @Subscribe
 * @Async
 * public void onScriptLoad(@NotNull ScriptLoadEvent event) {
 *     // runs off the main thread
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Async {
}

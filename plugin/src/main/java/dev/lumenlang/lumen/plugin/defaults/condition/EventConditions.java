package dev.lumenlang.lumen.plugin.defaults.condition;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.pattern.Categories;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

/**
 * Registers event-related condition patterns.
 */
@Registration
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class EventConditions {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("[this] (is|is not) [already] cancelled")
                .description("Checks if the current event is cancelled. Only works inside cancelable event handlers.")
                .example("if this is cancelled:")
                .since("1.0.0")
                .category(Categories.EVENT)
                .handler((match, env, ctx) -> {
                    if (env.block() == null || env.block().getEnvFromParents("__event_block") == null) {
                        throw new RuntimeException("'is cancelled' condition can only be used inside an event handler block");
                    }
                    Boolean cancellable = env.block().getEnvFromParents("__event_cancellable");
                    if (cancellable != null && !cancellable) {
                        throw new RuntimeException("'is cancelled' condition cannot be used here because this event is not cancellable");
                    }
                    ctx.addImport(Cancellable.class.getName());
                    if (match.choice(0).equals("is not")) {
                        return "!((Cancellable) event).isCancelled()";
                    } else {
                        return "((Cancellable) event).isCancelled()";
                    }
                }));
    }
}


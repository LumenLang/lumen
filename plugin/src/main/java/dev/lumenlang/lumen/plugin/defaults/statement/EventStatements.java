package dev.lumenlang.lumen.plugin.defaults.statement;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.pattern.Categories;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

import static dev.lumenlang.lumen.api.pattern.LumaExample.of;
import static dev.lumenlang.lumen.api.pattern.LumaExample.secondly;
import static dev.lumenlang.lumen.api.pattern.LumaExample.top;

/**
 * Registers event-related statement patterns.
 */
@Registration
@SuppressWarnings("unused")
public final class EventStatements {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("cancel [the] event")
                .description("Cancels the current event, preventing its default action. Must be used inside an event handler block.")
                .example("cancel event")
                .since("1.0.0")
                .category(Categories.EVENT)
                .handler((line, ctx, out) -> {
                    if (ctx.block().getEnvFromParents("__event_block") == null) {
                        throw new RuntimeException("'cancel event' can only be used inside an event handler block");
                    }
                    Boolean cancellable = ctx.block().getEnvFromParents("__event_cancellable");
                    if (cancellable != null && !cancellable) {
                        throw new RuntimeException("'cancel event' cannot be used here because this event is not cancellable");
                    }
                    ctx.codegen().addImport(Cancellable.class.getName());
                    out.line("((Cancellable) event).setCancelled(true);");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("[with] (absolute_top|top|highest|high|mid|medium|low|lowest) priority")
                .description("Sets the event priority for this handler. Must be used directly inside an event block.")
                .example(of(
                        top("on join:"),
                        secondly("with high priority"),
                        secondly("message player \"Welcome!\"")))
                .since("1.0.0")
                .category(Categories.EVENT)
                .handler((line, ctx, out) -> {
                    Object eventMetaObj = ctx.block().getEnvFromParents("__event_meta");
                    if (eventMetaObj == null) {
                        throw new RuntimeException("'with priority' must be used directly inside an event block, not nested");
                    }
                    String keyword = ctx.choice(0);
                    String priority = switch (keyword) {
                        case "absolute_top" -> "MONITOR";
                        case "top", "highest" -> "HIGHEST";
                        case "high" -> "HIGH";
                        case "mid", "medium" -> "NORMAL";
                        case "low" -> "LOW";
                        case "lowest" -> "LOWEST";
                        default -> "NORMAL";
                    };
                    try {
                        eventMetaObj.getClass().getMethod("priority", String.class).invoke(eventMetaObj, priority);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to set event priority: " + e.getMessage(), e);
                    }
                }));
    }
}

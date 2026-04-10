package dev.lumenlang.lumen.plugin.defaults.statement;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.plugin.defaults.block.EventBlocks.EventMeta;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

import static dev.lumenlang.lumen.api.pattern.LumaExample.of;
import static dev.lumenlang.lumen.api.pattern.LumaExample.secondly;
import static dev.lumenlang.lumen.api.pattern.LumaExample.top;

/**
 * Registers event-related statement patterns.
 */
@Registration
@SuppressWarnings({"unused", "DataFlowIssue"})
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
                    EventMeta eventMeta = ctx.block().getEnvFromParents("__event_meta");
                    if (eventMeta == null) {
                        throw new RuntimeException("'with priority' must be used inside an event block");
                    }
                    String priority = switch (ctx.choice(0)) {
                        case "absolute_top" -> "MONITOR";
                        case "top", "highest" -> "HIGHEST";
                        case "high" -> "HIGH";
                        case "low" -> "LOW";
                        case "lowest" -> "LOWEST";
                        default -> "NORMAL";
                    };
                    eventMeta.priority(priority);
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("ignore [if] cancelled [already]")
                .description("Skips this handler if the event was already cancelled by another plugin or script before this handler runs.")
                .example(of(
                        top("on interact:"),
                        secondly("ignore if cancelled already"),
                        secondly("message player \"You interacted!\"")))
                .since("1.0.0")
                .category(Categories.EVENT)
                .handler((line, ctx, out) -> {
                    EventMeta eventMeta = ctx.block().getEnvFromParents("__event_meta");
                    if (eventMeta == null) {
                        throw new RuntimeException("'ignore cancelled' must be used inside an event block");
                    }
                    eventMeta.ignoreCancelled(true);
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("uncancel [the] event")
                .description("Uncancels the current event, reversing a previous cancellation. Must be used inside an event handler block.")
                .example("uncancel event")
                .since("1.0.0")
                .category(Categories.EVENT)
                .handler((line, ctx, out) -> {
                    if (ctx.block().getEnvFromParents("__event_block") == null) {
                        throw new RuntimeException("'uncancel event' can only be used inside an event handler block");
                    }
                    Boolean cancellable = ctx.block().getEnvFromParents("__event_cancellable");
                    if (cancellable != null && !cancellable) {
                        throw new RuntimeException("'uncancel event' cannot be used here because this event is not cancellable");
                    }
                    ctx.codegen().addImport(Cancellable.class.getName());
                    out.line("((Cancellable) event).setCancelled(false);");
                }));
    }
}

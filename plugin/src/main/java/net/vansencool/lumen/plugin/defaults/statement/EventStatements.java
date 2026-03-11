package net.vansencool.lumen.plugin.defaults.statement;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.pattern.Categories;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

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
                        throw new RuntimeException(
                                "'cancel event' can only be used inside an event handler block");
                    }
                    Boolean cancellable = ctx.block().getEnvFromParents("__event_cancellable");
                    if (cancellable != null && !cancellable) {
                        throw new RuntimeException(
                                "'cancel event' cannot be used here because this event is not cancellable");
                    }
                    ctx.codegen().addImport(Cancellable.class.getName());
                    out.line("((Cancellable) event).setCancelled(true);");
                }));
    }
}

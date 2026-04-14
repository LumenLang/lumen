package dev.lumenlang.lumen.plugin.defaults.statement;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.plugin.scheduler.ScriptScheduler;
import org.jetbrains.annotations.NotNull;

/**
 * Registers statements for managing Bukkit schedules at runtime.
 *
 * <p>Scripts can cancel any named schedule they own using
 * {@code cancel schedule "name"}, or cancel the current schedule from
 * within its own body using the {@code cancel} statement.
 */
@Registration
@SuppressWarnings("unused")
public final class ScheduleStatements {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("cancel schedule %name:STRING%")
                .description("Cancels a named schedule belonging to this script, does nothing if the schedule does not exist or is cancelled already.")
                .example("cancel schedule \"schedule_name\"")
                .since("1.0.0")
                .category(Categories.SCHEDULING)
                .handler(ctx -> {
                    ctx.codegen().addImport(ScriptScheduler.class.getName());
                    ctx.out().line("ScriptScheduler.cancelByName(this, "
                            + ctx.java("name") + ");");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("cancel")
                .description("Cancels the current schedule from within its body. This works for both named and unnamed schedules. Must be used inside a schedule block (wait, every, etc).")
                .example("cancel")
                .since("1.0.0")
                .category(Categories.SCHEDULING)
                .handler(ctx -> {
                    Boolean cancellable = ctx.block().getEnvFromParents("__cancellable_schedule");
                    if (cancellable == null || !cancellable) {
                        throw new RuntimeException(
                                "The cancel statement can only be used inside a schedule block (wait, every, etc)");
                    }
                    ctx.out().line("__cancelTask.run(); return;");
                }));
    }
}

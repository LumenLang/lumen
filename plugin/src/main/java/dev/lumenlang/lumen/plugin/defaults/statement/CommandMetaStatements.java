package dev.lumenlang.lumen.plugin.defaults.statement;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.plugin.commands.CommandMeta;
import org.jetbrains.annotations.NotNull;

/**
 * Registers command metadata statements used inside {@code command} blocks.
 *
 * <p>
 * These statements modify the {@link CommandMeta} stored in the enclosing
 * command
 * block's scoped environment. The command block handler stores the meta via
 * {@code block().putEnv("cmd_meta", meta)} in its {@code begin()}, and these
 * statements retrieve it via {@code block().getEnvFromParents("cmd_meta")}.
 */
@Registration
@SuppressWarnings("unused")
public final class CommandMetaStatements {

    private static @NotNull CommandMeta requireCommandMeta(@NotNull HandlerContext ctx) {
        CommandMeta cmd = ctx.block().getEnvFromParents("cmd_meta");
        if (cmd == null) {
            throw new RuntimeException("Command metadata statements used outside a 'command' block");
        }
        return cmd;
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("description[:] %d:STRING%")
                .description("Sets the description of a command.")
                .example("description: \"Teleports the player home\"")
                .since("1.0.0")
                .category(Categories.COMMAND)
                .handler(ctx -> {
                    CommandMeta cmd = requireCommandMeta(ctx);
                    cmd.setDescription(ctx.java("d"));
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("name[:] %n:STRING%")
                .description("Sets the primary name of a command, useful if you need to have the command name based on a variable or expression.")
                .example("name: tp")
                .since("1.0.0")
                .category(Categories.COMMAND)
                .handler(ctx -> {
                    CommandMeta cmd = requireCommandMeta(ctx);
                    cmd.setName(ctx.java("n"));
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("aliases[:] %list:LITERAL_LIST%")
                .description("Adds comma-separated aliases to a command.")
                .example("aliases: tp, teleport, goto")
                .since("1.0.0")
                .category(Categories.COMMAND)
                .handler(ctx -> {
                    CommandMeta cmd = requireCommandMeta(ctx);
                    // noinspection DataFlowIssue
                    String raw = ctx.value("list").toString();
                    for (String part : raw.split(",")) {
                        String alias = part.trim();
                        if (alias.isEmpty())
                            continue;
                        if (!alias.matches("[a-zA-Z0-9_\\-]+")) {
                            throw new IllegalArgumentException("Invalid alias '" + alias + "' - aliases may only contain letters, digits, underscores, and hyphens");
                        }
                        cmd.addAlias(alias);
                    }
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("namespace[:] %ns:STRING%")
                .description("Sets the namespace of a command.")
                .example("namespace: myplugin")
                .since("1.0.0")
                .category(Categories.COMMAND)
                .handler(ctx -> {
                    CommandMeta cmd = requireCommandMeta(ctx);
                    cmd.setNamespace(ctx.java("ns"));
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("permission[:] %perm:STRING%")
                .description("Sets the required permission for a command.")
                .example("permission: \"myplugin.admin\"")
                .since("1.0.0")
                .category(Categories.COMMAND)
                .handler(ctx -> {
                    CommandMeta cmd = requireCommandMeta(ctx);
                    cmd.setPermission(ctx.java("perm"));
                }));
    }
}

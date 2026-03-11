package net.vansencool.lumen.plugin.commands;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the metadata for a Lumen-defined command collected during script compilation.
 *
 * <p>Fields on this class are set by metadata statements inside a {@code command} block
 * (e.g. {@code description "..."}). The values are later read by the block handler's
 * {@code end} method to generate the command registration call.
 *
 * <p>All fields are mutable and default to {@code null} / empty as appropriate; the block
 * handler is responsible for supplying sensible defaults for any that remain unset.
 *
 * <p>Block handlers store this in block-scoped env (via {@code block().putEnv()}) so that
 * child statements can retrieve it with {@code block().getEnvFromParents()} and the
 * block's {@code end()} can read it with {@code block().getEnv()}.
 */
public final class CommandMeta {

    private final List<String> aliases = new ArrayList<>();
    private @NotNull String name;
    private @Nullable String namespace;
    private @Nullable String description;
    private @Nullable String permission;

    /**
     * Creates a new CommandMeta with the given primary command name.
     *
     * @param name the primary command name
     */
    public CommandMeta(@NotNull String name) {
        this.name = name;
    }

    /**
     * Returns the primary command name.
     *
     * @return the command name
     */
    public @NotNull String name() {
        return name;
    }

    /**
     * Sets the primary command name.
     *
     * @param name the new command name
     */
    public void setName(@NotNull String name) {
        this.name = name;
    }

    /**
     * Sets the command description shown in {@code /help}.
     *
     * @param description a Java expression for the description string
     */
    public void setDescription(@NotNull String description) {
        this.description = description;
    }

    /**
     * Returns the command description, or {@code null} if not set.
     *
     * @return the description expression, or {@code null}
     */
    public @Nullable String description() {
        return description;
    }

    /**
     * Adds an alias for this command.
     *
     * @param alias the alias name to add
     */
    public void addAlias(@NotNull String alias) {
        aliases.add(alias);
    }

    /**
     * Returns the list of registered aliases.
     *
     * @return an unmodifiable view of the aliases
     */
    public @NotNull List<String> aliases() {
        return Collections.unmodifiableList(aliases);
    }

    /**
     * Sets the namespace prefix for the command.
     *
     * @param namespace a Java expression for the namespace
     */
    public void setNamespace(@NotNull String namespace) {
        this.namespace = namespace;
    }

    /**
     * Returns the namespace prefix, or {@code null} if not set.
     *
     * @return the namespace expression, or {@code null}
     */
    public @Nullable String namespace() {
        return namespace;
    }

    /**
     * Sets the permission node required to execute this command.
     *
     * @param permission a Java expression for the permission string
     */
    public void setPermission(@NotNull String permission) {
        this.permission = permission;
    }

    /**
     * Returns the permission node, or {@code null} if not set.
     *
     * @return the permission expression, or {@code null}
     */
    public @Nullable String permission() {
        return permission;
    }
}

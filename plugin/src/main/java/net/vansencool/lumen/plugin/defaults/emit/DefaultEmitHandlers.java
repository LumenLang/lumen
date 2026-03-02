package net.vansencool.lumen.plugin.defaults.emit;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.emit.EmitRegistrar;
import org.jetbrains.annotations.NotNull;

/**
 * Registers all built-in emit handlers through the API.
 *
 * <p>Registration order for statement forms matters: global var must be checked before
 * store var, and store var before plain var, mirroring the priority of the
 * {@code StatementClassifier}.
 *
 * <p>This registration runs at order {@code -2000} so that emit handlers are available
 * before any pattern or type registrations that might depend on them.
 */
@Registration(order = -2000)
@Description("Registers built-in emit handlers: config blocks, data blocks, global vars, stored vars, plain vars, and global var loading")
@SuppressWarnings("unused")
public final class DefaultEmitHandlers {

    @Call
    public void register(@NotNull LumenAPI api) {
        EmitRegistrar emitters = api.emitters();

        emitters.blockForm(new ConfigBlockForm());
        emitters.blockForm(new DataBlockForm());

        emitters.statementForm(new GlobalVarStatementForm());
        emitters.statementForm(new StoreVarStatementForm());
        emitters.statementForm(new VarStatementForm());

        emitters.blockEnterHook(new GlobalVarLoadHook());
    }
}

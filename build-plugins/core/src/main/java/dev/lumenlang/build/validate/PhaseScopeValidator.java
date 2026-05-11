package dev.lumenlang.build.validate;

import dev.lumenlang.build.result.Diagnostic;
import dev.lumenlang.build.result.Severity;
import dev.lumenlang.build.scan.handler.ScannedHandler;
import dev.lumenlang.build.scan.param.InjectParam;
import dev.lumenlang.build.source.ParsedHandlerSource;
import dev.lumenlang.build.source.phase.Phase;
import dev.lumenlang.build.source.phase.PhaseMarker;
import dev.lumenlang.build.validate.scope.ScopeStack;
import dev.lumenlang.build.validate.walk.AstWalker;
import dev.lumenlang.build.validate.walk.PhaseLookup;
import net.vansencool.vanta.parser.ast.declaration.MethodDeclaration;
import net.vansencool.vanta.parser.ast.declaration.Parameter;
import net.vansencool.vanta.parser.ast.statement.Statement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Walks the handler method body and rejects {@code @Inject} parameter
 * references inside {@code // lumen:compile} sections and non-Inject
 * parameter references (like {@code HandlerContext}) inside
 * {@code // lumen:runtime} sections.
 */
public final class PhaseScopeValidator {

    private PhaseScopeValidator() {
    }

    public static @NotNull List<Diagnostic> validate(@NotNull ScannedHandler scanned, @NotNull ParsedHandlerSource parsed) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        MethodDeclaration method = parsed.method();
        if (method.body() == null) return diagnostics;

        int methodStart = method.line();
        int methodEnd = method.body().statements().isEmpty() ? methodStart : method.body().statements().get(method.body().statements().size() - 1).line();
        int compileCount = 0;
        int runtimeCount = 0;
        for (PhaseMarker m : parsed.markers()) {
            if (m.line() < methodStart || m.line() > methodEnd) continue;
            if (m.phase() == Phase.COMPILE) compileCount++;
            else if (m.phase() == Phase.RUNTIME) runtimeCount++;
        }
        if (compileCount > 1) {
            diagnostics.add(new Diagnostic(Severity.ERROR, "Handler '" + method.name() + "' declares " + compileCount + " // lumen:compile sections; collapse them into one", parsed.sourceFile(), methodStart, 0));
        }
        if (runtimeCount > 1) {
            diagnostics.add(new Diagnostic(Severity.ERROR, "Handler '" + method.name() + "' declares " + runtimeCount + " // lumen:runtime sections; collapse them into one", parsed.sourceFile(), methodStart, 0));
        }

        Set<String> injectNames = new HashSet<>();
        for (InjectParam p : scanned.injectParams()) injectNames.add(p.placeholderName());

        Set<String> nonInjectParamNames = new HashSet<>();
        for (Parameter p : method.parameters()) {
            if (!injectNames.contains(p.name())) nonInjectParamNames.add(p.name());
        }

        ScopeStack scopes = new ScopeStack();
        for (Parameter p : method.parameters()) scopes.declare(p.name());

        PhaseLookup phaseLookup = new PhaseLookup(parsed.markers());

        AstWalker walker = new AstWalker(
                phaseLookup,
                scopes,
                (name, line, phase) -> {
                    if (phase == Phase.COMPILE && injectNames.contains(name)) {
                        diagnostics.add(new Diagnostic(Severity.ERROR, "'" + name + "' is an @Inject parameter and cannot be used inside a // lumen:compile section", parsed.sourceFile(), line, 0));
                    } else if (phase == Phase.RUNTIME && nonInjectParamNames.contains(name)) {
                        diagnostics.add(new Diagnostic(Severity.ERROR, "'" + name + "' is a parse-time-only parameter and cannot be used inside a // lumen:runtime section", parsed.sourceFile(), line, 0));
                    }
                },
                (construct, line) -> diagnostics.add(new Diagnostic(Severity.ERROR, construct + " is not supported inside an injectable handler body. Extract it to a top-level class.", parsed.sourceFile(), line, 0))
        );

        for (Statement s : method.body().statements()) walker.walkStatement(s);
        return diagnostics;
    }
}

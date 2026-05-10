package dev.lumenlang.build.source;

import dev.lumenlang.build.source.phase.PhaseMarker;
import dev.lumenlang.build.source.phase.PhaseMarkerScanner;
import net.vansencool.vanta.lexer.Lexer;
import net.vansencool.vanta.lexer.token.Token;
import net.vansencool.vanta.parser.Parser;
import net.vansencool.vanta.parser.ast.AstNode;
import net.vansencool.vanta.parser.ast.declaration.ClassDeclaration;
import net.vansencool.vanta.parser.ast.declaration.CompilationUnit;
import net.vansencool.vanta.parser.ast.declaration.MethodDeclaration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Lexes and parses a handler's source file with Vanta, then locates the
 * handler {@link MethodDeclaration} by class name, method name, and
 * parameter arity.
 */
public final class HandlerSourceParser {

    private HandlerSourceParser() {
    }

    /**
     * @param sourceFile      absolute path to the handler's source file
     * @param simpleClassName simple name of the class declaring the handler
     * @param methodName      the handler method's identifier
     * @param parameterCount  number of declared parameters on the handler
     * @return the parsed result, or null when the source can't be matched
     * @throws IOException when the file cannot be read
     */
    public static @Nullable ParsedHandlerSource parse(@NotNull Path sourceFile, @NotNull String simpleClassName, @NotNull String methodName, int parameterCount) throws IOException {
        String sourceText = Files.readString(sourceFile);
        List<PhaseMarker> markers = PhaseMarkerScanner.scan(sourceText);

        List<Token> tokens = new Lexer(sourceText).tokenize();
        CompilationUnit unit = new Parser(tokens).parse();

        ClassDeclaration owner = findClass(unit, simpleClassName);
        if (owner == null) return null;

        MethodDeclaration method = findMethod(owner, methodName, parameterCount);
        if (method == null) return null;

        return new ParsedHandlerSource(sourceFile, sourceText, unit, method, markers);
    }

    private static @Nullable ClassDeclaration findClass(@NotNull CompilationUnit unit, @NotNull String simpleName) {
        for (AstNode decl : unit.typeDeclarations()) {
            if (decl instanceof ClassDeclaration cd && simpleName.equals(cd.name())) return cd;
        }
        for (AstNode decl : unit.typeDeclarations()) {
            if (decl instanceof ClassDeclaration cd) {
                ClassDeclaration nested = findClassRecursive(cd, simpleName);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static @Nullable ClassDeclaration findClassRecursive(@NotNull ClassDeclaration cd, @NotNull String simpleName) {
        if (simpleName.equals(cd.name())) return cd;
        for (AstNode member : cd.members()) {
            if (member instanceof ClassDeclaration inner) {
                ClassDeclaration found = findClassRecursive(inner, simpleName);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static @Nullable MethodDeclaration findMethod(@NotNull ClassDeclaration owner, @NotNull String methodName, int parameterCount) {
        for (AstNode member : owner.members()) {
            if (member instanceof MethodDeclaration md && methodName.equals(md.name()) && md.parameters().size() == parameterCount) {
                return md;
            }
        }
        return null;
    }
}

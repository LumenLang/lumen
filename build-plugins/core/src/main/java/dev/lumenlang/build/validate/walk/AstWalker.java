package dev.lumenlang.build.validate.walk;

import dev.lumenlang.build.validate.scope.ScopeStack;
import dev.lumenlang.build.validate.walk.sink.BannedConstructSink;
import dev.lumenlang.build.validate.walk.sink.NameRefSink;
import net.vansencool.vanta.parser.ast.declaration.Parameter;
import net.vansencool.vanta.parser.ast.expression.ArrayAccessExpression;
import net.vansencool.vanta.parser.ast.expression.ArrayInitializerExpression;
import net.vansencool.vanta.parser.ast.expression.AssignmentExpression;
import net.vansencool.vanta.parser.ast.expression.BinaryExpression;
import net.vansencool.vanta.parser.ast.expression.CastExpression;
import net.vansencool.vanta.parser.ast.expression.Expression;
import net.vansencool.vanta.parser.ast.expression.FieldAccessExpression;
import net.vansencool.vanta.parser.ast.expression.InstanceofExpression;
import net.vansencool.vanta.parser.ast.expression.LambdaExpression;
import net.vansencool.vanta.parser.ast.expression.LiteralExpression;
import net.vansencool.vanta.parser.ast.expression.MethodCallExpression;
import net.vansencool.vanta.parser.ast.expression.MethodReferenceExpression;
import net.vansencool.vanta.parser.ast.expression.NameExpression;
import net.vansencool.vanta.parser.ast.expression.NewArrayExpression;
import net.vansencool.vanta.parser.ast.expression.NewExpression;
import net.vansencool.vanta.parser.ast.expression.ParenExpression;
import net.vansencool.vanta.parser.ast.expression.SuperExpression;
import net.vansencool.vanta.parser.ast.expression.SwitchExpression;
import net.vansencool.vanta.parser.ast.expression.TernaryExpression;
import net.vansencool.vanta.parser.ast.expression.ThisExpression;
import net.vansencool.vanta.parser.ast.expression.UnaryExpression;
import net.vansencool.vanta.parser.ast.statement.AssertStatement;
import net.vansencool.vanta.parser.ast.statement.BlockStatement;
import net.vansencool.vanta.parser.ast.statement.BreakStatement;
import net.vansencool.vanta.parser.ast.statement.CatchClause;
import net.vansencool.vanta.parser.ast.statement.ContinueStatement;
import net.vansencool.vanta.parser.ast.statement.DoWhileStatement;
import net.vansencool.vanta.parser.ast.statement.ExpressionStatement;
import net.vansencool.vanta.parser.ast.statement.ForEachStatement;
import net.vansencool.vanta.parser.ast.statement.ForStatement;
import net.vansencool.vanta.parser.ast.statement.IfStatement;
import net.vansencool.vanta.parser.ast.statement.LabeledStatement;
import net.vansencool.vanta.parser.ast.statement.ResourceDeclaration;
import net.vansencool.vanta.parser.ast.statement.ReturnStatement;
import net.vansencool.vanta.parser.ast.statement.Statement;
import net.vansencool.vanta.parser.ast.statement.SwitchCase;
import net.vansencool.vanta.parser.ast.statement.SwitchStatement;
import net.vansencool.vanta.parser.ast.statement.SynchronizedStatement;
import net.vansencool.vanta.parser.ast.statement.ThrowStatement;
import net.vansencool.vanta.parser.ast.statement.TryStatement;
import net.vansencool.vanta.parser.ast.statement.VariableDeclarationStatement;
import net.vansencool.vanta.parser.ast.statement.VariableDeclarator;
import net.vansencool.vanta.parser.ast.statement.WhileStatement;
import net.vansencool.vanta.parser.ast.statement.YieldStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Recursive walker over Vanta's statement and expression AST that emits a
 * {@link NameRefSink} call for every free {@link NameExpression}. Tracks
 * lexical name shadowing through method params, blocks, lambdas, for-loops,
 * catch clauses, and try-with-resources. Anonymous and local class
 * declarations are reported through {@link BannedConstructSink} and not
 * descended into.
 */
public final class AstWalker {

    private final @NotNull PhaseLookup phaseLookup;
    private final @NotNull ScopeStack scopes;
    private final @NotNull NameRefSink names;
    private final @NotNull BannedConstructSink banned;

    public AstWalker(@NotNull PhaseLookup phaseLookup, @NotNull ScopeStack scopes, @NotNull NameRefSink names, @NotNull BannedConstructSink banned) {
        this.phaseLookup = phaseLookup;
        this.scopes = scopes;
        this.names = names;
        this.banned = banned;
    }

    public void walkStatement(@NotNull Statement statement) {
        if (statement instanceof BlockStatement b) {
            scopes.push();
            for (Statement s : b.statements()) walkStatement(s);
            scopes.pop();
        } else if (statement instanceof ExpressionStatement e) {
            walkExpression(e.expression());
        } else if (statement instanceof VariableDeclarationStatement v) {
            for (VariableDeclarator d : v.declarators()) {
                if (d.initializer() != null) walkExpression(d.initializer());
                scopes.declare(d.name());
            }
        } else if (statement instanceof IfStatement i) {
            walkExpression(i.condition());
            walkStatement(i.thenBranch());
            if (i.elseBranch() != null) walkStatement(i.elseBranch());
        } else if (statement instanceof WhileStatement w) {
            walkExpression(w.condition());
            walkStatement(w.body());
        } else if (statement instanceof DoWhileStatement d) {
            walkStatement(d.body());
            walkExpression(d.condition());
        } else if (statement instanceof ForStatement f) {
            scopes.push();
            if (f.initializers() != null) for (Statement init : f.initializers()) walkStatement(init);
            if (f.condition() != null) walkExpression(f.condition());
            if (f.updaters() != null) for (Expression u : f.updaters()) walkExpression(u);
            walkStatement(f.body());
            scopes.pop();
        } else if (statement instanceof ForEachStatement fe) {
            walkExpression(fe.iterable());
            scopes.push();
            scopes.declare(fe.variableName());
            walkStatement(fe.body());
            scopes.pop();
        } else if (statement instanceof ReturnStatement r) {
            if (r.value() != null) walkExpression(r.value());
        } else if (statement instanceof ThrowStatement t) {
            walkExpression(t.expression());
        } else if (statement instanceof TryStatement t) {
            scopes.push();
            for (ResourceDeclaration res : t.resources()) {
                walkExpression(res.initializer());
                scopes.declare(res.name());
            }
            walkStatement(t.tryBlock());
            scopes.pop();
            for (CatchClause c : t.catchClauses()) {
                scopes.push();
                scopes.declare(c.variableName());
                walkStatement(c.body());
                scopes.pop();
            }
            if (t.finallyBlock() != null) walkStatement(t.finallyBlock());
        } else if (statement instanceof SwitchStatement s) {
            walkExpression(s.selector());
            for (SwitchCase c : s.cases()) walkSwitchCase(c);
        } else if (statement instanceof SynchronizedStatement s) {
            walkExpression(s.lock());
            walkStatement(s.body());
        } else if (statement instanceof AssertStatement a) {
            walkExpression(a.condition());
            if (a.message() != null) walkExpression(a.message());
        } else if (statement instanceof YieldStatement y) {
            walkExpression(y.value());
        } else if (statement instanceof LabeledStatement l) {
            walkStatement(l.statement());
        } else if (statement instanceof BreakStatement || statement instanceof ContinueStatement) {
            // no-op
        } else {
            throw new IllegalStateException("Unhandled statement type: " + statement.getClass().getName());
        }
    }

    public void walkExpression(@NotNull Expression expression) {
        if (expression instanceof NameExpression n) {
            if (!scopes.shadows(n.name())) {
                names.accept(n.name(), n.line(), phaseLookup.phaseAt(n.line()));
            }
        } else if (expression instanceof BinaryExpression b) {
            walkExpression(b.left());
            walkExpression(b.right());
        } else if (expression instanceof UnaryExpression u) {
            walkExpression(u.operand());
        } else if (expression instanceof AssignmentExpression a) {
            walkExpression(a.target());
            walkExpression(a.value());
        } else if (expression instanceof TernaryExpression t) {
            walkExpression(t.condition());
            walkExpression(t.thenExpression());
            walkExpression(t.elseExpression());
        } else if (expression instanceof MethodCallExpression mc) {
            if (mc.target() != null) walkExpression(mc.target());
            for (Expression arg : mc.arguments()) walkExpression(arg);
        } else if (expression instanceof FieldAccessExpression fa) {
            walkExpression(fa.target());
        } else if (expression instanceof ArrayAccessExpression aa) {
            walkExpression(aa.array());
            walkExpression(aa.index());
        } else if (expression instanceof CastExpression c) {
            walkExpression(c.expression());
        } else if (expression instanceof InstanceofExpression i) {
            walkExpression(i.expression());
        } else if (expression instanceof ParenExpression p) {
            walkExpression(p.expression());
        } else if (expression instanceof NewExpression n) {
            for (Expression arg : n.arguments()) walkExpression(arg);
            if (n.anonymousClassBody() != null) {
                banned.accept("anonymous class body", n.line());
            }
        } else if (expression instanceof NewArrayExpression na) {
            for (Expression d : na.dimensionExpressions()) walkExpression(d);
            if (na.initializer() != null) walkExpression(na.initializer());
        } else if (expression instanceof ArrayInitializerExpression ai) {
            for (Expression e : ai.elements()) walkExpression(e);
        } else if (expression instanceof LambdaExpression l) {
            scopes.push();
            for (Parameter p : l.parameters()) scopes.declare(p.name());
            if (l.body() != null) walkStatement(l.body());
            if (l.expressionBody() != null) walkExpression(l.expressionBody());
            scopes.pop();
        } else if (expression instanceof MethodReferenceExpression mr) {
            walkExpression(mr.target());
        } else if (expression instanceof SwitchExpression sw) {
            walkExpression(sw.selector());
            for (SwitchCase c : sw.cases()) walkSwitchCase(c);
        } else if (expression instanceof LiteralExpression || expression instanceof ThisExpression || expression instanceof SuperExpression) {
            // no-op
        } else {
            throw new IllegalStateException("Unhandled expression type: " + expression.getClass().getName());
        }
    }

    private void walkSwitchCase(@NotNull SwitchCase c) {
        scopes.push();
        @Nullable List<Expression> labels = c.labels();
        if (labels != null) for (Expression label : labels) walkExpression(label);
        for (Statement s : c.statements()) walkStatement(s);
        scopes.pop();
    }
}

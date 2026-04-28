package dev.lumenlang.lumen.plugin.defaults.block;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.BlockAccess;
import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.codegen.NarrowingFact;
import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.handler.BlockHandler;
import dev.lumenlang.lumen.api.pattern.Categories;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static dev.lumenlang.lumen.api.pattern.LumaExample.of;
import static dev.lumenlang.lumen.api.pattern.LumaExample.secondly;
import static dev.lumenlang.lumen.api.pattern.LumaExample.thirdly;
import static dev.lumenlang.lumen.api.pattern.LumaExample.top;

/**
 * Registers control flow block handlers.
 */
@Registration
@SuppressWarnings("unused")
public final class ControlBlocks {

    private static final String BRANCH_FACTS_KEY = "__if_branch_facts";
    private static final String CHAIN_STACK_KEY = "__if_chain_stack";

    private static @NotNull Deque<List<NarrowingFact>> chainStack(@NotNull HandlerContext ctx) {
        Deque<List<NarrowingFact>> stack = ctx.env().get(CHAIN_STACK_KEY);
        if (stack == null) {
            stack = new ArrayDeque<>();
            ctx.env().put(CHAIN_STACK_KEY, stack);
        }
        return stack;
    }

    private static void pushChain(@NotNull HandlerContext ctx, @NotNull List<NarrowingFact> facts) {
        chainStack(ctx).push(new ArrayList<>(facts));
    }

    private static void appendToChain(@NotNull HandlerContext ctx, @NotNull List<NarrowingFact> facts) {
        Deque<List<NarrowingFact>> stack = chainStack(ctx);
        if (stack.isEmpty()) stack.push(new ArrayList<>());
        stack.peek().addAll(facts);
    }

    private static @Nullable List<NarrowingFact> peekChain(@NotNull HandlerContext ctx) {
        Deque<List<NarrowingFact>> stack = chainStack(ctx);
        return stack.isEmpty() ? null : stack.peek();
    }

    private static void popChain(@NotNull HandlerContext ctx) {
        Deque<List<NarrowingFact>> stack = chainStack(ctx);
        if (!stack.isEmpty()) stack.pop();
    }

    private static boolean nextIsElseChain(@NotNull HandlerContext ctx) {
        if (!ctx.block().hasNext()) return false;
        String nextRaw = ctx.block().nextRaw().trim().toLowerCase();
        return nextRaw.startsWith("else if") || nextRaw.startsWith("else ") || nextRaw.startsWith("else:");
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("if %cond:COND%")
                .description("Conditionally executes a block of code. When used at the top level, wraps the block in a preload method that runs on script load.")
                .example(of(
                        top("on join:"),
                        secondly("if player is sneaking:"),
                        thirdly("send player \"You are sneaking!\"")))
                .since("1.0.0")
                .category(Categories.CONTROL_FLOW)
                .supportsRootLevel(true)
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull HandlerContext ctx) {
                        if (ctx.block().isRoot()) {
                            int methodId = ctx.codegen().nextMethodId();
                            ctx.out().line("@LumenPreload");
                            ctx.out().line("public void __lumen_if_" + methodId + "() {");
                        }
                        ctx.out().line("if (" + ctx.parseCondition("cond") + ") {");
                        List<NarrowingFact> facts = ctx.env().consumeNarrowings();
                        ctx.env().applyNarrowings(facts);
                        ctx.block().putEnv(BRANCH_FACTS_KEY, facts);
                    }

                    @Override
                    public void end(@NotNull HandlerContext ctx) {
                        List<NarrowingFact> branchFacts = ctx.block().getEnv(BRANCH_FACTS_KEY);
                        if (branchFacts != null) {
                            ctx.env().clearNarrowings(branchFacts);
                            pushChain(ctx, branchFacts);
                        }
                        ctx.out().line("}");
                        if (!nextIsElseChain(ctx)) {
                            popChain(ctx);
                        }
                        if (ctx.block().isRoot()) {
                            ctx.out().line("}");
                        }
                    }
                }));

        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("else if %cond:COND%")
                .description("An alternative condition branch that follows an if or else if block.")
                .example(of(
                        top("on join:"),
                        secondly("if player is sneaking:"),
                        thirdly("send player \"You are sneaking!\""),
                        secondly("else if player is sprinting:"),
                        thirdly("send player \"You are sprinting!\"")))
                .since("1.0.0")
                .category(Categories.CONTROL_FLOW)
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull HandlerContext ctx) {
                        validateElseBranch(ctx, "else if");
                        ctx.out().line("else if (" + ctx.parseCondition("cond") + ") {");
                        List<NarrowingFact> facts = ctx.env().consumeNarrowings();
                        ctx.env().applyNarrowings(facts);
                        ctx.block().putEnv(BRANCH_FACTS_KEY, facts);
                    }

                    @Override
                    public void end(@NotNull HandlerContext ctx) {
                        List<NarrowingFact> branchFacts = ctx.block().getEnv(BRANCH_FACTS_KEY);
                        if (branchFacts != null) {
                            ctx.env().clearNarrowings(branchFacts);
                            appendToChain(ctx, branchFacts);
                        }
                        ctx.out().line("}");
                        if (!nextIsElseChain(ctx)) {
                            popChain(ctx);
                        }
                    }
                }));

        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("else")
                .description("The fallback branch that executes when no previous if/else if matched.")
                .example(of(
                        top("on join:"),
                        secondly("if player is sneaking:"),
                        thirdly("send player \"You are sneaking!\""),
                        secondly("else:"),
                        thirdly("send player \"Nothing matched\"")))
                .since("1.0.0")
                .category(Categories.CONTROL_FLOW)
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull HandlerContext ctx) {
                        validateElseBranch(ctx, "else");
                        ctx.out().line("else {");
                        List<NarrowingFact> chain = peekChain(ctx);
                        if (chain != null && !chain.isEmpty()) {
                            List<NarrowingFact> inverted = new ArrayList<>(chain.size());
                            for (NarrowingFact f : chain) inverted.add(f.inverted());
                            ctx.env().applyNarrowings(inverted);
                            ctx.block().putEnv(BRANCH_FACTS_KEY, inverted);
                        }
                    }

                    @Override
                    public void end(@NotNull HandlerContext ctx) {
                        List<NarrowingFact> applied = ctx.block().getEnv(BRANCH_FACTS_KEY);
                        if (applied != null) ctx.env().clearNarrowings(applied);
                        popChain(ctx);
                        ctx.out().line("}");
                    }
                }));

        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("repeat %n:INT% [time|times]")
                .description("Repeats the enclosed block a fixed number of times.")
                .example(of(
                        top("repeat 5 times:"),
                        secondly("broadcast \"Hello!\"")))
                .since("1.0.0")
                .category(Categories.CONTROL_FLOW)
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull HandlerContext ctx) {
                        if (ctx.block().isRoot()) {
                            throw new DiagnosticException(LumenDiagnostic.error("Invalid 'repeat' placement")
                                    .at(ctx.block().line(), ctx.block().raw())
                                    .label("cannot be top-level")
                                    .help("a 'repeat' block must be inside an event or command handler")
                                    .build());
                        }
                        ctx.out().line("for (int __i = 0; __i < " + ctx.java("n") + "; __i++) {");
                    }

                    @Override
                    public void end(@NotNull HandlerContext ctx) {
                        ctx.out().line("}");
                    }
                }));
    }

    private static void validateElseBranch(@NotNull HandlerContext ctx, @NotNull String keyword) {
        BlockAccess block = ctx.block();
        if (block.isRoot()) {
            throw new DiagnosticException(LumenDiagnostic.error("'" + keyword + "' without matching 'if'")
                    .at(block.line(), block.raw())
                    .label("cannot be top-level without a preceding 'if'")
                    .help("'" + keyword + "' must directly follow an 'if' or 'else if' block with the same indentation")
                    .build());
        }
        if (block.isFirst() || (!block.prevHeadEquals("if") && !block.prevHeadEquals("else"))) {
            LumenDiagnostic.Builder diag = LumenDiagnostic.error("'" + keyword + "' without matching 'if'")
                    .at(block.line(), block.raw())
                    .label("no preceding 'if' or 'else if' block at the same indentation level");
            BlockAccess.SiblingInfo nearestIf = block.findSiblingBlock("if");
            if (nearestIf != null) {
                diag.note("found 'if' at line " + nearestIf.line() + ", but it is not the direct predecessor");
            }
            diag.help("'" + keyword + "' must directly follow an 'if' or 'else if' block with the same indentation");
            throw new DiagnosticException(diag.build());
        }
        if (block.prevHeadExact("else")) {
            LumenDiagnostic.Builder diag = LumenDiagnostic.error("'" + keyword + "' after 'else'")
                    .at(block.line(), block.raw())
                    .label("cannot follow an 'else' block");
            diag.context(block.prevLine(), block.prevRaw(), 0, block.prevRaw().stripTrailing().length(), "this is already the fallback branch");
            diag.help("'" + keyword + "' cannot come after 'else' because 'else' is the final branch");
            throw new DiagnosticException(diag.build());
        }
    }
}

package dev.lumenlang.lumen.plugin.defaults.block;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.BindingAccess;
import dev.lumenlang.lumen.api.codegen.BlockAccess;
import dev.lumenlang.lumen.api.codegen.JavaOutput;
import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.handler.BlockHandler;
import dev.lumenlang.lumen.api.pattern.Categories;
import org.jetbrains.annotations.NotNull;

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

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("if %cond:EXPR%")
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
                    public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        if (ctx.block().isRoot()) {
                            int lineNum = out.lineNum();
                            out.line("@LumenPreload");
                            out.line("public void __lumen_if_" + lineNum + "() {");
                        }
                        out.line("if (" + ctx.parseCondition("cond") + ") {");
                    }

                    @Override
                    public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        out.line("}");
                        if (ctx.block().isRoot()) {
                            out.line("}");
                        }
                    }
                }));

        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("else if %cond:EXPR%")
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
                    public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        validateElseBranch(ctx, "else if");
                        out.line("else if (" + ctx.parseCondition("cond") + ") {");
                    }

                    @Override
                    public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        out.line("}");
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
                    public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        validateElseBranch(ctx, "else");
                        out.line("else {");
                    }

                    @Override
                    public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        out.line("}");
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
                    public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        if (ctx.block().isRoot()) {
                            throw new DiagnosticException(LumenDiagnostic.error("E500", "Invalid 'repeat' placement")
                                    .at(ctx.block().line(), ctx.block().raw())
                                    .label("cannot be top-level")
                                    .help("a 'repeat' block must be inside an event or command handler")
                                    .build());
                        }
                        out.line("for (int __i = 0; __i < " + ctx.java("n") + "; __i++) {");
                    }

                    @Override
                    public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        out.line("}");
                    }
                }));
    }

    private static void validateElseBranch(@NotNull BindingAccess ctx, @NotNull String keyword) {
        BlockAccess block = ctx.block();
        if (block.isRoot()) {
            throw new DiagnosticException(LumenDiagnostic.error("E500", "'" + keyword + "' without matching 'if'")
                    .at(block.line(), block.raw())
                    .label("cannot be top-level without a preceding 'if'")
                    .help("'" + keyword + "' must directly follow an 'if' or 'else if' block with the same indentation")
                    .build());
        }
        if (block.isFirst() || (!block.prevHeadEquals("if") && !block.prevHeadEquals("else"))) {
            LumenDiagnostic.Builder diag = LumenDiagnostic.error("E500", "'" + keyword + "' without matching 'if'")
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
            LumenDiagnostic.Builder diag = LumenDiagnostic.error("E500", "'" + keyword + "' after 'else'")
                    .at(block.line(), block.raw())
                    .label("cannot follow an 'else' block");
            diag.context(block.prevLine(), block.prevRaw(), 0, block.prevRaw().stripTrailing().length(), "this is already the fallback branch");
            diag.help("'" + keyword + "' cannot come after 'else' because 'else' is the final branch");
            throw new DiagnosticException(diag.build());
        }
    }
}

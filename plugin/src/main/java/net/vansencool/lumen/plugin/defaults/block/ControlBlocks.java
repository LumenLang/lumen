package net.vansencool.lumen.plugin.defaults.block;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.codegen.BindingAccess;
import net.vansencool.lumen.api.codegen.JavaOutput;
import net.vansencool.lumen.api.handler.BlockHandler;
import net.vansencool.lumen.api.pattern.Categories;
import org.jetbrains.annotations.NotNull;

import static net.vansencool.lumen.api.pattern.LumaExample.of;
import static net.vansencool.lumen.api.pattern.LumaExample.secondly;
import static net.vansencool.lumen.api.pattern.LumaExample.thirdly;
import static net.vansencool.lumen.api.pattern.LumaExample.top;

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
                        if (ctx.block().isRoot()) {
                            throw new RuntimeException(
                                    "An 'else if' block cannot be top-level (it must follow an 'if' or 'else if' block)");
                        }
                        if (ctx.block().isFirst()) {
                            throw new RuntimeException(
                                    "An 'else if' block cannot be the first block among its siblings (it must come after an 'if' or 'else if' block)");
                        }
                        if (ctx.block().prevHeadExact("else")) {
                            throw new RuntimeException("An 'else if' block cannot come after an 'else' block");
                        }
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
                        if (ctx.block().isRoot()) {
                            throw new RuntimeException(
                                    "An 'else' block cannot be top-level (it must follow an 'if' or 'else if' block)");
                        }
                        if (ctx.block().isFirst()) {
                            throw new RuntimeException(
                                    "An 'else' block cannot be the first block among its siblings (it must come after an 'if' or 'else if' block)");
                        }
                        if (ctx.block().prevHeadExact("else")) {
                            throw new RuntimeException("An 'else' block cannot come after an 'else' block");
                        }
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
                            throw new RuntimeException("A 'repeat' block cannot be top-level");
                        }
                        out.line("for (int __i = 0; __i < " + ctx.java("n") + "; __i++) {");
                    }

                    @Override
                    public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        out.line("}");
                    }
                }));
    }
}

package dev.lumenlang.lumen.plugin.defaults.expression;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.BindingAccess;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.Types;
import dev.lumenlang.lumen.api.type.BuiltinLumenTypes;
import dev.lumenlang.lumen.pipeline.data.DataSchema;
import dev.lumenlang.lumen.pipeline.java.compiled.DataInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Registers data class expression patterns for construction and field access.
 */
@Registration
@SuppressWarnings("unused")
// TODO: Migrate this to use the new Type System
public final class DataExpressions {

    /**
     * Attempts to coerce the raw {@code DataInstance.get()} result based on the
     * compile-time data schema when both the object's data type and the field name
     * are known.
     *
     * <p>This inspects the {@code obj} parameter tokens, looks up the variable's
     * {@code data_type} metadata, retrieves the matching {@link DataSchema}, and
     * wraps the expression in the field type's coercion template (e.g. wrapping
     * with {@code Coerce.toDouble()}).
     *
     * <p>If the data type or field name cannot be determined at compile time, the
     * raw {@code Object} value is returned without coercion.
     *
     * @param ctx    the binding access for the current pattern match
     * @param rawGet the Java expression for the raw {@code DataInstance.get()} call
     * @return an expression result with the coerced (or raw) Java expression
     */
    private static @NotNull ExpressionResult coerceFieldResult(@NotNull BindingAccess ctx,
                                                               @NotNull String rawGet) {
        DataSchema.FieldType fieldType = resolveFieldType(ctx);
        if (fieldType != null && fieldType != DataSchema.FieldType.ANY) {
            return new ExpressionResult(fieldType.coerce(rawGet), null);
        }
        return new ExpressionResult(rawGet, null);
    }

    /**
     * Resolves the {@link DataSchema.FieldType} for the current field access expression
     * by inspecting the object variable's metadata and the field name tokens.
     *
     * @param ctx the binding access
     * @return the field type, or {@code null} if it cannot be determined at compile time
     */
    private static @Nullable DataSchema.FieldType resolveFieldType(@NotNull BindingAccess ctx) {
        List<String> objTokens = ctx.tokens("obj");
        if (objTokens.size() != 1) return null;

        EnvironmentAccess.VarHandle varHandle = ctx.env().lookupVar(objTokens.get(0));
        if (varHandle == null || !varHandle.hasMeta("data_type")) return null;

        String dataType = String.valueOf(varHandle.meta("data_type"));
        DataSchema schema = ctx.env().get("data_schema_" + dataType);
        if (schema == null) return null;

        List<String> fieldTokens = ctx.tokens("field");
        if (fieldTokens.size() != 1) return null;

        return schema.fields().get(fieldTokens.get(0));
    }

    /**
     * Resolves a single token to a Java expression. Checks for string literals,
     * numeric literals, boolean literals, and variable references.
     */
    private static @NotNull String resolveValueToken(@NotNull String token,
                                                     @NotNull EnvironmentAccess env) {
        if (token.startsWith("\"") && token.endsWith("\"")) {
            String inner = token.substring(1, token.length() - 1);
            if (inner.contains("{") && inner.contains("}")) {
                return env.expandPlaceholders(inner);
            }
            return "\"" + inner + "\"";
        }

        if (token.contains("{") && token.contains("}")) {
            return env.expandPlaceholders(token);
        }

        if (token.equalsIgnoreCase("true") || token.equalsIgnoreCase("false")) {
            return token.toLowerCase();
        }

        try {
            Integer.parseInt(token);
            return token;
        } catch (NumberFormatException ignored) {
        }

        try {
            Double.parseDouble(token);
            return token;
        } catch (NumberFormatException ignored) {
        }

        EnvironmentAccess.VarHandle ref = env.lookupVar(token);
        if (ref != null) {
            return ref.java();
        }

        return "\"" + token + "\"";
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        registerConstructor(api);
        registerFieldAccess(api);
    }

    /**
     * Registers the data constructor expression.
     *
     * <p>Syntax: {@code new <type>} or {@code new <type> with field1 val1 field2 val2 ...}
     * <p>The field-value pairs after "with" can use literal values or variable references.
     */
    private void registerConstructor(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("new %body:EXPR%")
                .description("Creates a new data instance. Use 'with' to set fields: new arena with name \"x\" x1 5")
                .examples("set a to new arena",
                        "set a to new arena with name \"PvP\" x1 0 y1 0 z1 0 x2 100 y2 100 z2 100")
                .since("1.0.0")
                .category(Categories.DATA)
                .handler(ctx -> {
                    List<String> tokens = ctx.tokens("body");
                    if (tokens.isEmpty()) {
                        throw new RuntimeException("Expected a data type name after 'new'");
                    }

                    String typeName = tokens.get(0).toLowerCase();
                    DataSchema schema = ctx.env().get("data_schema_" + typeName);
                    if (schema == null) {
                        throw new RuntimeException("Unknown data type: " + tokens.get(0));
                    }

                    ctx.codegen().addImport(DataInstance.class.getName());
                    ctx.codegen().addImport(LinkedHashMap.class.getName());
                    ctx.codegen().addImport(Map.class.getName());

                    if (tokens.size() == 1) {
                        return new ExpressionResult(
                                "new DataInstance(\"" + typeName + "\")",
                                BuiltinLumenTypes.DATA.id(),
                                Map.of("data_type", typeName));
                    }

                    if (tokens.size() < 2 || !tokens.get(1).equalsIgnoreCase("with")) {
                        throw new RuntimeException("Expected 'with' after data type name, got: " + tokens.get(1));
                    }

                    List<String> fieldTokens = tokens.subList(2, tokens.size());
                    StringBuilder mapBuilder = new StringBuilder("new LinkedHashMap<>(Map.of(");
                    StringJoiner entries = new StringJoiner(", ");
                    EnvironmentAccess env = ctx.env();

                    int i = 0;
                    while (i < fieldTokens.size()) {
                        if (i + 1 >= fieldTokens.size()) {
                            throw new RuntimeException("Expected a value after field name '" + fieldTokens.get(i) + "'");
                        }

                        String fieldName = fieldTokens.get(i);

                        if (!schema.fields().containsKey(fieldName)) {
                            throw new RuntimeException("Unknown field '" + fieldName
                                    + "' in data type '" + typeName + "'. Known fields: "
                                    + String.join(", ", schema.fields().keySet()));
                        }

                        String valueToken = fieldTokens.get(i + 1);
                        String valueJava = resolveValueToken(valueToken, env);

                        entries.add("\"" + fieldName + "\", (Object) " + valueJava);
                        i += 2;
                    }

                    mapBuilder.append(entries).append("))");

                    return new ExpressionResult(
                            "new DataInstance(\"" + typeName + "\", " + mapBuilder + ")",
                            BuiltinLumenTypes.DATA.id(),
                            Map.of("data_type", typeName));
                }));
    }

    /**
     * Registers field access expression patterns.
     *
     * <p>Syntax: {@code get field "<field>" of <obj>} or {@code <obj> field "<field>"}
     *
     * <p>When the data type and field name are both known at compile time, the returned
     * expression is automatically coerced to the schema's declared type (e.g. {@code double}
     * for number fields). Otherwise the raw {@code Object} value from {@code DataInstance.get()}
     * is returned, leaving coercion to the caller.
     */
    private void registerFieldAccess(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get field %field:STRING% (of|from) %obj:EXPR%")
                .description("Gets a field value from a data instance.")
                .examples("set name to get field \"name\" of myArena",
                        "set x to get field \"x1\" from myArena")
                .since("1.0.0")
                .category(Categories.DATA)
                .handler(ctx -> {
                    ctx.codegen().addImport(DataInstance.class.getName());
                    String objJava = ctx.java("obj");
                    String fieldJava = ctx.java("field");
                    String rawGet = "((DataInstance) " + objJava + ").get(" + fieldJava + ")";
                    return coerceFieldResult(ctx, rawGet);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("%obj:EXPR% field [of] %field:STRING%")
                .description("Gets a field value from a data instance using postfix syntax.")
                .example("set name to myArena field \"name\"")
                .since("1.0.0")
                .category(Categories.DATA)
                .handler(ctx -> {
                    ctx.codegen().addImport(DataInstance.class.getName());
                    String objJava = ctx.java("obj");
                    String fieldJava = ctx.java("field");
                    String rawGet = "((DataInstance) " + objJava + ").get(" + fieldJava + ")";
                    return coerceFieldResult(ctx, rawGet);
                }));
    }
}

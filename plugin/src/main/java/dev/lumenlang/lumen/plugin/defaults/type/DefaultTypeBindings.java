package dev.lumenlang.lumen.plugin.defaults.type;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.CodegenContext;
import dev.lumenlang.lumen.api.codegen.TypeEnv;
import dev.lumenlang.lumen.api.codegen.TypeEnv.VarHandle;
import dev.lumenlang.lumen.api.exceptions.ParseFailureException;
import dev.lumenlang.lumen.api.type.AddonTypeBinding;
import dev.lumenlang.lumen.api.type.BuiltinLumenTypes;
import dev.lumenlang.lumen.api.type.CollectionType;
import dev.lumenlang.lumen.api.type.EnumTypeBinding;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.api.type.ObjectType;
import dev.lumenlang.lumen.api.type.RegistryTypeBinding;
import dev.lumenlang.lumen.api.type.TypeBindingMeta;
import dev.lumenlang.lumen.api.util.FuzzyMatch;
import dev.lumenlang.lumen.api.version.MinecraftVersion;
import dev.lumenlang.lumen.pipeline.java.compiled.DataInstance;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Registers all built-in type bindings for the default Lumen language.
 */
@Registration(order = -1000)
@SuppressWarnings("unused")
public final class DefaultTypeBindings {

    private static <E extends Enum<E>> void tryRegisterEnum(
            @NotNull LumenAPI api,
            @NotNull String typeId,
            @NotNull Class<E> enumClass,
            @NotNull String fqcn) {
        try {
            api.types().register(EnumTypeBinding.of(typeId, enumClass, fqcn));
        } catch (Exception e) {
            LumenLogger.warning("Skipping enum type binding '" + typeId + "' (" + fqcn + "): " + e.getMessage());
        }
    }

    private static void tryRegisterRegistryType(
            @NotNull LumenAPI api,
            @NotNull String typeId,
            @NotNull Class<?> clazz,
            @NotNull String fqcn) {
        try {
            api.types().register(RegistryTypeBinding.fromStaticFields(typeId, clazz, fqcn));
        } catch (Exception e) {
            LumenLogger.warning("Skipping registry type binding '" + typeId + "' (" + fqcn + "): " + e.getMessage());
        }
    }

    /**
     * Formats a double value for clean Java source output.
     *
     * <p>
     * Whole numbers produce a trailing {@code .0} (e.g. {@code 20.0}).
     * Fractional values are rendered with up to 6 significant decimal digits,
     * with trailing zeros stripped.
     *
     * @param d the double value
     * @return a clean representation suitable for Java source code
     */
    private static @NotNull String formatDouble(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            long whole = (long) d;
            return whole + ".0";
        }
        String formatted = String.format(Locale.ROOT, "%.6f", d);
        formatted = formatted.replaceAll("0+$", "");
        if (formatted.endsWith(".")) {
            formatted += "0";
        }
        return formatted;
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        registerInt(api);
        registerLong(api);
        registerDouble(api);
        registerNumber(api);
        registerBoolean(api);
        registerMaterial(api);
        registerPlayer(api);
        registerOfflinePlayer(api);
        registerCond(api);
        registerOp(api);
        registerEntity(api);
        registerLivingEntity(api);
        registerItem(api);
        registerItemStack(api);
        registerLocation(api);
        registerWorld(api);
        registerList(api);
        registerMap(api);
        registerData(api);
        registerBlock(api);
        registerInventory(api);
        registerVar(api);
        registerLiteralList(api);
        registerEnums(api);
    }

    private void registerEnums(@NotNull LumenAPI api) {
        tryRegisterEnum(api, "DYE_COLOR", DyeColor.class, DyeColor.class.getName());
        tryRegisterEnum(api, "AXOLOTL_VARIANT", Axolotl.Variant.class, Axolotl.Variant.class.getName());
        tryRegisterEnum(api, "GAME_MODE", GameMode.class, GameMode.class.getName());
        tryRegisterEnum(api, "ACTION", Action.class, Action.class.getName());
        registerVillagerTypes(api);
    }

    private void registerVillagerTypes(@NotNull LumenAPI api) {
        if (MinecraftVersion.current().isAtLeast(MinecraftVersion.V1_21)) {
            tryRegisterRegistryType(api, "VILLAGER_PROFESSION",
                    Villager.Profession.class, Villager.Profession.class.getName());
            tryRegisterRegistryType(api, "VILLAGER_TYPE",
                    Villager.Type.class, Villager.Type.class.getName());
        } else {
            tryRegisterEnum(api, "VILLAGER_PROFESSION",
                    Villager.Profession.class, Villager.Profession.class.getName());
            tryRegisterEnum(api, "VILLAGER_TYPE",
                    Villager.Type.class, Villager.Type.class.getName());
        }
    }

    private void registerInt(@NotNull LumenAPI api) {
        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "INT";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "a valid integer",
                        "Parses an integer number from a single token or a variable reference, supporting modulo with %.",
                        "int",
                        List.of("give player diamond %amt:INT%"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                if (tokens.isEmpty())
                    throw new ParseFailureException("expected an integer value here");
                int count = hasNegativeLiteral(tokens) ? 2 : 1;
                if (tokens.size() > count && tokens.get(count).equals("%"))
                    count++;
                return count;
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                String text = joinNegative(tokens);
                try {
                    return Integer.parseInt(text);
                } catch (NumberFormatException e) {
                    VarHandle ref = env.lookupVar(text);
                    if (ref != null)
                        return ref;
                    throw new ParseFailureException("'" + text + "' is not a valid integer");
                }
            }

            @Override
            public @NotNull String toJava(Object value, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                if (value instanceof VarHandle ref) {
                    return ref.java();
                }
                return value.toString();
            }
        });
    }

    private void registerLong(@NotNull LumenAPI api) {
        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "LONG";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "a valid long",
                        "Parses a long integer from a single token or a variable reference. Useful for large numeric values that exceed the int range.",
                        "long",
                        List.of("set %var:EXPR% to %val:LONG%"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                if (tokens.isEmpty())
                    throw new ParseFailureException("expected a long value here");
                return hasNegativeLiteral(tokens) ? 2 : 1;
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                String text = joinNegative(tokens);
                String stripped = text;
                if (stripped.endsWith("L") || stripped.endsWith("l")) {
                    stripped = stripped.substring(0, stripped.length() - 1);
                }
                try {
                    return Long.parseLong(stripped);
                } catch (NumberFormatException e) {
                    VarHandle ref = env.lookupVar(text);
                    if (ref != null)
                        return ref;
                    throw new ParseFailureException("'" + text + "' is not a valid long");
                }
            }

            @Override
            public @NotNull String toJava(Object value, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                if (value instanceof VarHandle ref) {
                    return "((long) " + ref.java() + ")";
                }
                return value + "L";
            }
        });
    }

    private void registerDouble(@NotNull LumenAPI api) {
        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "DOUBLE";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "a valid number",
                        "Parses a decimal number from a single token or a variable reference. Trailing zeros are stripped for cleaner output (e.g. 20.50 becomes 20.5).",
                        "double",
                        List.of("set %e:ENTITY% max_health [to] %val:DOUBLE%"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                if (tokens.isEmpty())
                    throw new ParseFailureException("expected a number here");
                return hasNegativeLiteral(tokens) ? 2 : 1;
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                String text = joinNegative(tokens);
                try {
                    return Double.parseDouble(text);
                } catch (NumberFormatException e) {
                    VarHandle ref = env.lookupVar(text);
                    if (ref != null)
                        return ref;
                    throw new ParseFailureException("'" + text + "' is not a number");
                }
            }

            @Override
            public @NotNull String toJava(Object value, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                if (value instanceof VarHandle ref) {
                    return "((double) " + ref.java() + ")";
                }
                return formatDouble((Double) value);
            }
        });
    }

    private void registerNumber(@NotNull LumenAPI api) {
        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "NUMBER";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "a valid number",
                        "Parses any numeric literal (int, long, or double) from a single token or a variable reference. Automatically detects the appropriate numeric type based on the token content.",
                        "Number",
                        List.of("set %var:EXPR% to %val:NUMBER%"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                if (tokens.isEmpty())
                    throw new ParseFailureException("expected a number here");
                return hasNegativeLiteral(tokens) ? 2 : 1;
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                String text = joinNegative(tokens);
                if (text.endsWith("L") || text.endsWith("l")) {
                    try {
                        return Long.parseLong(text.substring(0, text.length() - 1));
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (text.contains(".")) {
                    try {
                        return Double.parseDouble(text);
                    } catch (NumberFormatException ignored) {
                    }
                }
                try {
                    return Integer.parseInt(text);
                } catch (NumberFormatException e) {
                    try {
                        return Long.parseLong(text);
                    } catch (NumberFormatException e2) {
                        VarHandle ref = env.lookupVar(text);
                        if (ref != null)
                            return ref;
                        throw new ParseFailureException("'" + text + "' is not a valid number");
                    }
                }
            }

            @Override
            public @NotNull String toJava(Object value, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                if (value instanceof VarHandle ref) {
                    return "((double) " + ref.java() + ")";
                }
                if (value instanceof Double d) {
                    return formatDouble(d);
                }
                if (value instanceof Long l) {
                    return l + "L";
                }
                return value.toString();
            }
        });
    }

    private void registerBoolean(@NotNull LumenAPI api) {
        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "BOOLEAN";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "a boolean (true or false)",
                        "Parses a boolean from a single token using truthiness: true/yes/on map to true, and false/no/off map to false.",
                        "boolean",
                        List.of("set %e:ENTITY% gravity [to] %val:BOOLEAN%"),
                        "1.0.0",
                        false);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                String raw = tokens.get(0).replace("\"", "").toLowerCase(Locale.ROOT);
                if (raw.equals("true") || raw.equals("yes") || raw.equals("on")) return Boolean.TRUE;
                if (raw.equals("false") || raw.equals("no") || raw.equals("off")) return Boolean.FALSE;
                VarHandle ref = env.lookupVar(tokens.get(0));
                if (ref != null) return ref;
                throw new ParseFailureException("'" + tokens.get(0) + "' is not a valid boolean (expected true/false, yes/no, or on/off)");
            }

            @Override
            public @NotNull String toJava(Object value, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                if (value instanceof VarHandle ref) {
                    return "Boolean.parseBoolean(String.valueOf(" + ref.java() + "))";
                }
                return value.toString();
            }
        });
    }

    private void registerMaterial(@NotNull LumenAPI api) {
        Set<String> knownMats = new LinkedHashSet<>();
        for (Material mat : Material.values()) {
            knownMats.add(mat.name());
        }
        Set<String> frozenMats = Set.copyOf(knownMats);

        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "MATERIAL";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "a valid material",
                        "Resolves a single token to a Bukkit Material enum constant, or a variable reference for runtime resolution.",
                        Material.class.getName(),
                        List.of("give player %mat:MATERIAL% 1"),
                        "1.0.0",
                        false);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                String normalized = tokens.get(0).toUpperCase(Locale.ROOT);
                if (frozenMats.contains(normalized)) return normalized;
                VarHandle ref = env.lookupVar(tokens.get(0));
                if (ref != null) return ref;
                throw new ParseFailureException(fuzzyMaterial(tokens.get(0), frozenMats));
            }

            @Override
            public @NotNull String toJava(Object value, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                ctx.addImport(Material.class.getName());
                if (value instanceof VarHandle ref) {
                    return "Material.valueOf(String.valueOf(" + ref.java() + ").toUpperCase())";
                }
                return "Material." + value;
            }
        });
    }

    private void registerPlayer(@NotNull LumenAPI api) {
        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "PLAYER";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "a player variable",
                        "Resolves a player reference from a variable name. Does not accept possessive syntax.",
                        Player.class.getName(),
                        List.of("message %who:PLAYER% \"Hello!\"", "kill player"),
                        "1.0.0",
                        false);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                String raw = tokens.get(0);
                if (raw.endsWith("'s"))
                    throw new ParseFailureException("'" + raw + "' should not use possessive form here");

                VarHandle ref = env.lookupVar(raw);
                if (ref == null)
                    throw new ParseFailureException("'" + raw + "' does not exist in this scope");
                if (!isPlayer(ref))
                    throw new ParseFailureException("'" + raw + "' is a " + ref.type().displayName() + ", not a player");
                return ref;
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                if (v == null)
                    throw new RuntimeException("Cannot generate Java for null player reference");
                return ((VarHandle) v).java();
            }

            private boolean isPlayer(@NotNull VarHandle ref) {
                return MinecraftTypes.PLAYER.equals(ref.type().unwrap());
            }
        });

        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "PLAYER_POSSESSIVE";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "a player variable in possessive form (e.g. player's)",
                        "Resolves a player reference from a possessive token (e.g. player's). The token must end with 's.",
                        Player.class.getName(),
                        List.of("%who:PLAYER_POSSESSIVE% name", "%who:PLAYER_POSSESSIVE% health"),
                        "1.0.0",
                        false);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                String raw = tokens.get(0);
                if (!raw.endsWith("'s"))
                    throw new ParseFailureException("'" + raw + "' must use possessive form (e.g. player's)");
                String name = raw.substring(0, raw.length() - 2);

                VarHandle ref = env.lookupVar(name);
                if (ref == null)
                    throw new ParseFailureException("'" + name + "' does not exist in this scope");
                if (!isPlayer(ref))
                    throw new ParseFailureException("'" + name + "' is a " + ref.type().displayName() + ", not a player");
                return ref;
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                if (v == null)
                    throw new RuntimeException("Cannot generate Java for null player reference");
                return ((VarHandle) v).java();
            }

            private boolean isPlayer(@NotNull VarHandle ref) {
                return MinecraftTypes.PLAYER.equals(ref.type().unwrap());
            }
        });
    }

    private void registerOfflinePlayer(@NotNull LumenAPI api) {
        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "OFFLINE_PLAYER";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "an offline player variable",
                        "Resolves an offline player reference. Does not accept possessive syntax.",
                        OfflinePlayer.class.getName(),
                        List.of("ban %target:OFFLINE_PLAYER%"),
                        "1.0.0",
                        false);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                String raw = tokens.get(0);
                if (raw.endsWith("'s"))
                    throw new ParseFailureException("'" + raw + "' should not use possessive form here");

                VarHandle ref = env.lookupVar(raw);
                if (ref == null)
                    throw new ParseFailureException("'" + raw + "' does not exist in this scope");
                if (!isOfflinePlayer(ref))
                    throw new ParseFailureException("'" + raw + "' is a " + ref.type().displayName() + ", not an offline player");
                return ref;
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                if (v == null)
                    throw new RuntimeException("Cannot generate Java for null offline player reference");
                return ((VarHandle) v).java();
            }

            private boolean isOfflinePlayer(@NotNull VarHandle ref) {
                return MinecraftTypes.OFFLINE_PLAYER.equals(ref.type().unwrap());
            }
        });

        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "OFFLINE_PLAYER_POSSESSIVE";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "an offline player variable in possessive form (e.g. player's)",
                        "Resolves an offline player reference from a possessive token (e.g. offlinePlayer's). The token must end with 's.",
                        OfflinePlayer.class.getName(),
                        List.of("%who:OFFLINE_PLAYER_POSSESSIVE% name"),
                        "1.0.0",
                        false);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                String raw = tokens.get(0);
                if (!raw.endsWith("'s"))
                    throw new ParseFailureException("'" + raw + "' must use possessive form (e.g. player's)");
                String name = raw.substring(0, raw.length() - 2);

                VarHandle ref = env.lookupVar(name);
                if (ref == null)
                    throw new ParseFailureException("'" + name + "' does not exist in this scope");
                if (!isOfflinePlayer(ref))
                    throw new ParseFailureException("'" + name + "' is a " + ref.type().displayName() + ", not an offline player");
                return ref;
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                if (v == null)
                    throw new RuntimeException("Cannot generate Java for null offline player reference");
                return ((VarHandle) v).java();
            }

            private boolean isOfflinePlayer(@NotNull VarHandle ref) {
                return MinecraftTypes.OFFLINE_PLAYER.equals(ref.type().unwrap());
            }
        });
    }

    private void registerCond(@NotNull LumenAPI api) {
        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "COND";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "a condition",
                        "Captures all remaining tokens as a raw condition string. Used internally for conditional expressions.",
                        "String",
                        List.of(),
                        "1.0.0",
                        false);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                return String.join(" ", tokens);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                return -1;
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                return (String) v;
            }
        });
    }

    private void registerOp(@NotNull LumenAPI api) {
        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "OP";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "a comparison operator (e.g. 'is', 'greater than', '==')",
                        "Parses a comparison operator from natural language (e.g. 'greater than', 'is', '==') into a Java operator string.",
                        "String",
                        List.of("%a:EXPR% %op:OP% %b:EXPR%"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                if (tokens.isEmpty())
                    throw new ParseFailureException("expected an operator here");
                String first = tokens.get(0).toLowerCase(Locale.ROOT);
                if (tokens.size() >= 3) {
                    String second = tokens.get(1).toLowerCase(Locale.ROOT);
                    String third = tokens.get(2).toLowerCase(Locale.ROOT);
                    if (first.equals("is") && second.equals("greater") && third.equals("than")) {
                        if (tokens.size() >= 6
                                && tokens.get(3).toLowerCase(Locale.ROOT).equals("or")
                                && tokens.get(4).toLowerCase(Locale.ROOT).equals("equal")
                                && tokens.get(5).toLowerCase(Locale.ROOT).equals("to"))
                            return 6;
                        return 3;
                    }
                    if (first.equals("is") && second.equals("less") && third.equals("than")) {
                        if (tokens.size() >= 6
                                && tokens.get(3).toLowerCase(Locale.ROOT).equals("or")
                                && tokens.get(4).toLowerCase(Locale.ROOT).equals("equal")
                                && tokens.get(5).toLowerCase(Locale.ROOT).equals("to"))
                            return 6;
                        return 3;
                    }
                    if (first.equals("greater") && second.equals("than") && third.equals("or") && tokens.size() >= 5) {
                        String fourth = tokens.get(3).toLowerCase(Locale.ROOT);
                        if (fourth.equals("equal") && tokens.get(4).toLowerCase(Locale.ROOT).equals("to"))
                            return 5;
                    }
                    if (first.equals("less") && second.equals("than") && third.equals("or") && tokens.size() >= 5) {
                        String fourth = tokens.get(3).toLowerCase(Locale.ROOT);
                        if (fourth.equals("equal") && tokens.get(4).toLowerCase(Locale.ROOT).equals("to"))
                            return 5;
                    }
                    if (first.equals("not") && second.equals("equal") && third.equals("to"))
                        return 3;
                    if (first.equals("is") && second.equals("not") && third.equals("equal")) {
                        if (tokens.size() >= 4 && tokens.get(3).toLowerCase(Locale.ROOT).equals("to"))
                            return 4;
                    }
                }
                if (tokens.size() >= 2) {
                    String second = tokens.get(1).toLowerCase(Locale.ROOT);
                    if (first.equals("greater") && second.equals("than"))
                        return 2;
                    if (first.equals("less") && second.equals("than"))
                        return 2;
                    if (first.equals("equal") && second.equals("to"))
                        return 2;
                    if (first.equals("not") && second.equals("equal"))
                        return 2;
                    if (first.equals("is") && second.equals("not"))
                        return 2;
                    if (second.equals("=")
                            && (first.equals(">") || first.equals("<") || first.equals("=") || first.equals("!")))
                        return 2;
                }
                return 1;
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                if (tokens.size() == 2) {
                    String combined = tokens.get(0) + tokens.get(1);
                    if (combined.equals(">=") || combined.equals("<=") || combined.equals("==")
                            || combined.equals("!="))
                        return combined;
                }
                String joined = String.join(" ", tokens).toLowerCase(Locale.ROOT);
                return switch (joined) {
                    case "==", "equals", "is", "equal to" -> "==";
                    case "!=", "not equal", "not equal to", "is not", "is not equal to" -> "!=";
                    case "<", "less than", "is less than" -> "<";
                    case ">", "greater than", "is greater than" -> ">";
                    case "<=", "less than or equal to", "is less than or equal to" -> "<=";
                    case ">=", "greater than or equal to", "is greater than or equal to" -> ">=";
                    default -> {
                        String op = tokens.get(0);
                        yield switch (op) {
                            case "==", "!=", "<", ">", "<=", ">=" -> op;
                            default -> throw new ParseFailureException("'" + joined + "' is not a valid operator");
                        };
                    }
                };
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                return (String) v;
            }
        });
    }

    private void registerEntity(@NotNull LumenAPI api) {
        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "ENTITY";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "an entity variable",
                        "Resolves an entity reference from a variable name. Does not accept possessive syntax.",
                        Entity.class.getName(),
                        List.of("kill %e:ENTITY%", "teleport entity to location"),
                        "1.0.0",
                        false);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                String raw = tokens.get(0);
                if (raw.endsWith("'s"))
                    throw new ParseFailureException("'" + raw + "' should not use possessive form here");

                VarHandle ref = env.lookupVar(raw);
                if (ref == null)
                    throw new ParseFailureException("'" + raw + "' does not exist in this scope");
                if (!isEntity(ref))
                    throw new ParseFailureException("'" + raw + "' is a " + ref.type().displayName() + ", not an entity");
                return ref;
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                if (v == null)
                    throw new RuntimeException("Cannot generate Java for null entity reference");
                return ((VarHandle) v).java();
            }

            private boolean isEntity(@NotNull VarHandle ref) {
                LumenType unwrapped = ref.type().unwrap();
                return unwrapped instanceof ObjectType obj && (obj.equals(MinecraftTypes.ENTITY) || obj.isSubtypeOf(MinecraftTypes.ENTITY));
            }
        });

        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "ENTITY_POSSESSIVE";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "an entity variable in possessive form (e.g. entity's)",
                        "Resolves an entity reference from a possessive token (e.g. entity's). The token must end with 's.",
                        Entity.class.getName(),
                        List.of("%e:ENTITY_POSSESSIVE% health"),
                        "1.0.0",
                        false);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                String raw = tokens.get(0);
                if (!raw.endsWith("'s"))
                    throw new ParseFailureException("'" + raw + "' must use possessive form (e.g. entity's)");
                String name = raw.substring(0, raw.length() - 2);

                VarHandle ref = env.lookupVar(name);
                if (ref == null)
                    throw new ParseFailureException("'" + name + "' does not exist in this scope");
                if (!isEntity(ref))
                    throw new ParseFailureException("'" + name + "' is a " + ref.type().displayName() + ", not an entity");
                return ref;
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                if (v == null)
                    throw new RuntimeException("Cannot generate Java for null entity reference");
                return ((VarHandle) v).java();
            }

            private boolean isEntity(@NotNull VarHandle ref) {
                LumenType unwrapped = ref.type().unwrap();
                return unwrapped instanceof ObjectType obj && (obj.equals(MinecraftTypes.ENTITY) || obj.isSubtypeOf(MinecraftTypes.ENTITY));
            }
        });

        api.types().register(new AddonTypeBinding() {
            private final Set<String> knownEntities = buildKnownEntities();

            private Set<String> buildKnownEntities() {
                Set<String> set = new LinkedHashSet<>();
                for (EntityType et : EntityType.values()) {
                    if (et != EntityType.UNKNOWN) {
                        set.add(et.name());
                    }
                }
                return Set.copyOf(set);
            }

            @Override
            public @NotNull String id() {
                return "ENTITY_TYPE";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "a valid entity type",
                        "Resolves a single token to a Bukkit EntityType enum constant. Validates against all known entity types.",
                        EntityType.class.getName(),
                        List.of("spawn %type:ENTITY_TYPE% at location"),
                        "1.0.0",
                        false);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                String raw = tokens.get(0);
                String normalized = raw.toUpperCase(Locale.ROOT)
                        .replace(' ', '_').replace('-', '_');
                if (knownEntities.contains(normalized))
                    return normalized;
                VarHandle ref = env.lookupVar(raw);
                if (ref != null)
                    return ref;
                throw new ParseFailureException(fuzzyEntityType(raw, knownEntities));
            }

            @Override
            public @NotNull String toJava(Object value, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                if (value instanceof VarHandle ref) {
                    ctx.addImport(EntityType.class.getName());
                    return "EntityType.valueOf(String.valueOf(" + ref.java() + ").toUpperCase())";
                }
                ctx.addImport(EntityType.class.getName());
                return "EntityType." + value;
            }
        });
    }

    private void registerLivingEntity(@NotNull LumenAPI api) {
        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "LIVING_ENTITY";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "a living entity variable",
                        "Resolves a living entity reference from a variable name. Accepts any variable whose declared type is a subtype of LIVING_ENTITY (so PLAYER and other living-entity subtypes both work).",
                        "org.bukkit.entity.LivingEntity",
                        List.of("heal %e:LIVING_ENTITY%", "damage %e:LIVING_ENTITY% by 5"),
                        "1.0.0",
                        false);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                String raw = tokens.get(0);
                if (raw.endsWith("'s"))
                    throw new ParseFailureException("'" + raw + "' should not use possessive form here");

                VarHandle ref = env.lookupVar(raw);
                if (ref == null)
                    throw new ParseFailureException("'" + raw + "' does not exist in this scope");
                if (!isLivingEntity(ref))
                    throw new ParseFailureException("'" + raw + "' is a " + ref.type().displayName() + ", not a living entity");
                return ref;
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                if (v == null)
                    throw new RuntimeException("Cannot generate Java for null living entity reference");
                return ((VarHandle) v).java();
            }

            private boolean isLivingEntity(@NotNull VarHandle ref) {
                LumenType unwrapped = ref.type().unwrap();
                return unwrapped instanceof ObjectType obj && (obj.equals(MinecraftTypes.LIVING_ENTITY) || obj.isSubtypeOf(MinecraftTypes.LIVING_ENTITY));
            }
        });

        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "LIVING_ENTITY_POSSESSIVE";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "a living entity variable in possessive form (e.g. mob's)",
                        "Resolves a living entity reference from a possessive token (e.g. mob's). The token must end with 's.",
                        "org.bukkit.entity.LivingEntity",
                        List.of("%e:LIVING_ENTITY_POSSESSIVE% health"),
                        "1.0.0",
                        false);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                String raw = tokens.get(0);
                if (!raw.endsWith("'s"))
                    throw new ParseFailureException("'" + raw + "' must use possessive form (e.g. mob's)");
                String name = raw.substring(0, raw.length() - 2);

                VarHandle ref = env.lookupVar(name);
                if (ref == null)
                    throw new ParseFailureException("'" + name + "' does not exist in this scope");
                if (!isLivingEntity(ref))
                    throw new ParseFailureException("'" + name + "' is a " + ref.type().displayName() + ", not a living entity");
                return ref;
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                if (v == null)
                    throw new RuntimeException("Cannot generate Java for null living entity reference");
                return ((VarHandle) v).java();
            }

            private boolean isLivingEntity(@NotNull VarHandle ref) {
                LumenType unwrapped = ref.type().unwrap();
                return unwrapped instanceof ObjectType obj && (obj.equals(MinecraftTypes.LIVING_ENTITY) || obj.isSubtypeOf(MinecraftTypes.LIVING_ENTITY));
            }
        });
    }

    private void registerItem(@NotNull LumenAPI api) {
        api.types().register(new AddonTypeBinding() {
            private final Set<String> knownMaterials = buildKnownMaterials();

            private Set<String> buildKnownMaterials() {
                Set<String> set = new LinkedHashSet<>();
                for (Material mat : Material.values()) {
                    if (mat.isItem()) {
                        set.add(mat.name());
                    }
                }
                return Set.copyOf(set);
            }

            @Override
            public @NotNull String id() {
                return "ITEM";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "a valid item",
                        "Resolves a single token to a Bukkit ItemStack created from a Material name. Only accepts materials that are valid items.",
                        ItemStack.class.getName(),
                        List.of("give player %item:ITEM%"),
                        "1.0.0",
                        false);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                String raw = tokens.get(0);
                String normalized = raw.toUpperCase(Locale.ROOT)
                        .replace(' ', '_').replace('-', '_');
                if (knownMaterials.contains(normalized))
                    return normalized;
                VarHandle ref = env.lookupVar(raw);
                if (ref != null) {
                    if (MinecraftTypes.ITEMSTACK.equals(ref.type().unwrap()))
                        throw new ParseFailureException("'" + raw + "' is an item stack variable, not a material name");
                    return ref;
                }
                throw new ParseFailureException(fuzzyItem(raw, knownMaterials));
            }

            @Override
            public @NotNull String toJava(Object value, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                ctx.addImport(ItemStack.class.getName());
                ctx.addImport(Material.class.getName());
                if (value instanceof VarHandle ref) {
                    return "new ItemStack(Material.valueOf(String.valueOf(" + ref.java()
                            + ").toUpperCase()))";
                }
                return "new ItemStack(Material." + value + ")";
            }
        });
    }

    private void registerItemStack(@NotNull LumenAPI api) {
        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "ITEMSTACK";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "an item variable",
                        "Resolves an ItemStack reference from a variable name. Does not accept possessive syntax.",
                        ItemStack.class.getName(),
                        List.of("set %i:ITEMSTACK% amount to 5"),
                        "1.0.0",
                        false);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                String raw = tokens.get(0);
                if (raw.endsWith("'s"))
                    throw new ParseFailureException("'" + raw + "' should not use possessive form here");
                VarHandle ref = env.lookupVar(raw);
                if (ref == null)
                    throw new ParseFailureException("'" + raw + "' does not exist in this scope");
                if (!isItemStack(ref))
                    throw new ParseFailureException("'" + raw + "' is a " + ref.type().displayName() + ", not an item");
                return ref;
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                if (v == null)
                    throw new RuntimeException("Cannot generate Java for null item stack reference");
                return ((VarHandle) v).java();
            }

            private boolean isItemStack(@NotNull VarHandle ref) {
                return MinecraftTypes.ITEMSTACK.equals(ref.type().unwrap());
            }
        });

        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "ITEMSTACK_POSSESSIVE";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "an item variable in possessive form (e.g. item's)",
                        "Resolves an ItemStack reference from a possessive token (e.g. item's). The token must end with 's.",
                        ItemStack.class.getName(),
                        List.of("%i:ITEMSTACK_POSSESSIVE% display name"),
                        "1.0.0",
                        false);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                String raw = tokens.get(0);
                if (!raw.endsWith("'s"))
                    throw new ParseFailureException("'" + raw + "' must use possessive form (e.g. item's)");
                String name = raw.substring(0, raw.length() - 2);
                VarHandle ref = env.lookupVar(name);
                if (ref == null)
                    throw new ParseFailureException("'" + name + "' does not exist in this scope");
                if (!isItemStack(ref))
                    throw new ParseFailureException("'" + name + "' is a " + ref.type().displayName() + ", not an item");
                return ref;
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                if (v == null)
                    throw new RuntimeException("Cannot generate Java for null item stack reference");
                return ((VarHandle) v).java();
            }

            private boolean isItemStack(@NotNull VarHandle ref) {
                return MinecraftTypes.ITEMSTACK.equals(ref.type().unwrap());
            }
        });
    }

    private void registerWorld(@NotNull LumenAPI api) {
        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "WORLD";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "a world variable",
                        "Resolves a world reference from a variable name.",
                        World.class.getName(),
                        List.of("set time in %w:WORLD% to 0"),
                        "1.0.0",
                        false);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                String name = tokens.get(0);
                VarHandle ref = env.lookupVar(name);
                if (ref == null)
                    throw new ParseFailureException("'" + name + "' does not exist in this scope");
                if (!isWorld(ref))
                    throw new ParseFailureException("'" + name + "' is a " + ref.type().displayName() + ", not a world");
                return ref;
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                return ((VarHandle) v).java();
            }

            private boolean isWorld(@NotNull VarHandle ref) {
                return MinecraftTypes.WORLD.equals(ref.type().unwrap());
            }
        });
    }

    private void registerLocation(@NotNull LumenAPI api) {
        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "LOCATION";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "a location variable",
                        "Resolves a location reference from a variable name.",
                        Location.class.getName(),
                        List.of("teleport player to %loc:LOCATION%"),
                        "1.0.0",
                        false);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                String name = tokens.get(0);
                VarHandle ref = env.lookupVar(name);
                if (ref == null)
                    throw new ParseFailureException("'" + name + "' does not exist in this scope");
                if (!isLocation(ref))
                    throw new ParseFailureException("'" + name + "' is a " + ref.type().displayName() + ", not a location");
                return ref;
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                return ((VarHandle) v).java();
            }

            private boolean isLocation(@NotNull VarHandle ref) {
                return MinecraftTypes.LOCATION.equals(ref.type().unwrap());
            }
        });
    }

    private void registerList(@NotNull LumenAPI api) {
        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "LIST";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "a list variable",
                        "Resolves a list variable reference. The variable must have been declared as a list type.",
                        List.class.getName(),
                        List.of("add \"hello\" to %list:LIST%"),
                        "1.0.0",
                        false);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                String name = tokens.get(0);
                VarHandle ref = env.lookupVar(name);
                if (ref == null)
                    throw new ParseFailureException("'" + name + "' does not exist in this scope");
                if (!isList(ref))
                    throw new ParseFailureException("'" + name + "' is a " + ref.type().displayName() + ", not a list");
                return ref;
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                if (v instanceof VarHandle ref) {
                    TypeEnv.GlobalInfo info = ref.globalInfo();
                    if (info != null && info.scoped()) return ref.name();
                    return ref.java();
                }
                throw new RuntimeException("Cannot generate Java for null list reference");
            }

            private boolean isList(@NotNull VarHandle ref) {
                return ref.type().unwrap() instanceof CollectionType ct && ct.rawType().id().equals("LIST");
            }
        });
    }

    private void registerMap(@NotNull LumenAPI api) {
        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "MAP";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "a map variable",
                        "Resolves a map variable reference. The variable must have been declared as a map type.",
                        Map.class.getName(),
                        List.of("set %map:MAP% at key \"name\" to \"value\""),
                        "1.0.0",
                        false);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                String name = tokens.get(0);
                VarHandle ref = env.lookupVar(name);
                if (ref == null)
                    throw new ParseFailureException("'" + name + "' does not exist in this scope");
                if (!isMap(ref))
                    throw new ParseFailureException("'" + name + "' is a " + ref.type().displayName() + ", not a map");
                return ref;
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                if (v instanceof VarHandle ref) {
                    TypeEnv.GlobalInfo info = ref.globalInfo();
                    if (info != null && info.scoped()) return ref.name();
                    return ref.java();
                }
                throw new RuntimeException("Cannot generate Java for null map reference");
            }

            private boolean isMap(@NotNull VarHandle ref) {
                return ref.type().unwrap() instanceof CollectionType ct && ct.rawType().id().equals("MAP");
            }
        });
    }

    private void registerData(@NotNull LumenAPI api) {
        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "DATA";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "a data variable",
                        "Resolves a data instance variable reference. The variable must have been declared as a data type.",
                        DataInstance.class.getName(),
                        List.of("get field \"name\" of %obj:DATA%"),
                        "1.0.0",
                        false);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                String name = tokens.get(0);
                VarHandle ref = env.lookupVar(name);
                if (ref == null)
                    throw new ParseFailureException("'" + name + "' does not exist in this scope");
                if (!isData(ref))
                    throw new ParseFailureException("'" + name + "' is a " + ref.type().displayName() + ", not a data instance");
                return ref;
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                if (v == null)
                    throw new RuntimeException("Cannot generate Java for null data reference");
                return ((VarHandle) v).java();
            }

            private boolean isData(@NotNull VarHandle ref) {
                return BuiltinLumenTypes.DATA.equals(ref.type().unwrap());
            }
        });
    }

    private void registerBlock(@NotNull LumenAPI api) {
        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "BLOCK";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "a block variable",
                        "Resolves a block reference from a variable name.",
                        Block.class.getName(),
                        List.of("set %b:BLOCK% type to stone", "break block naturally"),
                        "1.0.0",
                        false);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                String name = tokens.get(0);
                VarHandle ref = env.lookupVar(name);
                if (ref == null)
                    throw new ParseFailureException("'" + name + "' does not exist in this scope");
                if (!isBlock(ref))
                    throw new ParseFailureException("'" + name + "' is a " + ref.type().displayName() + ", not a block");
                return ref;
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                if (v == null)
                    throw new RuntimeException("Cannot generate Java for null block reference");
                return ((VarHandle) v).java();
            }

            private boolean isBlock(@NotNull VarHandle ref) {
                return MinecraftTypes.BLOCK.equals(ref.type().unwrap());
            }
        });
    }

    private void registerInventory(@NotNull LumenAPI api) {
        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "INVENTORY";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "an inventory variable",
                        "Resolves an inventory reference from a variable name.",
                        "org.bukkit.inventory.Inventory",
                        List.of("set slot 0 of %inv:INVENTORY% to diamond"),
                        "1.0.0",
                        false);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                String name = tokens.get(0);
                VarHandle ref = env.lookupVar(name);
                if (ref == null)
                    throw new ParseFailureException("'" + name + "' does not exist in this scope");
                if (!isInventory(ref))
                    throw new ParseFailureException("'" + name + "' is a " + ref.type().displayName() + ", not an inventory");
                return ref;
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                if (v == null)
                    throw new RuntimeException("Cannot generate Java for null inventory reference");
                return ((VarHandle) v).java();
            }

            private boolean isInventory(@NotNull VarHandle ref) {
                return MinecraftTypes.INVENTORY.equals(ref.type().unwrap());
            }
        });
    }

    private void registerVar(@NotNull LumenAPI api) {
        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "VAR";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "a known variable",
                        "Resolves any variable reference from a single token. Validates that the name refers to a known variable.",
                        "Object",
                        List.of("add 1 to %name:VAR%"),
                        "1.0.0",
                        false);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                String name = tokens.get(0);
                VarHandle ref = env.lookupVar(name);
                if (ref == null)
                    throw new ParseFailureException("'" + name + "' does not exist in this scope");
                return ref;
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                return ((VarHandle) v).java();
            }
        });
    }

    private void registerLiteralList(@NotNull LumenAPI api) {
        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "LITERAL_LIST";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "a comma separated list of values",
                        "Parses a comma separated list of literal values from all remaining tokens (e.g. 'hello, hi, hey').",
                        "String",
                        List.of("aliases %list:LITERAL_LIST%"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                return -1;
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env) {
                if (tokens.isEmpty())
                    throw new ParseFailureException("expected a comma separated list here");
                return String.join(" ", tokens);
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
                return (String) v;
            }
        });
    }

    private static @NotNull String fuzzyMaterial(@NotNull String token, @NotNull Set<String> known) {
        String closest = FuzzyMatch.closest(token.toUpperCase(Locale.ROOT), known);
        if (closest != null) return "'" + token + "' is not a valid material, did you mean '" + closest.toLowerCase(Locale.ROOT) + "'?";
        return "'" + token + "' is not a valid material";
    }

    private static @NotNull String fuzzyEntityType(@NotNull String token, @NotNull Set<String> known) {
        String closest = FuzzyMatch.closest(token.toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_'), known);
        if (closest != null) return "'" + token + "' is not a valid entity type, did you mean '" + closest.toLowerCase(Locale.ROOT) + "'?";
        return "'" + token + "' is not a valid entity type";
    }

    private static @NotNull String fuzzyItem(@NotNull String token, @NotNull Set<String> known) {
        String closest = FuzzyMatch.closest(token.toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_'), known);
        if (closest != null) return "'" + token + "' is not a valid item, did you mean '" + closest.toLowerCase(Locale.ROOT) + "'?";
        return "'" + token + "' is not a valid item";
    }

    private static boolean isNumericText(@NotNull String s) {
        if (s.isEmpty()) return false;
        boolean dot = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '.') {
                if (dot) return false;
                dot = true;
            } else if (!Character.isDigit(c)) return false;
        }
        return true;
    }

    private static boolean hasNegativeLiteral(@NotNull List<String> tokens) {
        return tokens.size() >= 2 && tokens.get(0).equals("-") && isNumericText(tokens.get(1));
    }

    private static @NotNull String joinNegative(@NotNull List<String> tokens) {
        if (hasNegativeLiteral(tokens)) return "-" + tokens.get(1);
        return tokens.get(0);
    }
}

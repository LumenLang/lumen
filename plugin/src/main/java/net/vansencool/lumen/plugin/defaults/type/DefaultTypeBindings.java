package net.vansencool.lumen.plugin.defaults.type;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.codegen.CodegenAccess;
import net.vansencool.lumen.api.codegen.EnvironmentAccess;
import net.vansencool.lumen.api.codegen.EnvironmentAccess.VarHandle;
import net.vansencool.lumen.api.exceptions.ParseFailureException;
import net.vansencool.lumen.api.type.AddonTypeBinding;
import net.vansencool.lumen.api.type.EnumTypeBinding;
import net.vansencool.lumen.api.type.RefTypes;
import net.vansencool.lumen.api.type.RegistryTypeBinding;
import net.vansencool.lumen.api.type.TypeBindingMeta;
import net.vansencool.lumen.api.version.MinecraftVersion;
import net.vansencool.lumen.pipeline.java.compiled.DataInstance;
import net.vansencool.lumen.pipeline.java.compiled.NullGuard;
import net.vansencool.lumen.pipeline.logger.LumenLogger;
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

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Registers all built-in type bindings for the default Lumen language.
 */
@Registration(order = -1000)
@Description("Registers all built-in type bindings for the default Lumen language")
@SuppressWarnings({ "unused", "DataFlowIssue" })
public final class DefaultTypeBindings {

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
        registerItem(api);
        registerItemStack(api);
        registerLocation(api);
        registerWorld(api);
        registerList(api);
        registerMap(api);
        registerData(api);
        registerBlock(api);
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

    private void registerInt(@NotNull LumenAPI api) {
        api.types().register(new AddonTypeBinding() {
            @Override
            public @NotNull String id() {
                return "INT";
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "Parses an integer number from a single token or a variable reference, supporting modulo with %.",
                        "int",
                        List.of("give player diamond %amt:INT%"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    throw new ParseFailureException("INT requires at least one token");
                if (tokens.size() >= 2 && tokens.get(1).equals("%"))
                    return 2;
                return 1;
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                String text = tokens.get(0);
                try {
                    return Integer.parseInt(text);
                } catch (NumberFormatException e) {
                    VarHandle ref = env.lookupVar(text);
                    if (ref != null)
                        return ref;
                    throw new ParseFailureException("Not a valid integer: " + text);
                }
            }

            @Override
            public @NotNull String toJava(Object value, @NotNull CodegenAccess ctx,
                    @NotNull EnvironmentAccess env) {
                if (value instanceof VarHandle ref) {
                    return "Coerce.toInt(" + ref.java() + ")";
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
                        "Parses a long integer from a single token or a variable reference. Useful for large numeric values that exceed the int range.",
                        "long",
                        List.of("set %var:EXPR% to %val:LONG%"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    throw new ParseFailureException("LONG requires at least one token");
                return 1;
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                String text = tokens.get(0);
                if (text.endsWith("L") || text.endsWith("l")) {
                    text = text.substring(0, text.length() - 1);
                }
                try {
                    return Long.parseLong(text);
                } catch (NumberFormatException e) {
                    VarHandle ref = env.lookupVar(tokens.get(0));
                    if (ref != null)
                        return ref;
                    throw e;
                }
            }

            @Override
            public @NotNull String toJava(Object value, @NotNull CodegenAccess ctx,
                    @NotNull EnvironmentAccess env) {
                if (value instanceof VarHandle ref) {
                    return "((long) Coerce.toDouble(" + ref.java() + "))";
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
                        "Parses a decimal number from a single token or a variable reference. Trailing zeros are stripped for cleaner output (e.g. 20.50 becomes 20.5).",
                        "double",
                        List.of("set %e:ENTITY% max_health [to] %val:DOUBLE%"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    throw new ParseFailureException("DOUBLE requires at least one token");
                return 1;
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                String text = tokens.get(0);
                try {
                    return Double.parseDouble(text);
                } catch (NumberFormatException e) {
                    VarHandle ref = env.lookupVar(text);
                    if (ref != null)
                        return ref;
                    throw e;
                }
            }

            @Override
            public @NotNull String toJava(Object value, @NotNull CodegenAccess ctx,
                    @NotNull EnvironmentAccess env) {
                if (value instanceof VarHandle ref) {
                    return "Coerce.toDouble(" + ref.java() + ")";
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
                        "Parses any numeric literal (int, long, or double) from a single token or a variable reference. Automatically detects the appropriate numeric type based on the token content.",
                        "Number",
                        List.of("set %var:EXPR% to %val:NUMBER%"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    throw new ParseFailureException("NUMBER requires at least one token");
                return 1;
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                String text = tokens.get(0);
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
                        throw new ParseFailureException("Not a valid number: " + text);
                    }
                }
            }

            @Override
            public @NotNull String toJava(Object value, @NotNull CodegenAccess ctx,
                    @NotNull EnvironmentAccess env) {
                if (value instanceof VarHandle ref) {
                    return "Coerce.toDouble(" + ref.java() + ")";
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
                        "Parses a boolean from a single token using truthiness: true/yes/on map to true, and false/no/off map to false.",
                        "boolean",
                        List.of("set %e:ENTITY% gravity [to] %val:BOOLEAN%"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    throw new ParseFailureException("BOOLEAN requires at least one token");
                String raw = tokens.get(0).replace("\"", "").toLowerCase(Locale.ROOT);
                if (raw.equals("true") || raw.equals("yes") || raw.equals("on"))
                    return 1;
                if (raw.equals("false") || raw.equals("no") || raw.equals("off"))
                    return 1;
                if (env.lookupVar(tokens.get(0)) != null)
                    return 1;
                throw new ParseFailureException("Unknown boolean value: " + tokens.get(0));
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                String raw = tokens.get(0).replace("\"", "").toLowerCase(Locale.ROOT);
                if (raw.equals("true") || raw.equals("yes") || raw.equals("on"))
                    return Boolean.TRUE;
                if (raw.equals("false") || raw.equals("no") || raw.equals("off"))
                    return Boolean.FALSE;
                VarHandle ref = env.lookupVar(tokens.get(0));
                if (ref != null)
                    return ref;
                throw new ParseFailureException("Unknown boolean value: " + tokens.get(0));
            }

            @Override
            public @NotNull String toJava(Object value, @NotNull CodegenAccess ctx,
                    @NotNull EnvironmentAccess env) {
                if (value instanceof VarHandle ref) {
                    return "Boolean.parseBoolean(String.valueOf(" + ref.java() + "))";
                }
                return value.toString();
            }
        });
    }

    private void registerMaterial(@NotNull LumenAPI api) {
        Set<String> knownMats = new HashSet<>();
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
                        "Resolves a single token to a Bukkit Material enum constant, or a variable reference for runtime resolution.",
                        Material.class.getName(),
                        List.of("give player %mat:MATERIAL% 1"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    throw new ParseFailureException("MATERIAL requires at least one token");
                String normalized = tokens.get(0).toUpperCase(Locale.ROOT);
                if (frozenMats.contains(normalized))
                    return 1;
                if (env.lookupVar(tokens.get(0)) != null)
                    return 1;
                throw new ParseFailureException("Unknown material: " + tokens.get(0));
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                String normalized = tokens.get(0).toUpperCase(Locale.ROOT);
                if (frozenMats.contains(normalized))
                    return normalized;
                VarHandle ref = env.lookupVar(tokens.get(0));
                if (ref != null)
                    return ref;
                throw new ParseFailureException("Unknown material: " + tokens.get(0));
            }

            @Override
            public @NotNull String toJava(Object value, @NotNull CodegenAccess ctx,
                    @NotNull EnvironmentAccess env) {
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
                        "Resolves a player reference from a variable name. Does not accept possessive syntax. Falls back to the default player in the current events context.",
                        Player.class.getName(),
                        List.of("message %who:PLAYER% \"Hello!\"", "kill player"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    return 0;

                String raw = tokens.get(0);
                if (raw.endsWith("'s"))
                    throw new ParseFailureException("PLAYER does not accept possessive form: " + raw);

                VarHandle ref = env.lookupVar(raw);
                if (ref != null && isPlayer(ref))
                    return 1;

                if (env.lookupDefault(RefTypes.PLAYER) != null)
                    return 0;

                return 1;
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty()) {
                    VarHandle def = env.lookupDefault(RefTypes.PLAYER);
                    if (def == null)
                        throw new ParseFailureException("No default player in this context");
                    return def;
                }

                String raw = tokens.get(0);
                if (raw.endsWith("'s"))
                    throw new ParseFailureException("PLAYER does not accept possessive form: " + raw);

                VarHandle ref = env.lookupVar(raw);
                if (ref != null) {
                    if (!isPlayer(ref))
                        throw new ParseFailureException(raw + " is not a player");
                    return ref;
                }

                VarHandle def = env.lookupDefault(RefTypes.PLAYER);
                if (def != null)
                    return def;

                throw new ParseFailureException("Unknown player reference: " + raw);
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenAccess ctx,
                    @NotNull EnvironmentAccess env) {
                if (v == null)
                    throw new RuntimeException("Cannot generate Java for null player reference");
                return NullGuard.codegenOrRaw((VarHandle) v);
            }

            private boolean isPlayer(@NotNull VarHandle ref) {
                return ref.type() != null && ref.type().id().equals(RefTypes.PLAYER.id());
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
                        "Resolves a player reference from a possessive token (e.g. player's). The token must end with 's. Falls back to the default player in the current events context.",
                        Player.class.getName(),
                        List.of("%who:PLAYER_POSSESSIVE% name", "%who:PLAYER_POSSESSIVE% health"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    return 0;

                String raw = tokens.get(0);
                boolean possessive = raw.endsWith("'s");
                String name = possessive ? raw.substring(0, raw.length() - 2) : raw;

                VarHandle ref = env.lookupVar(name);
                if (ref != null && isPlayer(ref))
                    return 1;

                if (possessive) {
                    if (env.lookupDefault(RefTypes.PLAYER) != null)
                        return 1;
                    return 1;
                }

                throw new ParseFailureException("Not a known player reference: " + raw);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty()) {
                    VarHandle def = env.lookupDefault(RefTypes.PLAYER);
                    if (def == null)
                        throw new ParseFailureException("No default player in this context");
                    return def;
                }

                String raw = tokens.get(0);
                boolean possessive = raw.endsWith("'s");
                String name = possessive ? raw.substring(0, raw.length() - 2) : raw;

                VarHandle ref = env.lookupVar(name);
                if (ref != null) {
                    if (!isPlayer(ref))
                        throw new ParseFailureException(name + " is not a player");
                    return ref;
                }

                VarHandle def = env.lookupDefault(RefTypes.PLAYER);
                if (def != null)
                    return def;

                throw new ParseFailureException("Unknown player reference: " + name);
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenAccess ctx,
                    @NotNull EnvironmentAccess env) {
                if (v == null)
                    throw new RuntimeException("Cannot generate Java for null player reference");
                return NullGuard.codegenOrRaw((VarHandle) v);
            }

            private boolean isPlayer(@NotNull VarHandle ref) {
                return ref.type() != null && ref.type().id().equals(RefTypes.PLAYER.id());
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
                        "Resolves an offline player reference. Does not accept possessive syntax. Falls back to the default offline player in the current events context.",
                        OfflinePlayer.class.getName(),
                        List.of("ban %target:OFFLINE_PLAYER%"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    return 0;

                String raw = tokens.get(0);
                if (raw.endsWith("'s"))
                    throw new ParseFailureException("OFFLINE_PLAYER does not accept possessive form: " + raw);

                VarHandle ref = env.lookupVar(raw);
                if (ref != null && isOfflinePlayer(ref))
                    return 1;

                if (env.lookupDefault(RefTypes.OFFLINE_PLAYER) != null)
                    return 0;

                return 1;
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty()) {
                    VarHandle def = env.lookupDefault(RefTypes.OFFLINE_PLAYER);
                    if (def == null)
                        throw new ParseFailureException("No default offline player in this context");
                    return def;
                }

                String raw = tokens.get(0);
                if (raw.endsWith("'s"))
                    throw new ParseFailureException("OFFLINE_PLAYER does not accept possessive form: " + raw);

                VarHandle ref = env.lookupVar(raw);
                if (ref != null) {
                    if (!isOfflinePlayer(ref))
                        throw new ParseFailureException(raw + " is not an offline player");
                    return ref;
                }

                VarHandle def = env.lookupDefault(RefTypes.OFFLINE_PLAYER);
                if (def != null)
                    return def;

                throw new ParseFailureException("Unknown offline player reference: " + raw);
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenAccess ctx,
                    @NotNull EnvironmentAccess env) {
                if (v == null)
                    throw new RuntimeException("Cannot generate Java for null offline player reference");
                return NullGuard.codegenOrRaw((VarHandle) v);
            }

            private boolean isOfflinePlayer(@NotNull VarHandle ref) {
                return ref.type() != null && ref.type().id().equals(RefTypes.OFFLINE_PLAYER.id());
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
                        "Resolves an offline player reference from a possessive token (e.g. offlinePlayer's). The token must end with 's.",
                        OfflinePlayer.class.getName(),
                        List.of("%who:OFFLINE_PLAYER_POSSESSIVE% name"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    return 0;

                String raw = tokens.get(0);
                boolean possessive = raw.endsWith("'s");
                String name = possessive ? raw.substring(0, raw.length() - 2) : raw;

                VarHandle ref = env.lookupVar(name);
                if (ref != null && isOfflinePlayer(ref))
                    return 1;

                if (possessive) {
                    if (env.lookupDefault(RefTypes.OFFLINE_PLAYER) != null)
                        return 1;
                    return 1;
                }

                throw new ParseFailureException("Not a known offline player reference: " + raw);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty()) {
                    VarHandle def = env.lookupDefault(RefTypes.OFFLINE_PLAYER);
                    if (def == null)
                        throw new ParseFailureException("No default offline player in this context");
                    return def;
                }

                String raw = tokens.get(0);
                boolean possessive = raw.endsWith("'s");
                String name = possessive ? raw.substring(0, raw.length() - 2) : raw;

                VarHandle ref = env.lookupVar(name);
                if (ref != null) {
                    if (!isOfflinePlayer(ref))
                        throw new ParseFailureException(name + " is not an offline player");
                    return ref;
                }

                VarHandle def = env.lookupDefault(RefTypes.OFFLINE_PLAYER);
                if (def != null)
                    return def;

                throw new ParseFailureException("Unknown offline player reference: " + name);
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenAccess ctx,
                    @NotNull EnvironmentAccess env) {
                if (v == null)
                    throw new RuntimeException("Cannot generate Java for null offline player reference");
                return NullGuard.codegenOrRaw((VarHandle) v);
            }

            private boolean isOfflinePlayer(@NotNull VarHandle ref) {
                return ref.type() != null && ref.type().id().equals(RefTypes.OFFLINE_PLAYER.id());
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
                        "Captures all remaining tokens as a raw condition string. Used internally for conditional expressions.",
                        "String",
                        List.of(),
                        "1.0.0",
                        false);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                return String.join(" ", tokens);
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenAccess ctx,
                    @NotNull EnvironmentAccess env) {
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
                        "Parses a comparison operator from natural language (e.g. 'greater than', 'is', '==') into a Java operator string.",
                        "String",
                        List.of("%a:EXPR% %op:OP% %b:EXPR%"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    return 0;
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
            public Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    throw new ParseFailureException("Expected operator but got end of input");
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
                            default -> throw new ParseFailureException("Unknown operator: " + joined);
                        };
                    }
                };
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenAccess ctx,
                    @NotNull EnvironmentAccess env) {
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
                        "Resolves an entity reference from a variable name. Does not accept possessive syntax. Falls back to the default entity in the current events context.",
                        Entity.class.getName(),
                        List.of("kill %e:ENTITY%", "teleport entity to location"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    return 0;

                String raw = tokens.get(0);
                if (raw.endsWith("'s"))
                    throw new ParseFailureException("ENTITY does not accept possessive form: " + raw);

                VarHandle ref = env.lookupVar(raw);
                if (ref != null && isEntity(ref))
                    return 1;

                if (env.lookupDefault(RefTypes.ENTITY) != null)
                    return 0;

                return 1;
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty()) {
                    VarHandle def = env.lookupDefault(RefTypes.ENTITY);
                    if (def == null)
                        throw new ParseFailureException("No default entity in this context");
                    return def;
                }

                String raw = tokens.get(0);
                if (raw.endsWith("'s"))
                    throw new ParseFailureException("ENTITY does not accept possessive form: " + raw);

                VarHandle ref = env.lookupVar(raw);
                if (ref != null) {
                    if (!isEntity(ref))
                        throw new ParseFailureException(raw + " is not an entity");
                    return ref;
                }

                VarHandle def = env.lookupDefault(RefTypes.ENTITY);
                if (def != null)
                    return def;

                throw new ParseFailureException("Unknown entity reference: " + raw);
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenAccess ctx,
                    @NotNull EnvironmentAccess env) {
                if (v == null)
                    throw new RuntimeException("Cannot generate Java for null entity reference");
                return NullGuard.codegenOrRaw((VarHandle) v);
            }

            private boolean isEntity(@NotNull VarHandle ref) {
                if (ref.type() == null)
                    return false;
                String id = ref.type().id();
                return id.equals(RefTypes.ENTITY.id()) || id.equals(RefTypes.PLAYER.id());
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
                        "Resolves an entity reference from a possessive token (e.g. entity's). The token must end with 's. Falls back to the default entity in the current events context.",
                        Entity.class.getName(),
                        List.of("%e:ENTITY_POSSESSIVE% health"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    return 0;

                String raw = tokens.get(0);
                boolean possessive = raw.endsWith("'s");
                String name = possessive ? raw.substring(0, raw.length() - 2) : raw;

                VarHandle ref = env.lookupVar(name);
                if (ref != null && isEntity(ref))
                    return 1;

                if (possessive) {
                    if (env.lookupDefault(RefTypes.ENTITY) != null)
                        return 1;
                    return 1;
                }

                throw new ParseFailureException("Not a known entity reference: " + raw);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty()) {
                    VarHandle def = env.lookupDefault(RefTypes.ENTITY);
                    if (def == null)
                        throw new ParseFailureException("No default entity in this context");
                    return def;
                }

                String raw = tokens.get(0);
                boolean possessive = raw.endsWith("'s");
                String name = possessive ? raw.substring(0, raw.length() - 2) : raw;

                VarHandle ref = env.lookupVar(name);
                if (ref != null) {
                    if (!isEntity(ref))
                        throw new ParseFailureException(name + " is not an entity");
                    return ref;
                }

                VarHandle def = env.lookupDefault(RefTypes.ENTITY);
                if (def != null)
                    return def;

                throw new ParseFailureException("Unknown entity reference: " + name);
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenAccess ctx,
                    @NotNull EnvironmentAccess env) {
                if (v == null)
                    throw new RuntimeException("Cannot generate Java for null entity reference");
                return NullGuard.codegenOrRaw((VarHandle) v);
            }

            private boolean isEntity(@NotNull VarHandle ref) {
                if (ref.type() == null)
                    return false;
                String id = ref.type().id();
                return id.equals(RefTypes.ENTITY.id()) || id.equals(RefTypes.PLAYER.id());
            }
        });

        api.types().register(new AddonTypeBinding() {
            private final Set<String> knownEntities = buildKnownEntities();

            private Set<String> buildKnownEntities() {
                Set<String> set = new HashSet<>();
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
                        "Resolves a single token to a Bukkit EntityType enum constant. Validates against all known entity types.",
                        EntityType.class.getName(),
                        List.of("spawn %type:ENTITY_TYPE% at location"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    return 0;
                String normalized = tokens.get(0).toUpperCase(Locale.ROOT)
                        .replace(' ', '_').replace('-', '_');
                if (knownEntities.contains(normalized))
                    return 1;
                if (env.lookupVar(tokens.get(0)) != null)
                    return 1;
                throw new ParseFailureException("Unknown entity type: " + tokens.get(0));
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    throw new ParseFailureException("No entity type token provided");
                String raw = tokens.get(0);
                String normalized = raw.toUpperCase(Locale.ROOT)
                        .replace(' ', '_').replace('-', '_');
                if (knownEntities.contains(normalized))
                    return normalized;
                VarHandle ref = env.lookupVar(raw);
                if (ref != null)
                    return ref;
                throw new ParseFailureException("Unknown entity type: " + raw);
            }

            @Override
            public @NotNull String toJava(Object value, @NotNull CodegenAccess ctx,
                    @NotNull EnvironmentAccess env) {
                if (value instanceof VarHandle ref) {
                    ctx.addImport(EntityType.class.getName());
                    return "EntityType.valueOf(String.valueOf(" + ref.java() + ").toUpperCase())";
                }
                ctx.addImport(EntityType.class.getName());
                return "EntityType." + value;
            }
        });
    }

    private void registerItem(@NotNull LumenAPI api) {
        api.types().register(new AddonTypeBinding() {
            private final Set<String> knownMaterials = buildKnownMaterials();

            private Set<String> buildKnownMaterials() {
                Set<String> set = new HashSet<>();
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
                        "Resolves a single token to a Bukkit ItemStack created from a Material name. Only accepts materials that are valid items.",
                        ItemStack.class.getName(),
                        List.of("give player %item:ITEM%"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    return 0;
                String normalized = tokens.get(0).toUpperCase(Locale.ROOT)
                        .replace(' ', '_').replace('-', '_');
                if (knownMaterials.contains(normalized))
                    return 1;
                VarHandle ref = env.lookupVar(tokens.get(0));
                if (ref != null) {
                    if (ref.type() != null && ref.type().id().equals(RefTypes.ITEMSTACK.id()))
                        throw new ParseFailureException("ITEM does not accept ITEMSTACK variable: " + tokens.get(0));
                    return 1;
                }
                throw new ParseFailureException("Unknown item: " + tokens.get(0));
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                String raw = tokens.get(0);
                String normalized = raw.toUpperCase(Locale.ROOT)
                        .replace(' ', '_').replace('-', '_');
                if (knownMaterials.contains(normalized))
                    return normalized;
                VarHandle ref = env.lookupVar(raw);
                if (ref != null) {
                    if (ref.type() != null && ref.type().id().equals(RefTypes.ITEMSTACK.id()))
                        throw new ParseFailureException("ITEM does not accept ITEMSTACK variable: " + raw);
                    return ref;
                }
                throw new ParseFailureException("Unknown item: " + raw);
            }

            @Override
            public @NotNull String toJava(Object value, @NotNull CodegenAccess ctx,
                    @NotNull EnvironmentAccess env) {
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
                        "Resolves an ItemStack reference from a variable name. Does not accept possessive syntax. Falls back to the default item in the current events context.",
                        ItemStack.class.getName(),
                        List.of("set %i:ITEMSTACK% amount to 5"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    return 0;
                String raw = tokens.get(0);
                if (raw.endsWith("'s"))
                    throw new ParseFailureException("ITEMSTACK does not accept possessive form: " + raw);
                VarHandle ref = env.lookupVar(raw);
                if (ref != null && isItemStack(ref))
                    return 1;
                if (env.lookupDefault(RefTypes.ITEMSTACK) != null)
                    return 0;
                return 1;
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty()) {
                    VarHandle def = env.lookupDefault(RefTypes.ITEMSTACK);
                    if (def == null)
                        throw new ParseFailureException("No default item stack in this context");
                    return def;
                }
                String raw = tokens.get(0);
                if (raw.endsWith("'s"))
                    throw new ParseFailureException("ITEMSTACK does not accept possessive form: " + raw);
                VarHandle ref = env.lookupVar(raw);
                if (ref != null) {
                    if (!isItemStack(ref))
                        throw new ParseFailureException(raw + " is not an item stack");
                    return ref;
                }
                VarHandle def = env.lookupDefault(RefTypes.ITEMSTACK);
                if (def != null)
                    return def;
                throw new ParseFailureException("Unknown item stack reference: " + raw);
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenAccess ctx,
                    @NotNull EnvironmentAccess env) {
                if (v == null)
                    throw new RuntimeException("Cannot generate Java for null item stack reference");
                return NullGuard.codegenOrRaw((VarHandle) v);
            }

            private boolean isItemStack(@NotNull VarHandle ref) {
                return ref.type() != null && ref.type().id().equals(RefTypes.ITEMSTACK.id());
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
                        "Resolves an ItemStack reference from a possessive token (e.g. item's). The token must end with 's. Falls back to the default item in the current events context.",
                        ItemStack.class.getName(),
                        List.of("%i:ITEMSTACK_POSSESSIVE% display name"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    return 0;
                String raw = tokens.get(0);
                boolean possessive = raw.endsWith("'s");
                String name = possessive ? raw.substring(0, raw.length() - 2) : raw;
                VarHandle ref = env.lookupVar(name);
                if (ref != null && isItemStack(ref))
                    return 1;
                if (possessive) {
                    if (env.lookupDefault(RefTypes.ITEMSTACK) != null)
                        return 1;
                    return 1;
                }
                throw new ParseFailureException("Not a known item stack reference: " + raw);
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty()) {
                    VarHandle def = env.lookupDefault(RefTypes.ITEMSTACK);
                    if (def == null)
                        throw new ParseFailureException("No default item stack in this context");
                    return def;
                }
                String raw = tokens.get(0);
                boolean possessive = raw.endsWith("'s");
                String name = possessive ? raw.substring(0, raw.length() - 2) : raw;
                VarHandle ref = env.lookupVar(name);
                if (ref != null) {
                    if (!isItemStack(ref))
                        throw new ParseFailureException(name + " is not an item stack");
                    return ref;
                }
                VarHandle def = env.lookupDefault(RefTypes.ITEMSTACK);
                if (def != null)
                    return def;
                throw new ParseFailureException("Unknown item stack reference: " + name);
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenAccess ctx,
                    @NotNull EnvironmentAccess env) {
                if (v == null)
                    throw new RuntimeException("Cannot generate Java for null item stack reference");
                return NullGuard.codegenOrRaw((VarHandle) v);
            }

            private boolean isItemStack(@NotNull VarHandle ref) {
                return ref.type() != null && ref.type().id().equals(RefTypes.ITEMSTACK.id());
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
                        "Resolves a world reference from a variable name. Falls back to the default world in the current events context.",
                        World.class.getName(),
                        List.of("set time in %w:WORLD% to 0"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    return 0;
                String name = tokens.get(0);
                VarHandle ref = env.lookupVar(name);
                if (ref != null && isWorld(ref))
                    return 1;
                if (env.lookupDefault(RefTypes.WORLD) != null)
                    return 0;
                return 1;
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty()) {
                    VarHandle def = env.lookupDefault(RefTypes.WORLD);
                    if (def == null)
                        throw new ParseFailureException("No default world in this context");
                    return def;
                }
                String name = tokens.get(0);
                VarHandle ref = env.lookupVar(name);
                if (ref != null) {
                    if (!isWorld(ref))
                        throw new ParseFailureException(name + " is not a world");
                    return ref;
                }
                VarHandle def = env.lookupDefault(RefTypes.WORLD);
                if (def != null)
                    return def;
                throw new ParseFailureException("Unknown world reference: " + name);
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenAccess ctx,
                    @NotNull EnvironmentAccess env) {
                return NullGuard.codegenOrRaw((VarHandle) v);
            }

            private boolean isWorld(@NotNull VarHandle ref) {
                return ref.type() != null && ref.type().id().equals(RefTypes.WORLD.id());
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
                        "Resolves a location reference from a variable name. Falls back to the default location in the current events context.",
                        Location.class.getName(),
                        List.of("teleport player to %loc:LOCATION%"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    return 0;
                String name = tokens.get(0);
                VarHandle ref = env.lookupVar(name);
                if (ref != null && isLocation(ref))
                    return 1;
                if (env.lookupDefault(RefTypes.LOCATION) != null)
                    return 0;
                return 1;
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty()) {
                    VarHandle def = env.lookupDefault(RefTypes.LOCATION);
                    if (def == null)
                        throw new ParseFailureException("No default location in this context");
                    return def;
                }
                String name = tokens.get(0);
                VarHandle ref = env.lookupVar(name);
                if (ref != null) {
                    LumenLogger.debug("LOCATION.parse",
                            "Found var '" + name + "', type=" + (ref.type() != null ? ref.type().id() : "null"));
                    if (!isLocation(ref))
                        throw new ParseFailureException(name + " is not a location");
                    return ref;
                }
                VarHandle def = env.lookupDefault(RefTypes.LOCATION);
                if (def != null)
                    return def;
                throw new ParseFailureException("Unknown location reference: " + name);
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenAccess ctx,
                    @NotNull EnvironmentAccess env) {
                return NullGuard.codegenOrRaw((VarHandle) v);
            }

            private boolean isLocation(@NotNull VarHandle ref) {
                return ref.type() != null && ref.type().id().equals(RefTypes.LOCATION.id());
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
                        "Resolves a list variable reference. The variable must have been declared as a list type.",
                        List.class.getName(),
                        List.of("add \"hello\" to %list:LIST%"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    return 0;
                String name = tokens.get(0);
                VarHandle ref = env.lookupVar(name);
                if (ref != null && isList(ref))
                    return 1;
                throw new ParseFailureException("Expected a list variable, got '" + name + "'");
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    throw new ParseFailureException("Expected list variable name");
                String name = tokens.get(0);
                VarHandle ref = env.lookupVar(name);
                if (ref != null) {
                    if (!isList(ref))
                        throw new ParseFailureException(name + " is not a list");
                    return ref;
                }
                throw new ParseFailureException("Unknown list variable: " + name);
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenAccess ctx,
                    @NotNull EnvironmentAccess env) {
                if (v == null)
                    throw new RuntimeException("Cannot generate Java for null list reference");
                return NullGuard.codegenOrRaw((VarHandle) v);
            }

            private boolean isList(@NotNull VarHandle ref) {
                return ref.type() != null && ref.type().id().equals(RefTypes.LIST.id());
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
                        "Resolves a map variable reference. The variable must have been declared as a map type.",
                        Map.class.getName(),
                        List.of("set %map:MAP% at key \"name\" to \"value\""),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    return 0;
                String name = tokens.get(0);
                VarHandle ref = env.lookupVar(name);
                if (ref != null && isMap(ref))
                    return 1;
                throw new ParseFailureException("Expected a map variable, got '" + name + "'");
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    throw new ParseFailureException("Expected map variable name");
                String name = tokens.get(0);
                VarHandle ref = env.lookupVar(name);
                if (ref != null) {
                    if (!isMap(ref))
                        throw new ParseFailureException(name + " is not a map");
                    return ref;
                }
                throw new ParseFailureException("Unknown map variable: " + name);
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenAccess ctx,
                    @NotNull EnvironmentAccess env) {
                if (v == null)
                    throw new RuntimeException("Cannot generate Java for null map reference");
                return NullGuard.codegenOrRaw((VarHandle) v);
            }

            private boolean isMap(@NotNull VarHandle ref) {
                return ref.type() != null && ref.type().id().equals(RefTypes.MAP.id());
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
                        "Resolves a data instance variable reference. The variable must have been declared as a data type.",
                        DataInstance.class.getName(),
                        List.of("get field \"name\" of %obj:DATA%"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    return 0;
                String name = tokens.get(0);
                VarHandle ref = env.lookupVar(name);
                if (ref != null && isData(ref))
                    return 1;
                throw new ParseFailureException("Expected a data variable, got '" + name + "'");
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    throw new ParseFailureException("Expected data variable name");
                String name = tokens.get(0);
                VarHandle ref = env.lookupVar(name);
                if (ref != null) {
                    if (!isData(ref))
                        throw new ParseFailureException(name + " is not a data instance");
                    return ref;
                }
                throw new ParseFailureException("Unknown data variable: " + name);
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenAccess ctx,
                    @NotNull EnvironmentAccess env) {
                if (v == null)
                    throw new RuntimeException("Cannot generate Java for null data reference");
                return NullGuard.codegenOrRaw((VarHandle) v);
            }

            private boolean isData(@NotNull VarHandle ref) {
                return ref.type() != null && ref.type().id().equals(RefTypes.DATA.id());
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
                        "Resolves a block reference from a variable name. Falls back to the default block in the current events context.",
                        Block.class.getName(),
                        List.of("set %b:BLOCK% type to stone", "break block naturally"),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty())
                    return 0;
                String name = tokens.get(0);
                VarHandle ref = env.lookupVar(name);
                if (ref != null && isBlock(ref))
                    return 1;
                if (env.lookupDefault(RefTypes.BLOCK) != null)
                    return 0;
                return 1;
            }

            @Override
            public Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty()) {
                    VarHandle def = env.lookupDefault(RefTypes.BLOCK);
                    if (def == null)
                        throw new ParseFailureException("No default block in this context");
                    return def;
                }
                String name = tokens.get(0);
                VarHandle ref = env.lookupVar(name);
                if (ref != null) {
                    if (!isBlock(ref))
                        throw new ParseFailureException(name + " is not a block");
                    return ref;
                }
                VarHandle def = env.lookupDefault(RefTypes.BLOCK);
                if (def != null)
                    return def;
                throw new ParseFailureException("Unknown block reference: " + name);
            }

            @Override
            public @NotNull String toJava(Object v, @NotNull CodegenAccess ctx,
                    @NotNull EnvironmentAccess env) {
                if (v == null)
                    throw new RuntimeException("Cannot generate Java for null block reference");
                return NullGuard.codegenOrRaw((VarHandle) v);
            }

            private boolean isBlock(@NotNull VarHandle ref) {
                return ref.type() != null && ref.type().id().equals(RefTypes.BLOCK.id());
            }
        });
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
}

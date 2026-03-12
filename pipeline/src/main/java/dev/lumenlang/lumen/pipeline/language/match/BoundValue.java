package dev.lumenlang.lumen.pipeline.language.match;

import dev.lumenlang.lumen.pipeline.language.TypeBinding;
import dev.lumenlang.lumen.pipeline.language.pattern.Placeholder;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents a single bound parameter from a pattern match.
 *
 * <p>
 * When a placeholder like {@code %who:PLAYER%} is matched against tokens, the
 * result is stored in a BoundValue which contains:
 * <ul>
 * <li><b>ph:</b> The placeholder metadata (name="who", type="PLAYER")</li>
 * <li><b>tokens:</b> The original tokens that were consumed (e.g., ["player"])</li>
 * <li><b>value:</b> The parsed runtime value (e.g., Ref(PLAYER, "player"))</li>
 * <li><b>binding:</b> The type binding that parsed the value</li>
 * </ul>
 *
 * @param ph      the placeholder that was matched
 * @param tokens  the input tokens that were consumed
 * @param value   the parsed runtime value
 * @param binding the type binding that performed the parsing
 */
public record BoundValue(
        @NotNull Placeholder ph,
        @NotNull List<Token> tokens,
        Object value,
        @NotNull TypeBinding binding) {
}

package eevee.secretplayer;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

public final class SecretPlayerGameRules {
    public static final GameRule<Boolean> HIDE_FROM_OPERATORS =
            GameRuleBuilder.forBoolean(false)
                    .category(GameRuleCategory.MISC)
                    .buildAndRegister(
                            Identifier.fromNamespaceAndPath(
                                    "secretplayer",
                                    "hide_from_operators"
                            )
                    );

    private SecretPlayerGameRules() {
    }

    public static void initialize() {
        // Calling this method ensures the gamerule is registered.
    }
}
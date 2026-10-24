
package eevee.secretplayer.mixin;

import eevee.secretplayer.SecretPlayer;
import eevee.secretplayer.SecretPlayerState;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class PlayerListMessageMixin {
    @Shadow
    @Final
    private net.minecraft.server.MinecraftServer server;

    @Inject(
            method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void secretplayer$filterHiddenAnnouncements(
            Component message,
            boolean overlay,
            CallbackInfo ci
    ) {
        if (!(message.getContents() instanceof TranslatableContents contents)) {
            return;
        }

        String key = contents.getKey();

        boolean joinOrLeave =
                key.equals("multiplayer.player.joined")
                        || key.equals("multiplayer.player.left")
                        || key.equals("multiplayer.player.joined.renamed");

        boolean advancement =
                key.equals("chat.type.advancement.task")
                        || key.equals("chat.type.advancement.goal")
                        || key.equals("chat.type.advancement.challenge");

        if (!joinOrLeave && !advancement) {
            return;
        }

        Object[] args = contents.getArgs();
        if (args.length == 0) {
            return;
        }

        String playerName = args[0] instanceof Component component
                ? component.getString()
                : String.valueOf(args[0]);

        ServerPlayer hiddenPlayer = null;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (SecretPlayerState.HIDDEN.contains(player.getUUID())
                    && (player.getGameProfile().name().equals(playerName)
                    || player.getName().getString().equals(playerName))) {
                hiddenPlayer = player;
                break;
            }
        }

        if (hiddenPlayer == null) {
            return;
        }

        boolean hideFromOps = SecretPlayer.hideFromOperators(server);

        // Replace the original global broadcast with per-player delivery.
        ci.cancel();

        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            boolean isOperator = Commands.LEVEL_GAMEMASTERS
                    .check(viewer.permissions());

            // Hidden players' announcements are visible only to operators
            // unless the gamerule also hides them from operators.
            if (isOperator && !hideFromOps) {
                viewer.sendSystemMessage(message, overlay);
            }
        }
    }
}

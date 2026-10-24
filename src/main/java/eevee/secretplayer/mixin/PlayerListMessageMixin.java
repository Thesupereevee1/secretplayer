package eevee.secretplayer.mixin;

import eevee.secretplayer.SecretPlayerState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.network.Connection;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerList.class)
public abstract class PlayerListMessageMixin {
    @Redirect(
            method = "placeNewPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"
            )
    )
    private void secretplayer$hideJoinAnnouncement(
            PlayerList instance,
            Component message,
            boolean overlay,
            Connection connection,
            ServerPlayer joiningPlayer,
            CommonListenerCookie cookie
    ) {
        if (!SecretPlayerState.HIDDEN.contains(joiningPlayer.getUUID())) {
            instance.broadcastSystemMessage(message, overlay);
        }
    }
    @Inject(
            method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )

    private void secretplayer$hideJoinLeaveMessage(
            Component message,
            boolean overlay,
            CallbackInfo ci
    ) {
        if (!(message.getContents() instanceof TranslatableContents contents)) {
            return;
        }

        String key = contents.getKey();

        if (!key.equals("multiplayer.player.joined")
                && !key.equals("multiplayer.player.left")
                && !key.equals("multiplayer.player.joined.renamed")) {
            return;
        }

        Object[] args = contents.getArgs();
        if (args.length == 0) return;

        String playerName = args[0] instanceof Component component
                ? component.getString()
                : String.valueOf(args[0]);

        PlayerList playerList = (PlayerList) (Object) this;

        for (ServerPlayer player : playerList.getPlayers()) {
            if (SecretPlayerState.HIDDEN.contains(player.getUUID())
                    && (player.getGameProfile().name().equals(playerName)
                    || player.getName().getString().equals(playerName))) {
                ci.cancel();
                return;
            }
        }
    }
}
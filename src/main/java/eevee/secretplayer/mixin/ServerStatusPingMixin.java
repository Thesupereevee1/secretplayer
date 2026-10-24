package eevee.secretplayer.mixin;

import eevee.secretplayer.SecretPlayerState;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;
import net.minecraft.server.network.ServerStatusPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ServerStatusPacketListenerImpl.class)
public abstract class ServerStatusPingMixin {
    @Shadow @Final private Connection connection;
    @Shadow @Final private ServerStatus status;
    @Shadow private boolean hasRequestedStatus;

    @Inject(
            method = "handleStatusRequest",
            at = @At("HEAD"),
            cancellable = true
    )
    private void secretplayer$hidePlayersFromPing(
            ServerboundStatusRequestPacket packet,
            CallbackInfo ci
    ) {
        if (hasRequestedStatus) return;

        Optional<ServerStatus.Players> playersOptional = status.players();
        if (playersOptional.isEmpty()) return;

        ServerStatus.Players players = playersOptional.get();

        var filteredSample = players.sample().stream()
                .filter(player ->
                        !SecretPlayerState.HIDDEN.contains(player.id()))
                .toList();

        if (filteredSample.size() == players.sample().size()) return;

        int hiddenOnlinePlayers = (int) SecretPlayerState.HIDDEN.stream()
                .filter(uuid -> status.players().isPresent())
                .filter(uuid -> SecretPlayerState.HIDDEN.contains(uuid))
                .count();

        ServerStatus.Players filteredPlayers =
                new ServerStatus.Players(
                        players.max(),
                        Math.max(0, players.online() - hiddenOnlinePlayers),
                        filteredSample
                );

        ServerStatus filteredStatus = new ServerStatus(
                status.description(),
                Optional.of(filteredPlayers),
                status.version(),
                status.favicon(),
                status.enforcesSecureChat()
        );

        hasRequestedStatus = true;
        connection.send(new ClientboundStatusResponsePacket(filteredStatus));
        ci.cancel();
    }
}
package eevee.secretplayer.mixin;

import com.mojang.authlib.GameProfile;
import eevee.secretplayer.SecretPlayer;
import eevee.secretplayer.SecretPlayerState;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.commands.Commands;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerPacketFilterMixin {
    @Shadow @Final protected Connection connection;
    @Shadow protected abstract GameProfile playerProfile();

    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void secretplayer$filterPlayerInfo(
            Packet<?> packet,
            CallbackInfo ci
    ) {
        if (secretplayer$filter(packet, null)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void secretplayer$filterPlayerInfoWithCallback(
            Packet<?> packet,
            ChannelFutureListener listener,
            CallbackInfo ci
    ) {
        if (secretplayer$filter(packet, listener)) {
            ci.cancel();
        }
    }

    private boolean secretplayer$filter(
            Packet<?> packet,
            ChannelFutureListener listener
    ) {
        if (!(packet instanceof ClientboundPlayerInfoUpdatePacket update)) {
            return false;
        }

        MinecraftServer server = SecretPlayer.SERVER;
        if (server == null) return false;

        ServerPlayer viewer =
                server.getPlayerList().getPlayer(playerProfile().id());

        if (viewer != null
                && Commands.LEVEL_GAMEMASTERS.check(viewer.permissions())
                && !SecretPlayer.hideFromOperators(server)) {
            return false;
        }

        List<ClientboundPlayerInfoUpdatePacket.Entry> filtered =
                update.entries().stream()
                        .filter(entry ->
                                !SecretPlayerState.HIDDEN.contains(entry.profileId())
                                        || entry.profileId().equals(playerProfile().id()))                        .toList();

        if (filtered.size() == update.entries().size()) {
            return false;
        }

        if (filtered.isEmpty()) {
            return true;
        }

        ClientboundPlayerInfoUpdatePacket replacement =
                new ClientboundPlayerInfoUpdatePacket(update.actions(), List.of());

        ((PlayerInfoUpdatePacketAccessor) replacement)
                .secretplayer$setEntries(filtered);

        connection.send(replacement, listener);
        return true;
    }
}
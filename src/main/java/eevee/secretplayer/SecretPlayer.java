package eevee.secretplayer;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

public class SecretPlayer implements ModInitializer {
	public static volatile MinecraftServer SERVER;

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			SERVER = server;
			SecretPlayerState.load(server);
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			SecretPlayerState.save(server);
			SERVER = null;
		});

		CommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess, environment) ->
						dispatcher.register(
								Commands.literal("secretplayer")
										.requires(Commands.hasPermission(
												Commands.LEVEL_GAMEMASTERS))

										// Existing name-based commands
										.then(Commands.literal("hide")
												.then(Commands.argument(
																"player",
																StringArgumentType.word())
														.executes(context -> {
															var source = context.getSource();
															String name = StringArgumentType.getString(
																	context, "player");
															ServerPlayer target = source.getServer()
																	.getPlayerList()
																	.getPlayerByName(name);

															if (target == null) {
																source.sendFailure(Component.literal(
																		"That player is not online. "
																				+ "Use /secretplayer uuid hide <UUID> "
																				+ "for offline players."));
																return 0;
															}

															hideUuid(source.getServer(),
																	target.getUUID());

															source.sendSuccess(
																	() -> Component.literal(
																			"Hidden " + name + "."),
																	true);
															return 1;
														})))

										.then(Commands.literal("show")
												.then(Commands.argument(
																"player",
																StringArgumentType.word())
														.executes(context -> {
															var source = context.getSource();
															String name = StringArgumentType.getString(
																	context, "player");
															ServerPlayer target = source.getServer()
																	.getPlayerList()
																	.getPlayerByName(name);

															if (target == null) {
																source.sendFailure(Component.literal(
																		"That player is not online. "
																				+ "Use /secretplayer uuid show <UUID> "
																				+ "for offline players."));
																return 0;
															}

															showUuid(source.getServer(),
																	target.getUUID());

															source.sendSuccess(
																	() -> Component.literal(
																			"Shown " + name + " again."),
																	true);
															return 1;
														})))

										// UUID-based commands work for offline players too
										.then(Commands.literal("uuid")
												.then(Commands.literal("hide")
														.then(Commands.argument(
																		"uuid",
																		StringArgumentType.word())
																.executes(context -> {
																	var source = context.getSource();
																	UUID uuid = parseUuid(
																			source,
																			StringArgumentType.getString(
																					context, "uuid"));
																	if (uuid == null) return 0;

																	hideUuid(source.getServer(), uuid);

																	source.sendSuccess(
																			() -> Component.literal(
																					"Hidden UUID " + uuid + "."),
																			true);
																	return 1;
																})))
												.then(Commands.literal("show")
														.then(Commands.argument(
																		"uuid",
																		StringArgumentType.word())
																.executes(context -> {
																	var source = context.getSource();
																	UUID uuid = parseUuid(
																			source,
																			StringArgumentType.getString(
																					context, "uuid"));
																	if (uuid == null) return 0;

																	showUuid(source.getServer(), uuid);

																	source.sendSuccess(
																			() -> Component.literal(
																					"Shown UUID " + uuid + " again."),
																			true);
																	return 1;
																})))
										)
						)
		);
	}

	private static UUID parseUuid(
			net.minecraft.commands.CommandSourceStack source,
			String value
	) {
		try {
			return UUID.fromString(value);
		} catch (IllegalArgumentException e) {
			source.sendFailure(Component.literal(
					"Invalid UUID. Use the full UUID, for example "
							+ "123e4567-e89b-12d3-a456-426614174000"));
			return null;
		}
	}

	private static void hideUuid(MinecraftServer server, UUID uuid) {
		SecretPlayerState.HIDDEN.add(uuid);
		SecretPlayerState.save(server);

		// Remove the player from regular players' TAB lists if they're online.
		for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
			if (!Commands.LEVEL_GAMEMASTERS.check(viewer.permissions())
					&& !viewer.getUUID().equals(uuid)) {
				viewer.connection.send(
						new ClientboundPlayerInfoRemovePacket(List.of(uuid)));
			}
		}
	}

	private static void showUuid(MinecraftServer server, UUID uuid) {
		SecretPlayerState.HIDDEN.remove(uuid);
		SecretPlayerState.save(server);

		// Restore the entry immediately if the target is online.
		ServerPlayer target = server.getPlayerList().getPlayer(uuid);
		if (target == null) return;

		ClientboundPlayerInfoUpdatePacket packet =
				ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(
						List.of(target));

		for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
			viewer.connection.send(packet);
		}
	}
}
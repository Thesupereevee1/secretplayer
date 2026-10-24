package eevee.secretplayer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SecretPlayerState {
    private SecretPlayerState() {}

    public static final Set<UUID> HIDDEN = ConcurrentHashMap.newKeySet();

    private static Path getFile(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT)
                .resolve("secretplayer-hidden.txt");
    }

    public static void load(MinecraftServer server) {
        Path file = getFile(server);

        HIDDEN.clear();

        if (!Files.exists(file)) {
            return;
        }

        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                try {
                    HIDDEN.add(UUID.fromString(line.trim()));
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid lines.
                }
            }

            System.out.println("[SecretPlayer] Loaded " + HIDDEN.size()
                    + " hidden player UUID(s).");
        } catch (IOException e) {
            System.err.println("[SecretPlayer] Could not load hidden players.");
            e.printStackTrace();
        }
    }

    public static void save(MinecraftServer server) {
        Path file = getFile(server);

        try {
            Files.createDirectories(file.getParent());

            Files.write(
                    file,
                    HIDDEN.stream()
                            .map(UUID::toString)
                            .sorted()
                            .toList(),
                    StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            System.err.println("[SecretPlayer] Could not save hidden players.");
            e.printStackTrace();
        }
    }
}
# SecretPlayer

**SecretPlayer** is a server-side Fabric mod that lets server operators hide selected players from regular players without kicking them or affecting their ability to play.

### Features
- **Hide players:** Remove selected players from the TAB list for non-operator players.
- **Hide offline players:** Use UUID commands to hide players even when they are offline.
- **Persistent hiding:** Hidden player UUIDs are saved and remain hidden after server restarts.
- **Hide server-list player samples:** Hidden players are excluded from the multiplayer server-list hover sample and online-player count.
- **Hide join and leave messages:** Suppress join and leave announcements for hidden players.
- **Restore visibility:** Unhide players at any time using their username or UUID.
- **Server-side only:** Players don't need to install the mod on their own clients.

### Commands

- `/secretplayer hide <player>` — Hide an online player.
- `/secretplayer show <player>` — Restore an online player's visibility.
- `/secretplayer uuid hide <UUID>` — Hide a player by UUID, even when offline.
- `/secretplayer uuid show <UUID>` — Restore visibility by UUID.

Commands require operator-level permission.

**Compatibility:** Minecraft 26.3, Fabric Loader 0.19.5 or newer.

SecretPlayer is designed for server administration and minigames, keeping selected players connected and able to play normally while removing them from TAB and playerlist.

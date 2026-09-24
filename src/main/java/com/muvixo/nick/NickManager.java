package com.muvixo.nick;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class NickManager implements Listener {

    private final JavaPlugin plugin;
    private final NickStorage storage;
    private final List<String> namePool = new ArrayList<>();
    private final Map<UUID, String> activeNicks = new HashMap<>();
    private final Map<UUID, String> activeRanks = new HashMap<>();
    private final Random random = new Random();
    private static final String TEAM_PREFIX = "nick_";

    public NickManager(JavaPlugin plugin, NickStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
        loadNamePool();
    }

    private void loadNamePool() {
        namePool.clear();
        try {
            InputStream in = plugin.getResource("names.txt");
            if (in == null) {
                plugin.getLogger().warning("names.txt not found inside the plugin jar - /nick random will use fallback names.");
                return;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) namePool.add(line);
            }
            reader.close();
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to load names.txt: " + t.getMessage());
        }
        plugin.getLogger().info("Loaded " + namePool.size() + " nicknames.");
    }

    public void reload() { loadNamePool(); }
    public int getNamePoolSize() { return namePool.size(); }
    public String randomName() {
        if (namePool.isEmpty()) return "Player" + (1000 + random.nextInt(9000));
        return namePool.get(random.nextInt(namePool.size()));
    }

    /**
     * Applies a nickname + rank tag to a player.
     * The rank is NOT a permission rank - it is a cosmetic prefix that gets
     * chained directly in front of the nickname (e.g. "[VIP+] Steve"). That
     * combined text is what appears in the tab list, above the player's head,
     * and in chat.
     */
    public void applyNick(final Player player, final String nickname, final String rank) {
        if (player == null || !player.isOnline()) return;

        activeNicks.put(player.getUniqueId(), nickname);
        activeRanks.put(player.getUniqueId(), rank);

        removeTeam(player);

        // Update GameProfile (TAB list identity + what the client uses for the nametag)
        if (Bukkit.isPrimaryThread()) {
            NMS.updateName(player, nickname);
        } else {
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override public void run() { NMS.updateName(player, nickname); }
            });
        }

        // Update Display Name (chat/nametag base) - prefix is baked in directly so
        // it shows even if the server's tab plugin ignores scoreboard teams.
        String prefix = color(rankPrefix(rank));
        player.setDisplayName(prefix + nickname + ChatColor.RESET);
        player.setPlayerListName(prefix + nickname + ChatColor.RESET);

        // Apply Scoreboard Team (nametag-above-head prefix)
        applyTeam(player, nickname, rank);
    }

    public void removeNick(final Player player) {
        if (player == null || !player.isOnline()) return;
        final UUID id = player.getUniqueId();

        activeNicks.remove(id);
        activeRanks.remove(id);

        // Remove the rank team immediately so the prefix disappears above the player's head
        removeTeam(player);

        // Restore the real profile name in TAB (and therefore the nametag, since the
        // client renders the overhead nametag from the same cached identity)
        if (Bukkit.isPrimaryThread()) {
            NMS.updateName(player, player.getName());
        } else {
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override public void run() {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) NMS.updateName(p, p.getName());
                }
            });
        }

        // Reset Bukkit-facing names used for chat and default tab rendering
        player.setDisplayName(player.getName());
        player.setPlayerListName(player.getName());

        // Clear persisted data so a relog doesn't bring the old nick back
        storage.clearNickname(id);
        storage.clearNickRank(id);
    }

    private void applyTeam(Player player, String nickname, String rank) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = teamName(player);
        if (teamName.length() > 16) teamName = teamName.substring(0, 16);

        // Cleanup any stale team entries for this player under either identity
        for (Team t : new ArrayList<>(board.getTeams())) {
            if (t.hasEntry(player.getName()) || t.hasEntry(nickname)) {
                t.removeEntry(player.getName());
                t.removeEntry(nickname);
            }
        }
        Team old = board.getTeam(teamName);
        if (old != null) old.unregister();

        Team team = board.registerNewTeam(teamName);

        // IMPORTANT: register BOTH identities on the same team.
        // Depending on server/client state, the overhead nametag can be matched
        // against the player's real username while the tab list is matched
        // against the spoofed nickname (or vice versa if NMS spoofing failed).
        // Adding both entries means the rank prefix is "chained" to the name
        // correctly everywhere, regardless of which identity is being matched.
        team.addEntry(player.getName());
        team.addEntry(nickname);

        String prefix = rankPrefix(rank);
        if (prefix == null) prefix = "";
        if (prefix.length() > 14) prefix = prefix.substring(0, 14);

        team.setPrefix(color(prefix));
        team.setSuffix("");
    }

    private void removeTeam(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = teamName(player);
        if (teamName.length() > 16) teamName = teamName.substring(0, 16);

        Team team = board.getTeam(teamName);
        if (team != null) {
            for (String entry : new ArrayList<>(team.getEntries())) {
                team.removeEntry(entry);
            }
            team.unregister();
        }
    }

    private String teamName(Player player) {
        String uuid = player.getUniqueId().toString().replace("-", "");
        return TEAM_PREFIX + uuid.substring(0, 8);
    }

    private String rankPrefix(String rank) {
        if (rank == null) rank = "default";
        return plugin.getConfig().getString("ranks." + rank + ".prefix", "&7");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        String savedNick = storage.getNickname(uuid);
        if (savedNick == null || savedNick.isEmpty()) return;

        String savedRank = storage.getNickRank(uuid);
        if (savedRank == null || savedRank.isEmpty()) {
            savedRank = plugin.getConfig().getString("default-nick-rank", "default");
        }

        final String nickToApply = savedNick;
        final String rankToApply = savedRank;

        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override public void run() {
                if (player.isOnline()) applyNick(player, nickToApply, rankToApply);
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        final String nick = activeNicks.get(player.getUniqueId());
        if (nick == null) return;

        final String rank = activeRanks.get(player.getUniqueId());

        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override public void run() {
                if (player.isOnline()) applyNick(player, nick, rank == null ? "default" : rank);
            }
        }, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        String nick = activeNicks.get(id);
        if (nick != null) {
            storage.setNickname(id, nick);
            String rank = activeRanks.get(id);
            if (rank != null) storage.setNickRank(id, rank);
        }
        removeTeam(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String nick = activeNicks.get(player.getUniqueId());
        if (nick == null) return;

        String rank = activeRanks.get(player.getUniqueId());
        String prefix = color(rankPrefix(rank));

        String format = event.getFormat();
        if (format != null) {
            event.setFormat(format.replace("%1$s", prefix + nick + ChatColor.RESET));
        }
    }

    public void restoreAllOnline() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            String nick = storage.getNickname(p.getUniqueId());
            if (nick != null && !nick.isEmpty()) {
                String rank = storage.getNickRank(p.getUniqueId());
                if (rank == null || rank.isEmpty())
                    rank = plugin.getConfig().getString("default-nick-rank", "default");
                applyNick(p, nick, rank);
            }
        }
    }

    public void saveAll() {
        for (Map.Entry<UUID, String> e : activeNicks.entrySet()) {
            storage.setNickname(e.getKey(), e.getValue());
            String rank = activeRanks.get(e.getKey());
            if (rank != null) storage.setNickRank(e.getKey(), rank);
        }
        storage.save();
    }

    public boolean isNicked(Player player) { return activeNicks.containsKey(player.getUniqueId()); }
    public String getNick(Player player) { return activeNicks.get(player.getUniqueId()); }
    public String getRank(Player player) { return activeRanks.get(player.getUniqueId()); }
    public NickStorage getStorage() { return storage; }

    private String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}

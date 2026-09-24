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

/**
 * Core nickname manager.
 * Author: Muvixo
 * Theme : Aqua & White
 */
public class NickManager implements Listener {

    private final JavaPlugin plugin;
    private final NickStorage storage;

    private final List<String> namePool = new ArrayList<String>();
    private final Map<UUID, String> activeNicks = new HashMap<UUID, String>();
    private final Map<UUID, String> activeRanks = new HashMap<UUID, String>();
    private final Random random = new Random();

    private final String AQUA;
    private final String WHITE;
    private final String DARK_AQUA;
    private final String MUTED;
    private final String BOLD;

    public NickManager(JavaPlugin plugin, NickStorage storage) {
        this.plugin = plugin;
        this.storage = storage;

        this.AQUA      = color(plugin.getConfig().getString("colors.primary", "&b"));
        this.WHITE     = color(plugin.getConfig().getString("colors.secondary", "&f"));
        this.DARK_AQUA = color(plugin.getConfig().getString("colors.accent", "&3"));
        this.MUTED     = color(plugin.getConfig().getString("colors.muted", "&7"));
        this.BOLD      = color(plugin.getConfig().getString("colors.bold", "&l"));

        loadNamePool();
    }

    private void loadNamePool() {
        namePool.clear();
        try {
            InputStream in = plugin.getResource("names.txt");
            if (in == null) {
                plugin.getLogger().warning("names.txt not found inside the jar.");
                return;
            }
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) namePool.add(line);
            }
            reader.close();
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to load names.txt: " + t.getMessage());
        }
        plugin.getLogger().info("Loaded " + namePool.size() + " nicknames from names.txt.");
    }

    public void reload() { loadNamePool(); }

    public int getNamePoolSize() { return namePool.size(); }

    public String randomName() {
        if (namePool.isEmpty()) return "Player" + (1000 + random.nextInt(9000));
        return namePool.get(random.nextInt(namePool.size()));
    }

    public void applyNick(Player player, String nickname, String rank) {
        if (player == null || !player.isOnline()) return;

        activeNicks.put(player.getUniqueId(), nickname);
        activeRanks.put(player.getUniqueId(), rank);

        if (plugin.getConfig().getBoolean("nick.show-in-chat", true)) {
            player.setDisplayName(AQUA + nickname + ChatColor.RESET);
        }
        if (plugin.getConfig().getBoolean("nick.show-in-tab", true)) {
            player.setPlayerListName(AQUA + nickname + ChatColor.RESET);
        }
        if (plugin.getConfig().getBoolean("nick.show-in-nametag", true)) {
            applyNametagAndTab(player, nickname, rank);
        }
    }

    public void removeNick(Player player) {
        if (player == null) return;

        activeNicks.remove(player.getUniqueId());
        activeRanks.remove(player.getUniqueId());

        player.setDisplayName(WHITE + player.getName() + ChatColor.RESET);
        player.setPlayerListName(WHITE + player.getName() + ChatColor.RESET);

        removeNametag(player);
    }

    private void applyNametagAndTab(Player player, String nickname, String rank) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = teamNameFor(player);

        for (Team t : board.getTeams()) {
            if (t.hasEntry(player.getName())) t.removeEntry(player.getName());
        }

        Team team = board.getTeam(teamName);
        if (team == null) team = board.registerNewTeam(teamName);
        team.addEntry(player.getName());

        String prefix = getRankPrefix(rank);
        if (prefix.length() > 16) prefix = prefix.substring(0, 16);
        team.setPrefix(prefix);

        String suffix = AQUA + nickname;
        if (suffix.length() > 16) suffix = suffix.substring(0, 16);
        team.setSuffix(suffix);
    }

    private void removeNametag(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam(teamNameFor(player));
        if (team != null) {
            if (team.hasEntry(player.getName())) team.removeEntry(player.getName());
            if (team.getEntries().isEmpty()) team.unregister();
        }
    }

    private String teamNameFor(Player player) {
        String uuid = player.getUniqueId().toString().replace("-", "");
        return "nick_" + uuid.substring(0, Math.min(11, uuid.length()));
    }

    private String getRankPrefix(String rank) {
        if (rank == null) rank = "default";
        String prefix = plugin.getConfig().getString("ranks." + rank + ".prefix", "&7");
        return color(prefix);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        String savedNick = storage.getNickname(player.getUniqueId());
        if (savedNick != null && !savedNick.isEmpty()) {
            String savedRank = storage.getNickRank(player.getUniqueId());
            if (savedRank == null || savedRank.isEmpty()) {
                savedRank = plugin.getConfig().getString("default-nick-rank", "default");
            }
            final String nick = savedNick;
            final String rank = savedRank;

            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override public void run() {
                    if (player.isOnline()) applyNick(player, nick, rank);
                }
            }, 5L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        final String nick = activeNicks.get(player.getUniqueId());
        if (nick != null) {
            final String rank = activeRanks.get(player.getUniqueId());
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override public void run() {
                    if (player.isOnline()) applyNick(player, nick, rank == null ? "default" : rank);
                }
            }, 5L);
        }
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
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String nick = activeNicks.get(player.getUniqueId());
        if (nick == null) return;

        if (!plugin.getConfig().getBoolean("nick.show-in-chat", true)) return;

        String format = event.getFormat();
        if (format != null && format.contains("%1$s")) {
            event.setFormat(format.replace("%1$s", AQUA + nick + ChatColor.RESET));
        }
    }

    public void restoreAllOnline() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            String nick = storage.getNickname(p.getUniqueId());
            if (nick != null && !nick.isEmpty()) {
                String rank = storage.getNickRank(p.getUniqueId());
                if (rank == null || rank.isEmpty()) {
                    rank = plugin.getConfig().getString("default-nick-rank", "default");
                }
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

    public boolean isNicked(Player player) {
        return activeNicks.containsKey(player.getUniqueId());
    }

    public boolean isNickedByUuid(UUID id) {
        return activeNicks.containsKey(id);
    }

    public String getNick(Player player) {
        return activeNicks.get(player.getUniqueId());
    }

    public String getNickByUuid(UUID id) {
        return activeNicks.get(id);
    }

    public String getRank(Player player) {
        return activeRanks.get(player.getUniqueId());
    }

    public Map<UUID, String> getActiveNicks() { return activeNicks; }
    public NickStorage getStorage() { return storage; }

    private String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}

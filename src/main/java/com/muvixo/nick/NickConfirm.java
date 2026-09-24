package com.muvixo.nick;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class NickConfirm {

    private final NickPlugin plugin;
    private final NickManager manager;

    public NickConfirm(NickPlugin plugin, NickManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void applyRandom(final Player player) {
        new BukkitRunnable() {
            @Override public void run() {
                String nick = manager.randomName();
                int tries = 0;
                while (nickTaken(nick) && tries++ < 20) nick = manager.randomName();
                if (nickTaken(nick)) {
                    player.sendMessage(ChatColor.RED + "All nicknames are taken. Try again.");
                    return;
                }
                String rank = plugin.getConfig().getString("default-nick-rank", "default");
                manager.getStorage().setNickname(player.getUniqueId(), nick);
                manager.getStorage().setNickRank(player.getUniqueId(), rank);
                manager.applyNick(player, nick, rank);
                player.sendMessage(ChatColor.AQUA + "Your nickname is now: " + ChatColor.WHITE + nick);
            }
        }.runTask(plugin);
    }

    private boolean nickTaken(String nick) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            String n = manager.getNick(online);
            if (n != null && n.equalsIgnoreCase(nick)) return true;
        }
        return false;
    }
}

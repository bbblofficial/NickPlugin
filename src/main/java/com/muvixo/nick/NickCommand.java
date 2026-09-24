package com.muvixo.nick;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * /nick command handler.
 * Author: Muvixo
 */
public class NickCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final NickManager manager;

    private static final List<String> SUB = Arrays.asList(
            "random", "reset", "rank", "skin", "list", "reload", "help"
    );

    public NickCommand(JavaPlugin plugin, NickManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private String msg(String key) {
        String raw = plugin.getConfig().getString("messages." + key, "&cMissing message: " + key);
        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&bNick&8] &f");
        return ChatColor.translateAlternateColorCodes('&', prefix + raw);
    }

    private String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private boolean validNick(String nick) {
        int min = plugin.getConfig().getInt("nick.min-length", 3);
        int max = plugin.getConfig().getInt("nick.max-length", 16);
        if (nick.length() < min || nick.length() > max) return false;
        String regex = plugin.getConfig().getString("nick.allowed-regex", "^[A-Za-z0-9_]+$");
        return java.util.regex.Pattern.matches(regex, nick);
    }

    private boolean nickTaken(String nick) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            String n = manager.getNick(online);
            if (n != null && n.equalsIgnoreCase(nick)) return true;
        }
        return false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }

        String sub = args[0].toLowerCase();

        if (sub.equals("list")) {
            if (!sender.hasPermission("nick.use")) { sender.sendMessage(msg("no-permission")); return true; }
            sender.sendMessage(color("&8&m---------------- &bNicknames &8&m----------------"));
            int count = 0;
            for (Player p : Bukkit.getOnlinePlayers()) {
                String n = manager.getNick(p);
                if (n != null) {
                    String line = plugin.getConfig()
                            .getString("messages.nick-list-entry", "&b%player% &8-> &f%nick%")
                            .replace("%player%", p.getName())
                            .replace("%nick%", n);
                    sender.sendMessage(color(line));
                    count++;
                }
            }
            if (count == 0) {
                sender.sendMessage(color(plugin.getConfig()
                        .getString("messages.nick-list-empty", "&7No nicknamed players online.")));
            }
            sender.sendMessage(color("&8&m----------------------------------------"));
            return true;
        }

        if (sub.equals("reload")) {
            if (!sender.hasPermission("nick.admin")) { sender.sendMessage(msg("no-permission")); return true; }
            try {
                ((NickPlugin) plugin).reloadEverything();
                sender.sendMessage(msg("reload-done"));
            } catch (Throwable t) {
                String err = plugin.getConfig()
                        .getString("messages.reload-failed", "Reload failed: %error%")
                        .replace("%error%", t.getMessage() == null ? "unknown" : t.getMessage());
                sender.sendMessage(color(err));
            }
            return true;
        }

        if (sub.equals("help")) { sendHelp(sender); return true; }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;

        if (sub.equals("reset")) {
            if (!player.hasPermission("nick.reset")) { player.sendMessage(msg("no-permission")); return true; }
            manager.getStorage().clearNickname(player.getUniqueId());
            manager.getStorage().clearNickRank(player.getUniqueId());
            manager.removeNick(player);
            player.sendMessage(msg("nick-reset"));
            return true;
        }

        if (sub.equals("random")) {
            if (!player.hasPermission("nick.random")) { player.sendMessage(msg("no-permission")); return true; }
            String nick = manager.randomName();
            int tries = 0;
            while (nickTaken(nick) && tries++ < 20) nick = manager.randomName();
            if (nickTaken(nick)) { player.sendMessage(msg("nick-taken")); return true; }

            String rank = plugin.getConfig().getString("default-nick-rank", "default");
            manager.getStorage().setNickname(player.getUniqueId(), nick);
            manager.getStorage().setNickRank(player.getUniqueId(), rank);
            manager.applyNick(player, nick, rank);

            player.sendMessage(msg("nick-random").replace("%nick%", nick));
            return true;
        }

        if (sub.equals("rank")) {
            if (!player.hasPermission("nick.rank")) { player.sendMessage(msg("no-permission")); return true; }
            if (args.length < 2) { player.sendMessage(color("&bUsage: &f/nick rank <rank>")); return true; }
            String rank = args[1].toLowerCase();
            if (!plugin.getConfig().contains("ranks." + rank)) {
                player.sendMessage(color("&cUnknown rank: &f" + rank));
                return true;
            }
            String nick = manager.getNick(player);
            if (nick == null) { player.sendMessage(color("&cYou must have a nickname first. Use /nick random")); return true; }
            manager.getStorage().setNickRank(player.getUniqueId(), rank);
            manager.applyNick(player, nick, rank);
            String display = plugin.getConfig().getString("ranks." + rank + ".display", rank);
            player.sendMessage(msg("nick-rank-set").replace("%rank%", display));
            return true;
        }

        if (sub.equals("skin")) {
            if (!player.hasPermission("nick.skin")) { player.sendMessage(msg("no-permission")); return true; }
            player.sendMessage(msg("skin-not-supported"));
            return true;
        }

        if (args.length >= 2 && args[1].equalsIgnoreCase("reset") && !sub.equals("reset")) {
            if (!player.hasPermission("nick.others")) { player.sendMessage(msg("no-permission")); return true; }
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                player.sendMessage(msg("player-not-found").replace("%player%", args[0]));
                return true;
            }
            manager.getStorage().clearNickname(target.getUniqueId());
            manager.getStorage().clearNickRank(target.getUniqueId());
            manager.removeNick(target);
            player.sendMessage(msg("nick-other-reset").replace("%player%", target.getName()));
            target.sendMessage(msg("nick-reset"));
            return true;
        }

        String nick = args[0];
        if (!validNick(nick)) { player.sendMessage(msg("nick-invalid")); return true; }
        if (nickTaken(nick)) { player.sendMessage(msg("nick-taken")); return true; }

        String rank = plugin.getConfig().getString("default-nick-rank", "default");
        manager.getStorage().setNickname(player.getUniqueId(), nick);
        manager.getStorage().setNickRank(player.getUniqueId(), rank);
        manager.applyNick(player, nick, rank);

        player.sendMessage(msg("nick-set").replace("%nick%", nick));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(color("&8&m---------------- &bNick &8&m----------------"));
        sender.sendMessage(color("&b/nick &f<name>          &7Set a specific nickname"));
        sender.sendMessage(color("&b/nick random          &7Pick a random name from names.txt"));
        sender.sendMessage(color("&b/nick reset           &7Remove your nickname"));
        sender.sendMessage(color("&b/nick rank &f<rank>     &7Set display rank while nicked"));
        sender.sendMessage(color("&b/nick list            &7List nicked players"));
        sender.sendMessage(color("&b/nick reload          &7Reload config + names.txt"));
        sender.sendMessage(color("&8&m----------------------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (String s : SUB) if (s.startsWith(prefix)) out.add(s);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix)) out.add(p.getName());
            }
            return out;
        }
        if (args.length == 2) {
            String first = args[0].toLowerCase();
            if (first.equals("rank")) {
                if (plugin.getConfig().getConfigurationSection("ranks") != null) {
                    for (String r : plugin.getConfig().getConfigurationSection("ranks").getKeys(false)) {
                        if (r.startsWith(args[1].toLowerCase())) out.add(r);
                    }
                }
            } else if (first.equals("skin")) {
                out.add("<playername>");
            } else {
                out.add("reset");
            }
            return out;
        }
        return out;
    }
}

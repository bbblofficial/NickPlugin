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

public class NickCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final NickManager manager;
    private final NickConfirm confirm;

    private static final List<String> SUB = Arrays.asList(
            "random", "reset", "rank", "list", "reload", "help", "ok", "no"
    );

    public NickCommand(JavaPlugin plugin, NickManager manager, NickConfirm confirm) {
        this.plugin = plugin;
        this.manager = manager;
        this.confirm = confirm;
    }

    private String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private String msg(String key) {
        String raw = plugin.getConfig().getString("messages." + key, "&cMissing: " + key);
        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&bNick&8] &f");
        return color(prefix + raw);
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

        if (args.length == 0) {
            if (sender instanceof Player) {
                if (NMS.isAvailable()) {
                    ((NickPlugin) plugin).getGui().openConfirmBook((Player) sender);
                } else {
                    // Book menu relies on server internals that aren't available here -
                    // fail gracefully instead of doing nothing / erroring silently.
                    sender.sendMessage(msg("menu-unavailable"));
                    sendHelp(sender);
                }
            } else {
                sendHelp(sender);
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("ok")) {
            if (!(sender instanceof Player)) return true;
            if (!sender.hasPermission("nick.random")) { sender.sendMessage(msg("no-permission")); return true; }
            confirm.applyRandom((Player) sender);
            return true;
        }

        if (sub.equals("no")) {
            if (!(sender instanceof Player)) return true;
            sender.sendMessage(ChatColor.RED + "Nick canceled.");
            return true;
        }

        if (sub.equals("list")) {
            if (!sender.hasPermission("nick.use")) { sender.sendMessage(msg("no-permission")); return true; }
            sender.sendMessage(color("&8&m---------- &bNicknames &8&m----------"));
            int count = 0;
            for (Player p : Bukkit.getOnlinePlayers()) {
                String n = manager.getNick(p);
                if (n != null) { sender.sendMessage(color("&b" + p.getName() + " &8-> &f" + n)); count++; }
            }
            if (count == 0) sender.sendMessage(color("&7No nicknamed players online."));
            sender.sendMessage(color("&8&m----------------------------------"));
            return true;
        }

        if (sub.equals("reload")) {
            if (!sender.hasPermission("nick.admin")) { sender.sendMessage(msg("no-permission")); return true; }
            try {
                ((NickPlugin) plugin).reloadEverything();
                sender.sendMessage(msg("reload-done"));
            } catch (Throwable t) {
                sender.sendMessage(color("&cReload failed: " + t.getMessage()));
            }
            return true;
        }

        if (sub.equals("help")) { sendHelp(sender); return true; }

        if (!(sender instanceof Player)) { sender.sendMessage(ChatColor.RED + "Players only."); return true; }
        Player player = (Player) sender;

        if (sub.equals("reset")) {
            if (!player.hasPermission("nick.reset")) { player.sendMessage(msg("no-permission")); return true; }
            if (!manager.isNicked(player)) {
                player.sendMessage(color("&7You don't have a nickname set."));
                return true;
            }
            manager.removeNick(player);
            player.sendMessage(color("&aYour nickname has been reset."));
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
            player.sendMessage(color("&aYour nickname is now: &f" + nick));
            return true;
        }

        if (sub.equals("rank")) {
            if (!player.hasPermission("nick.rank")) { player.sendMessage(msg("no-permission")); return true; }
            if (args.length < 2) { player.sendMessage(color("&bUsage: /nick rank <rank>")); return true; }
            String rank = args[1].toLowerCase();
            if (!plugin.getConfig().contains("ranks." + rank)) {
                player.sendMessage(color("&cUnknown rank: " + rank)); return true;
            }
            String nick = manager.getNick(player);
            if (nick == null) { player.sendMessage(color("&cPick a nick first using /nick")); return true; }
            manager.getStorage().setNickRank(player.getUniqueId(), rank);
            manager.applyNick(player, nick, rank);
            player.sendMessage(color("&aRank set to: " + rank));
            return true;
        }

        String nick = args[0];
        if (!validNick(nick)) { player.sendMessage(msg("nick-invalid")); return true; }
        if (nickTaken(nick))   { player.sendMessage(msg("nick-taken")); return true; }
        String rank = plugin.getConfig().getString("default-nick-rank", "default");
        manager.getStorage().setNickname(player.getUniqueId(), nick);
        manager.getStorage().setNickRank(player.getUniqueId(), rank);
        manager.applyNick(player, nick, rank);
        player.sendMessage(color("&aYour nickname is now: &f" + nick));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(color("&8&m--------- &bNick &8&m---------"));
        sender.sendMessage(color("&b/nick &f- open the book menu"));
        sender.sendMessage(color("&b/nick <name> &f- set custom nick"));
        sender.sendMessage(color("&b/nick random &f- random nick"));
        sender.sendMessage(color("&b/nick reset &f- remove your nick"));
        sender.sendMessage(color("&b/nick rank <rank> &f- change display rank"));
        sender.sendMessage(color("&b/nick list &f- list nicked players"));
        sender.sendMessage(color("&8&m----------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String p = args[0].toLowerCase();
            for (String s : SUB) if (s.startsWith(p)) out.add(s);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("rank")) {
            if (plugin.getConfig().getConfigurationSection("ranks") != null) {
                for (String r : plugin.getConfig().getConfigurationSection("ranks").getKeys(false)) {
                    if (r.startsWith(args[1].toLowerCase())) out.add(r);
                }
            }
        }
        return out;
    }
}

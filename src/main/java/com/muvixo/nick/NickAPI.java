package com.muvixo.nick;

import org.bukkit.entity.Player;

public final class NickAPI {
    private static NickManager manager;
    private NickAPI() {}
    public static void init(NickManager m) { manager = m; }
    public static String getNick(Player player) { return manager != null && player != null ? manager.getNick(player) : null; }
    public static String getDisplayName(Player player) {
        if (player == null) return "unknown";
        String n = getNick(player);
        return (n == null || n.isEmpty()) ? player.getName() : n;
    }
    public static boolean isNicked(Player player) { return manager != null && player != null && manager.isNicked(player); }
    public static String getNickRank(Player player) { return manager != null && player != null ? manager.getRank(player) : null; }
}

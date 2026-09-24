package com.muvixo.nick;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Public API so other plugins can read nicknames.
 * Author: Muvixo
 */
public final class NickAPI {

    private static NickManager manager;

    private NickAPI() {}

    public static void init(NickManager m) { manager = m; }

    public static String getNick(Player player) {
        if (manager == null || player == null) return null;
        return manager.getNick(player);
    }

    public static String getNick(UUID uuid) {
        if (manager == null || uuid == null) return null;
        return manager.getNickByUuid(uuid);
    }

    public static String getDisplayName(Player player) {
        if (player == null) return "unknown";
        String n = getNick(player);
        return (n == null || n.isEmpty()) ? player.getName() : n;
    }

    public static boolean isNicked(Player player) {
        if (manager == null || player == null) return false;
        return manager.isNicked(player);
    }

    public static boolean isNicked(UUID uuid) {
        if (manager == null || uuid == null) return false;
        return manager.isNickedByUuid(uuid);
    }

    public static String getNickRank(Player player) {
        if (manager == null || player == null) return null;
        return manager.getRank(player);
    }
}

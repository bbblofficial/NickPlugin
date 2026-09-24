package com.muvixo.nick;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.logging.Logger;

public final class NMS {

    private static Logger LOGGER = Logger.getLogger("Minecraft");

    // true only if every reflective hook below resolved successfully
    private static boolean available = false;

    private static String VERSION;
    private static Class<?> CRAFT_PLAYER;
    private static Class<?> ENTITY_PLAYER;
    private static Class<?> PACKET;
    private static Class<?> PACKET_PLAYER_INFO;
    private static Class<?> ENUM_INFO_ACTION;
    private static Class<?> GAME_PROFILE;
    private static Class<?> PACKET_CUSTOM_PAYLOAD;
    private static Class<?> PACKET_DATA_SERIALIZER;
    private static Class<?> UNPOOLED;

    private static Method CRAFT_PLAYER_GET_HANDLE;
    private static Method ENTITY_PLAYER_GET_PROFILE;
    private static Field ENTITY_PLAYER_PLAYER_CONNECTION;

    private static Constructor<?> PACKET_INFO_CTOR;
    private static Constructor<?> PACKET_CUSTOM_PAYLOAD_CTOR;
    private static Constructor<?> PACKET_DATA_SERIALIZER_CTOR;
    private static Method UNPOOLED_BUFFER;

    static {
        try {
            String pkg = Bukkit.getServer().getClass().getPackage().getName();
            VERSION = pkg.substring(pkg.lastIndexOf('.') + 1);

            CRAFT_PLAYER = Class.forName("org.bukkit.craftbukkit." + VERSION + ".entity.CraftPlayer");
            ENTITY_PLAYER = Class.forName("net.minecraft.server." + VERSION + ".EntityPlayer");
            PACKET = Class.forName("net.minecraft.server." + VERSION + ".Packet");
            PACKET_PLAYER_INFO = Class.forName("net.minecraft.server." + VERSION + ".PacketPlayOutPlayerInfo");
            ENUM_INFO_ACTION = Class.forName("net.minecraft.server." + VERSION + ".PacketPlayOutPlayerInfo$EnumPlayerInfoAction");
            GAME_PROFILE = Class.forName("com.mojang.authlib.GameProfile");
            PACKET_CUSTOM_PAYLOAD = Class.forName("net.minecraft.server." + VERSION + ".PacketPlayOutCustomPayload");
            PACKET_DATA_SERIALIZER = Class.forName("net.minecraft.server." + VERSION + ".PacketDataSerializer");
            UNPOOLED = Class.forName("io.netty.buffer.Unpooled");

            CRAFT_PLAYER_GET_HANDLE = CRAFT_PLAYER.getMethod("getHandle");
            ENTITY_PLAYER_GET_PROFILE = ENTITY_PLAYER.getMethod("getProfile");
            ENTITY_PLAYER_PLAYER_CONNECTION = ENTITY_PLAYER.getField("playerConnection");

            PACKET_INFO_CTOR = PACKET_PLAYER_INFO.getConstructor(ENUM_INFO_ACTION, Iterable.class);
            PACKET_CUSTOM_PAYLOAD_CTOR = PACKET_CUSTOM_PAYLOAD.getConstructor(String.class, PACKET_DATA_SERIALIZER);
            PACKET_DATA_SERIALIZER_CTOR = PACKET_DATA_SERIALIZER.getConstructor(Class.forName("io.netty.buffer.ByteBuf"));
            UNPOOLED_BUFFER = UNPOOLED.getMethod("buffer", int.class);

            available = true;
        } catch (Throwable t) {
            available = false;
            LOGGER.warning("[NickPlugin] Could not hook into server internals for version '" + VERSION
                    + "'. Tab list and nametag spoofing will be disabled (nicknames will still work for "
                    + "chat and commands). This usually means the server is running a build/fork whose "
                    + "internal class names don't match this plugin's expectations. Cause: " + t);
        }
    }

    private NMS() {}

    /** Lets the plugin route NMS log messages through its own logger instead of the raw "Minecraft" one. */
    public static void init(Logger logger) {
        if (logger != null) LOGGER = logger;
    }

    /** True if the reflective server hooks initialized correctly on this server build. */
    public static boolean isAvailable() {
        return available;
    }

    private static Object handle(Player p) {
        try { return CRAFT_PLAYER_GET_HANDLE.invoke(p); }
        catch (Throwable t) { return null; }
    }

    private static void send(Player p, Object packet) {
        try {
            Object h = handle(p);
            if (h == null) return;
            Object conn = ENTITY_PLAYER_PLAYER_CONNECTION.get(h);
            for (Method m : conn.getClass().getMethods()) {
                if (m.getName().equals("sendPacket")) {
                    m.invoke(conn, packet);
                    break;
                }
            }
        } catch (Throwable ignored) {}
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void updateName(Player player, String newName) {
        if (!available) return;
        try {
            Object h = handle(player);
            if (h == null) return;

            // 1. Change the GameProfile name field
            Object profile = ENTITY_PLAYER_GET_PROFILE.invoke(h);
            Field nameField = GAME_PROFILE.getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(profile, newName);

            // 2. Create REMOVE and ADD packets
            Object actionRemove = Enum.valueOf((Class<Enum>) ENUM_INFO_ACTION, "REMOVE_PLAYER");
            Object actionAdd = Enum.valueOf((Class<Enum>) ENUM_INFO_ACTION, "ADD_PLAYER");

            Object removePacket = PACKET_INFO_CTOR.newInstance(actionRemove, Collections.singletonList(h));
            Object addPacket = PACKET_INFO_CTOR.newInstance(actionAdd, Collections.singletonList(h));

            // 3. Send to all players to refresh TAB list
            for (Player p : Bukkit.getOnlinePlayers()) {
                send(p, removePacket);
                send(p, addPacket);
            }
        } catch (Throwable t) {
            LOGGER.warning("[NickPlugin] Failed to update tab/nametag identity for "
                    + player.getName() + ": " + t);
        }
    }

    public static void openBook(Player player, ItemStack book) {
        if (!available) {
            LOGGER.warning("[NickPlugin] Cannot open the nick book menu for " + player.getName()
                    + " - NMS hooks are unavailable on this server build.");
            return;
        }
        try {
            int slot = player.getInventory().getHeldItemSlot();
            ItemStack old = player.getInventory().getItem(slot);
            player.getInventory().setItem(slot, book);

            Object buf = UNPOOLED_BUFFER.invoke(null, 0);
            Object serializer = PACKET_DATA_SERIALIZER_CTOR.newInstance(buf);
            Object packet = PACKET_CUSTOM_PAYLOAD_CTOR.newInstance("MC|BOpen", serializer);
            send(player, packet);

            player.getInventory().setItem(slot, old);
        } catch (Throwable t) {
            LOGGER.warning("[NickPlugin] Failed to open the nick book menu for " + player.getName() + ": " + t);
        }
    }
}

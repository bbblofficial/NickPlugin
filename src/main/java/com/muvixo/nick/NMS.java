package com.muvixo.nick;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * NMS helper for 1.8.8.
 * Author: Muvixo
 */
public final class NMS {

    public static final String VERSION;
    public static final Class<?> CRAFT_PLAYER;
    public static final Class<?> ENTITY_PLAYER;
    public static final Class<?> PACKET;
    public static final Class<?> PACKET_PLAYER_INFO;
    public static final Class<?> PACKET_TEAM;
    public static final Class<?> ENUM_PLAYER_INFO_ACTION;
    public static final Class<?> GAME_PROFILE;
    public static final Class<?> CHAT_COMPONENT_TEXT;
    public static final Class<?> PACKET_CHAT;
    public static final Class<?> I_CHAT_BASE;

    public static final Method CRAFT_PLAYER_GET_HANDLE;
    public static final Method ENTITY_PLAYER_GET_PROFILE;
    public static final Method PLAYER_CONNECTION_SEND;
    public static final Field  ENTITY_PLAYER_PLAYER_CONNECTION;
    public static final Field  ENTITY_PLAYER_PING;

    public static final Constructor<?> GAME_PROFILE_CTOR;
    public static final Constructor<?> PACKET_CHAT_CTOR;

    static {
        String version = "UNKNOWN";
        Class<?> craftPlayer = null;
        Class<?> entityPlayer = null;
        Class<?> packet = null;
        Class<?> packetInfo = null;
        Class<?> packetTeam = null;
        Class<?> enumAction = null;
        Class<?> gameProfile = null;
        Class<?> chatText = null;
        Class<?> packetChat = null;
        Class<?> iChat = null;
        Method getHandle = null;
        Method getProfile = null;
        Method send = null;
        Field pc = null;
        Field ping = null;
        Constructor<?> gpCtor = null;
        Constructor<?> chatCtor = null;

        try {
            String pkg = Bukkit.getServer().getClass().getPackage().getName();
            version = pkg.substring(pkg.lastIndexOf('.') + 1);

            craftPlayer  = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            entityPlayer = Class.forName("net.minecraft.server." + version + ".EntityPlayer");
            packet       = Class.forName("net.minecraft.server." + version + ".Packet");
            packetInfo   = Class.forName("net.minecraft.server." + version + ".PacketPlayOutPlayerInfo");
            packetTeam   = Class.forName("net.minecraft.server." + version + ".PacketPlayOutScoreboardTeam");
            enumAction   = Class.forName("net.minecraft.server." + version + ".PacketPlayOutPlayerInfo$EnumPlayerInfoAction");
            gameProfile  = Class.forName("com.mojang.authlib.GameProfile");
            chatText     = Class.forName("net.minecraft.server." + version + ".ChatComponentText");
            packetChat   = Class.forName("net.minecraft.server." + version + ".PacketPlayOutChat");
            iChat        = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");

            getHandle = craftPlayer.getMethod("getHandle");
            getProfile = entityPlayer.getMethod("getProfile");
            pc = entityPlayer.getField("playerConnection");
            ping = entityPlayer.getField("ping");

            Class<?> playerConn = Class.forName("net.minecraft.server." + version + ".PlayerConnection");
            send = playerConn.getMethod("sendPacket", packet);

            gpCtor = gameProfile.getConstructor(UUID.class, String.class);
            chatCtor = packetChat.getConstructor(iChat, byte.class);
        } catch (Throwable t) {
            t.printStackTrace();
        }

        VERSION = version;
        CRAFT_PLAYER = craftPlayer;
        ENTITY_PLAYER = entityPlayer;
        PACKET = packet;
        PACKET_PLAYER_INFO = packetInfo;
        PACKET_TEAM = packetTeam;
        ENUM_PLAYER_INFO_ACTION = enumAction;
        GAME_PROFILE = gameProfile;
        CHAT_COMPONENT_TEXT = chatText;
        PACKET_CHAT = packetChat;
        I_CHAT_BASE = iChat;

        CRAFT_PLAYER_GET_HANDLE = getHandle;
        ENTITY_PLAYER_GET_PROFILE = getProfile;
        PLAYER_CONNECTION_SEND = send;
        ENTITY_PLAYER_PLAYER_CONNECTION = pc;
        ENTITY_PLAYER_PING = ping;

        GAME_PROFILE_CTOR = gpCtor;
        PACKET_CHAT_CTOR = chatCtor;
    }

    private NMS() {}

    public static Object getHandle(Player p) {
        try { return CRAFT_PLAYER_GET_HANDLE.invoke(p); }
        catch (Throwable t) { return null; }
    }

    public static void sendPacket(Player p, Object packet) {
        try {
            Object handle = getHandle(p);
            if (handle == null) return;
            Object conn = ENTITY_PLAYER_PLAYER_CONNECTION.get(handle);
            PLAYER_CONNECTION_SEND.invoke(conn, packet);
        } catch (Throwable ignored) {}
    }

    public static void sendActionBar(Player p, String text) {
        try {
            Object chat = CHAT_COMPONENT_TEXT.getConstructor(String.class).newInstance(text);
            Object packet = PACKET_CHAT_CTOR.newInstance(chat, (byte) 2);
            sendPacket(p, packet);
        } catch (Throwable ignored) {}
    }

    public static int getPing(Player p) {
        try {
            Object handle = getHandle(p);
            return ENTITY_PLAYER_PING.getInt(handle);
        } catch (Throwable t) { return 0; }
    }
}

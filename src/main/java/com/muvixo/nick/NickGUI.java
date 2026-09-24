package com.muvixo.nick;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NickGUI {

    private final NickPlugin plugin;
    private File menuFile;
    private FileConfiguration menu;

    public NickGUI(NickPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        menuFile = new File(plugin.getDataFolder(), "menu.yml");
        if (!menuFile.exists()) {
            try {
                plugin.saveResource("menu.yml", false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("menu.yml is missing from the plugin jar; using built-in defaults.");
            }
        }
        menu = YamlConfiguration.loadConfiguration(menuFile);
    }

    public void reload() {
        load();
    }

    private String colorize(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public void openConfirmBook(Player player) {
        if (!NMS.isAvailable()) {
            player.sendMessage(colorize("&cThe nick menu is unavailable on this server version. Use &f/nick random &cor &f/nick <name>&c instead."));
            return;
        }

        try {
            ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
            BookMeta meta = (BookMeta) book.getItemMeta();
            if (meta == null) {
                plugin.getLogger().warning("Could not create book meta for the nick menu.");
                return;
            }

            meta.setTitle(colorize(menu.getString("title", "&b&lNick Setup")));
            meta.setAuthor(menu.getString("author", "Muvixo"));

            // --- Page 1: info text + confirm/cancel buttons ---
            TextComponent page1 = new TextComponent(colorize(menu.getString("page-1", "&7Nick setup")));
            page1.addExtra(new TextComponent("\n\n"));
            page1.addExtra(buildButton("buttons.ok", "/nick ok"));
            page1.addExtra(new TextComponent("\n\n"));
            page1.addExtra(buildButton("buttons.cancel", "/nick no"));

            // --- Page 2: manual command reference ---
            TextComponent page2 = new TextComponent(colorize(menu.getString("page-2", "")));

            // BookMeta.Spigot#addPage(BaseComponent...) is NOT declared on the
            // compile-time ItemMeta.Spigot type shipped in the 1.8.8 spigot-api
            // (confirmed - trying to call it directly fails to even compile).
            // Every real CraftBukkit/Spigot build DOES implement it on the
            // concrete runtime object though, so it has to be reached via
            // reflection. Unlike the old code, failures here are now logged
            // with their real cause instead of being swallowed.
            addPage(meta, new BaseComponent[]{page1});
            addPage(meta, new BaseComponent[]{page2});

            book.setItemMeta(meta);
            NMS.openBook(player, book);
        } catch (Throwable t) {
            Throwable cause = (t instanceof InvocationTargetException && t.getCause() != null) ? t.getCause() : t;
            plugin.getLogger().warning("Failed to open the nick menu for " + player.getName() + ": " + cause);
            cause.printStackTrace();
            player.sendMessage(colorize("&cCould not open the nick menu. Use &f/nick <name> &cor &f/nick random&c instead."));
        }
    }

    /**
     * Adds a page to a book via reflection, since BookMeta.Spigot#addPage
     * isn't part of the compile-time API on Spigot 1.8.8.
     */
    private void addPage(BookMeta meta, BaseComponent[] page) throws Exception {
        Object spigot = meta.spigot();
        Method addPageMethod = spigot.getClass().getMethod("addPage", BaseComponent[].class);
        addPageMethod.invoke(spigot, (Object) page);
    }

    private TextComponent buildButton(String path, String command) {
        String text = menu.getString(path + ".text", "[ Click ]");
        String color = menu.getString(path + ".color", "&f");
        boolean bold = menu.getBoolean(path + ".bold", false);
        String hover = menu.getString(path + ".hover", "");

        TextComponent comp = new TextComponent(colorize(color + (bold ? "&l" : "") + text));
        comp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        if (hover != null && !hover.isEmpty()) {
            comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder(hover).create()));
        }
        comp.setUnderlined(true);
        return comp;
    }
}

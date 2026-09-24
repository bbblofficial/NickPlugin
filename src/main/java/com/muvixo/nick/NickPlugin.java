package com.muvixo.nick;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * NickPlugin - Hypixel-style nickname system for Spigot 1.8.8.
 * Author: Muvixo
 * Theme : Aqua & White
 */
public class NickPlugin extends JavaPlugin {

    private NickStorage storage;
    private NickManager manager;
    private NickCommand command;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.storage = new NickStorage(this);
        this.manager = new NickManager(this, storage);
        this.command = new NickCommand(this, manager);

        NickAPI.init(manager);

        getCommand("nick").setExecutor(command);
        getCommand("nick").setTabCompleter(command);

        Bukkit.getPluginManager().registerEvents(manager, this);

        manager.restoreAllOnline();

        long autosave = getConfig().getLong("storage.autosave-seconds", 300L);
        if (autosave < 30L) autosave = 30L;
        Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override public void run() { storage.save(); }
        }, autosave * 20L, autosave * 20L);

        getLogger().info("===============================================");
        getLogger().info("  NickPlugin v1.0.0");
        getLogger().info("  Author: Muvixo");
        getLogger().info("  Theme : Aqua & White");
        getLogger().info("  Nicknames loaded: " + manager.getNamePoolSize());
        getLogger().info("  NMS version: " + NMS.VERSION);
        getLogger().info("===============================================");
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.saveAll();
        if (storage != null) storage.save();
        getLogger().info("NickPlugin disabled.");
    }

    public void reloadEverything() {
        reloadConfig();
        manager.reload();
        manager.restoreAllOnline();
    }

    public NickManager getManager() { return manager; }
    public NickStorage getStorage() { return storage; }
}

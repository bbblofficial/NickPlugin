package com.muvixo.nick;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class NickPlugin extends JavaPlugin {

    private NickStorage storage;
    private NickManager manager;
    private NickCommand command;
    private NickGUI gui;
    private NickConfirm confirm;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Route NMS's warnings through this plugin's own logger so problems
        // show up clearly in console instead of being silently swallowed.
        NMS.init(getLogger());
        if (!NMS.isAvailable()) {
            getLogger().warning("Running without tab/nametag spoofing support - see the warning above for details.");
        }

        this.storage = new NickStorage(this);
        this.manager = new NickManager(this, storage);
        this.confirm = new NickConfirm(this, manager);
        this.command = new NickCommand(this, manager, confirm);
        this.gui = new NickGUI(this);

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

        getLogger().info("NickPlugin enabled.");
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.saveAll();
        if (storage != null) storage.save();
    }

    public void reloadEverything() {
        reloadConfig();
        manager.reload();
        gui.reload();
        manager.restoreAllOnline();
    }

    public NickManager getManager() { return manager; }
    public NickStorage getStorage() { return storage; }
    public NickGUI getGui() { return gui; }
}

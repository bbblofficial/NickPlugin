package com.muvixo.nick;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class NickStorage {

    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration config;

    public NickStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        String fileName = plugin.getConfig().getString("storage.file", "nicks.yml");
        if (fileName == null || fileName.isEmpty()) fileName = "nicks.yml";
        this.file = new File(plugin.getDataFolder(), fileName);
        load();
    }

    private void load() {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        if (!file.exists()) {
            try { file.createNewFile(); }
            catch (IOException e) {
                plugin.getLogger().warning("Could not create " + file.getName() + ": " + e.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try { config.save(file); }
        catch (IOException e) {
            plugin.getLogger().warning("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }

    public String getNickname(UUID uuid) { return config.getString(uuid.toString() + ".nick"); }
    public void setNickname(UUID uuid, String nickname) { config.set(uuid.toString() + ".nick", nickname); save(); }
    public void clearNickname(UUID uuid) { config.set(uuid.toString() + ".nick", null); save(); }

    public String getNickRank(UUID uuid) { return config.getString(uuid.toString() + ".rank"); }
    public void setNickRank(UUID uuid, String rank) { config.set(uuid.toString() + ".rank", rank); save(); }
    public void clearNickRank(UUID uuid) { config.set(uuid.toString() + ".rank", null); save(); }
}

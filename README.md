# NickPlugin

**Hypixel-style Nick plugin for Spigot 1.8.8**
Author: **Muvixo**
Theme : **Aqua & White**

## Features

- `/nick <name>`       - set a specific nickname
- `/nick random`       - pick a random name from `names.txt`
- `/nick reset`        - remove nickname
- `/nick rank <rank>`  - set display rank while nicked
- `/nick list`         - list nicked players
- `/nick reload`       - reload config + names.txt
- Nickname **persists** until `/nick reset`
- Survives logout / login / restart / respawn
- Rank hidden -> shows `default`
- Public API `NickAPI` for other plugins

## Build

GitHub Actions builds automatically on push.
Or locally: `mvn clean package`

Output: `target/NickPlugin-1.0.0.jar`

## Install

1. Drop JAR into `plugins/`
2. Restart server
3. Edit `plugins/NickPlugin/config.yml`
4. Replace `plugins/NickPlugin/names.txt`

## Credits

- **Muvixo** - Plugin author

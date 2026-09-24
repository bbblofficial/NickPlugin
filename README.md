# NickPlugin

**Hypixel-style Nick plugin for Spigot 1.8.8**
Author: **Muvixo**
Theme : **Aqua & White**

## Features

- `/nick`             - Opens the Hypixel-style Book GUI
- `/nick <name>`       - set a specific nickname
- `/nick random`       - pick a random name from `names.txt`
- `/nick reset`        - remove nickname (fully restores tab + nametag)
- `/nick rank <rank>`  - set the display prefix while nicked
- `/nick list`         - list nicked players
- `/nick reload`       - reload config + names.txt + menu.yml
- Nickname **persists** until `/nick reset`
- Survives logout / login / restart / respawn
- Public API `NickAPI` for other plugins

## About "ranks"

A rank is **not** a permission rank. It is a cosmetic tag that gets
chained directly in front of your nickname everywhere it's shown
(tab list, chat, and the nametag above your head), e.g.:

```
rank: vip+   +   nick: Steve   =>   "[VIP+] Steve"
```

Edit the `prefix` field for a rank in `config.yml` to change what
gets glued in front of the nickname:

```yaml
ranks:
  vip+:
    prefix: '&a[VIP&6+&a] &a'
```

## This version's fixes (v1.0.2)

- **Book menu crash ("Error loading menu")**: the GUI previously built
  the book's pages with brittle reflection that could fail depending on
  the server build. It now calls Spigot's own `BookMeta.Spigot#addPage`
  API directly, and falls back to a chat message (instead of a broken
  book) if server internals aren't available at all.
- **`menu.yml` is now actually used.** It shipped with the plugin but
  was never read - the book's text/buttons were hardcoded in Java. The
  GUI now loads its title, page text, and both the OK/Cancel buttons
  from `menu.yml`, and `/nick reload` picks up changes to it.
- **Rank prefix wasn't reliably "chained" to the name.** The nametag
  above a nicked player's head and the tab-list entry can each be
  matched by the client against a different identity string depending
  on server/plugin state. The scoreboard team used for the rank prefix
  now registers *both* the real username and the spoofed nickname as
  entries on the same team, so the prefix shows correctly in both
  places (and in chat) regardless of which one gets matched.
- **`/nick reset` now fully clears everything**: the scoreboard team,
  the spoofed tab/nametag identity, the Bukkit display/list name, and
  the saved nickname + rank in storage - so a relog can't bring the old
  nick back.
- **No more silent failures.** If the server build isn't compatible
  with this plugin's low-level hooks, it now logs a clear one-time
  warning explaining what's disabled instead of swallowing exceptions.
- **Build fix (v1.0.2):** `BookMeta.Spigot#addPage` isn't part of the
  compile-time 1.8.8 API (only the real runtime implementation has it),
  so calling it directly failed CI. Restored a proper reflective call
  for that one method only, now with the real underlying exception
  logged (including a full stack trace) if it ever fails again.

## Build

GitHub Actions builds automatically on push.
Or locally: `mvn clean package`

Output: `target/NickPlugin-1.0.2.jar`

## Install

1. Drop JAR into `plugins/`
2. Restart server
3. Edit `plugins/NickPlugin/config.yml` and `menu.yml` to taste
4. Replace `plugins/NickPlugin/names.txt` if you want your own name pool

## Credits

- **Muvixo** - Plugin author

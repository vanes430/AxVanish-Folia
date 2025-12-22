# AxVanish

A powerful and flexible vanish plugin designed for Folia and Paper.

## Commands

| Command | Aliases | Description | Permission |
|---------|---------|-------------|------------|
| `/vanish` | `/v`, `/axvanish` | Toggle your own vanish state. | `axvanish.vanish` |
| `/vanish toggle <player>` | | Toggle vanish for another player. | `axvanish.command.toggle.other` |
| `/vanish admin reload` | | Reload the plugin configuration. | `axvanish.command.admin.reload` |
| `/vanish admin version` | | Display the plugin version. | `axvanish.command.admin.version` |

## Permissions

- `axvanish.vanish`: Basic permission to vanish.
- `axvanish.command.toggle.other`: Permission to toggle vanish for others.
- `axvanish.command.admin.reload`: Permission to reload the config.
- `axvanish.command.admin.version`: Permission to see version info.
- `axvanish.notify`: Permission to receive vanish/unvanish notifications.
- `axvanish.group.<groupname>`: Assigns a player to a specific vanish group (e.g., `axvanish.group.default`).

## Features & Configuration

### Groups (`groups.yml`)
Groups allow you to define different levels of vanish with specific capabilities.
- **Priority**: Higher priority groups can see vanished players in lower priority groups.
- **Capabilities**:
    - `silent_open`: Open chests without animation/sound.
    - `invincible`: Prevent taking damage while vanished.
    - `flight`: Enable flight while vanished.
    - `chat`: Send messages while vanished (use prefix like `!` to bypass).
    - `prevent`: Block certain actions (breaking blocks, picking up items, etc.).
    - `action_bar`: Show a message on the action bar while vanished.
    - `potion_effects`: Give specific effects (like Night Vision) while vanished.

### Database (`config.yml`)
Supports H2, SQLite, and MySQL for storing vanish states across restarts.

### Fake Join/Leave (`config.yml`)
You can enable/disable and customize fake messages that are sent when a player vanishes or unvanishes:
- `fake-join`: Sent when a player unvanishes (appearing as if they just joined).
- `fake-leave`: Sent when a player vanishes (appearing as if they just left).
- Supports MiniMessage color codes (e.g., `<yellow>`, `<red>`, `<gradient:red:blue>`).

## Placeholders
If PlaceholderAPI is installed, you can use the following placeholders (identifier `axvanish`):
- `%axvanish_online%`: Real online player count (total online - vanished).
- `%axvanish_vanished%`: Total number of vanished players.
- `%axvanish_vanished_players%`: A comma-separated list of vanished player names.
- `%axvanish_state%`: Returns `true` if the player is vanished, `false` otherwise.

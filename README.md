# Chameleon

A multi-platform mod disabler for Minecraft 1.21.1 (NeoForge / Fabric / Quilt).

## Overview

Chameleon runs at the earliest stage of game startup (pre-launch / mod discovery) and disables incompatible mods before they are loaded. Disabled JARs are moved to `mods/disabled/` to prevent crashes caused by environment mismatches.

## Features

### Core Features
- Multi-platform support with a shared `common` module
- Environment detection: Windows, Linux, macOS, and Android launchers (PojavLauncher, FCL, Zalith, Boat/MCinaBox, HMCL-PE, etc.)
- Client vs Server side detection
- Safe file handling: moves to `mods/disabled/` with automatic `.bak` deduplication
- Zero external dependencies: toml4j shaded into the JAR

### Advanced Features (v2.0+)
- **Regex Rules**: Support for regular expression matching with `r:` prefix (e.g., `r:.*beta.*`)
- **Version Constraints**: Semantic version matching (e.g., `sodium (<0.6.0)`, `iris (<=1.7.0)`)
- **Logging System**: Independent log file `logs/chameleon.log` with configurable levels (INFO/WARN/DEBUG)
- **System Language Detection**: Auto-detect system language and output localized logs
- **Rule Match Details**: Detailed logs showing which mods matched each rule
- **Command System**: In-game commands for mod management (client-side only)

## Configuration

Create or edit `config/chameleon_config.json`:

```json
{
  "equipment": {
    "android": ["sodium", "physics-mod-1.0.0.jar"],
    "windows": [],
    "linux": ["some-linux-only-mod"],
    "mac": []
  },
  "environment": {
    "client": ["spark", "inventory-profiles", "fancymenu-2.0.1.jar"],
    "server": ["lithium", "chunky"]
  },
  "log_level": "INFO",
  "enable_commands": true,
  "version_constraints": [
    "sodium (<0.6.0)",
    "iris (<=1.7.0)",
    "physics-mod (>=2.0.0)"
  ],
  "rules": [
    "r:.*beta.*",
    "r:.*-dev\\.jar"
  ]
}
```

### Configuration Options

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `equipment` | Object | `{}` | Platform-specific disable rules |
| `environment` | Object | `{}` | Client/Server-specific disable rules |
| `log_level` | String | `"INFO"` | Log level: `INFO`, `WARN`, `DEBUG` |
| `enable_commands` | Boolean | `true` | Enable in-game commands |
| `version_constraints` | Array | `[]` | Version-based disable rules |
| `rules` | Array | `[]` | Advanced rules (supports `r:` regex prefix) |

### Rule Types

1. **ModID Match**: `"sodium"` - disables mod with ID `sodium`
2. **Filename Match**: `"physics-mod-1.0.0.jar"` - exact filename match
3. **Version Constraint**: `"sodium (<0.6.0)"` - disables sodium versions below 0.6.0
4. **Regex Match**: `"r:.*beta.*"` - matches any mod with "beta" in filename or ModID

### Version Constraint Operators

| Operator | Example | Description |
|----------|---------|-------------|
| `<` | `(<0.6.0)` | Less than |
| `<=` | `(<=1.7.0)` | Less than or equal |
| `>` | `(>2.0.0)` | Greater than |
| `>=` | `(>=2.0.0)` | Greater than or equal |
| `=` | `(=1.5.0)` | Exact version match |

## Commands

In-game commands (client-side only, requires `enable_commands: true`):

| Command | Function | Output |
|---------|----------|--------|
| `/chameleon list` | Scan `mods/` directory and list all mods | Terminal summary + `config/chameleon_mod_list.md` |
| `/chameleon mod` | List disabled mods in `mods/disabled/` | Terminal output |
| `/chameleon undo` | Restore all disabled mods to `mods/` | Requires `/chameleon undo confirm` |

**Note**: 
- Commands are client-side only. On servers, only `list` and `mod` are available.
- `undo` requires double confirmation to prevent accidental restoration.

## Supported Platforms

- NeoForge 21.1.x
- Fabric Loader >= 0.16.0
- Quilt Loader >= 0.26.0 (compatible with Fabric builds)

## Log Files

- **Location**: `logs/chameleon.log`
- **Content**: Startup logs, rule match details, operation summary
- **Debug Mode**: Set `log_level: "DEBUG"` for detailed mod extraction info

## License

MIT License

# Chameleon

A multi-platform mod disabler for Minecraft 1.21.1 (NeoForge / Fabric / Quilt).

## Overview

Chameleon runs at the earliest stage of game startup (pre-launch / mod discovery) and disables incompatible mods before they are loaded. Disabled JARs are moved to `mods/disabled/` to prevent crashes caused by environment mismatches.

## Features

- Multi-platform support with a shared `common` module
- Environment detection: Windows, Linux, macOS, and Android launchers (PojavLauncher, FCL, Zalith, Boat/MCinaBox, HMCL-PE, etc.)
- Client vs Server side detection
- Flexible configuration: match by ModID or exact filename (`.jar` suffix auto-detected)
- Safe file handling: moves to `mods/disabled/` with automatic `.bak` deduplication
- Zero external dependencies: toml4j shaded into the JAR

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
  }
}
```

- Entries without `.jar` suffix are matched by ModID
- Entries with `.jar` suffix are matched by exact filename

## Supported Platforms

- NeoForge 21.1.x
- Fabric Loader >= 0.16.0
- Quilt Loader >= 0.26.0

## License

MIT License

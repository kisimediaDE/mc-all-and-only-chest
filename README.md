# All and Only Chests

An independent Paper plugin implementing the "All and Only Chests" challenge
for Minecraft Java 26.x.

The project is inspired by the challenge popularized by BastiGHG and by the
publicly available
[Skippaddin/All-and-Only-Chests](https://github.com/Skippaddin/All-and-Only-Chests)
plugin. The implementation in the main source tree is being rewritten from
scratch.

## Requirements

- Java 25
- Paper 26.2

Compatibility with Paper 26.1 will be verified during development. The plugin
declares Paper API version 26.1 so that one artifact can support both versions
as long as no 26.2-only API is required.

## Build

macOS/Linux:

```bash
./gradlew build
```

Windows:

```bat
gradlew.bat build
```

The resulting plugin JAR is written to `build/libs/`.

## Local test server

The disposable Paper development server lives in the ignored `run/` directory.
Build and install the current plugin with:

```bash
./gradlew deployToTestServer
```

Start it on macOS/Linux with:

```bash
./scripts/start-test-server.sh
```

On Windows, use:

```bat
scripts\start-test-server.bat
```

The first Paper launch creates `run/eula.txt` and stops. Read the linked
Minecraft EULA before changing `eula=false` to `eula=true`.

## Persistence

Challenge data is stored in
`plugins/AllAndOnlyChests/data/challenge.db` inside the server directory.
Player-placed block positions are indexed in memory while the server runs and
written to SQLite immediately. The bundled SQLite driver supports macOS,
Windows, and Linux.

## Current smoke test

The current milestone provides:

- a working `/gui` placeholder command;
- no item drops from naturally generated blocks;
- normal drops from blocks placed by a player;
- persistent placed-block tracking across server restarts;
- no block drops from entity or block explosions;
- no mob drops except blaze rods and ender pearls;
- persistent tracking when pistons or gravity move player-placed blocks.

Additional non-player block transformations are covered as their challenge
rules are implemented.

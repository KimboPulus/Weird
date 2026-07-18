# Weird

Java terrarium game about keeping plants, rabbits, predators, people, moisture,
and temperature inside a moving balance. Player changes local conditions and
adds species while six levels introduce weather, failure rules, and limited
upgrades.

[Download latest Windows or Linux build](https://github.com/KimboPulus/Weird/releases/latest)

![Window capture](docs/screenshots/window-check.png)

## Current game

- six explicit ecosystem-balance levels;
- rain, drought, compost, species, lightning, and sanctuary tools;
- changing weather and population pressure;
- crisis warnings with recoverable failure windows;
- session shop, persistent score/tokens, veteran animals;
- replay log recording accepted and rejected board commands;
- bundled Windows ZIP and Linux `.deb`/`.tar.gz` releases.

Full controls and level behavior live in [player guide](docs/player-guide.md).
Release changes remain in [changelog](CHANGELOG.md).

## Build

- Java 17
- Java Swing
- JavaFX 21.0.11 for embedded completion video

Portable releases include required Java runtime. Local development requires a
JavaFX 21.0.11 SDK on compiler and runtime module paths. Exact Windows and Linux
compile/package commands live in `.github/workflows/release.yml`.

## Verification

Checks cover simulation invariants, training progression, settings, packaging,
known regression cases, offscreen rendering, and one real Swing-window capture.
Every pull request compiles on Ubuntu with JavaFX and runs five headless smoke
or regression checks. Release workflow repeats checks while building Windows and
Ubuntu packages; downloadable assets remain separate from CI test jobs.

## Screenshots

![Animal pack capture](docs/screenshots/animal-pack-check.png)

![Level-up capture](docs/screenshots/level-up-check.png)

Six level captures and more visual evidence are in [screenshot gallery](docs/index.md).

## Code map

- `sim` - ecosystem model, weather, births, deaths, population history;
- `training` - level catalog, target bands, warnings, score;
- `game` - typed commands, replay recorder, bounded event log;
- `ui` - Swing/JavaFX rendering and input translation;
- `audio`, `settings`, `progression`, `support` - focused services.

Detailed design: [architecture](docs/architecture.md).

## Scope

This is a game, not a clinical focus tool or ecological model. Supporting
studies in `docs/research-backed-production.md` informed interface decisions;
they do not validate gameplay as treatment. Balance values are designed for
playability, not biological accuracy.

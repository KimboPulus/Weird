# Weird Architecture

Weird is a small desktop game, but it is structured like a product: simulation rules are testable without UI, player input is recorded as replayable commands, packaged builds are checked, and visual regressions are captured.

## Layers

`com.kimbopulus.weird.sim`
Core terrarium model. Owns grid state, organisms, weather, area effects, deaths, births, population snapshots, and deterministic rule execution for a given seed.

`com.kimbopulus.weird.training`
Objective and progression logic. Owns level targets, warning timers, failure reasons, score, and the data catalog used to audit level definitions.

`com.kimbopulus.weird.game`
Product-level runtime services. Owns replay commands and the bounded event log. This package is deliberately UI-light so bug reports can be replayed in tests.

`com.kimbopulus.weird.ui`
Swing and JavaFX presentation. It draws the board, opens dialogs, plays the completion video, and translates player clicks into game commands.

`com.kimbopulus.weird.audio`, `settings`, `progression`, `support`
Small service packages for sound, settings persistence, shop score, and error logging.

## Replay Flow

Each accepted or rejected board click becomes a `GameCommand`.

```
mouse click -> ToolMode -> GameCommandType -> Simulation mutation -> replay log
```

The latest run is written to `data/replays/latest-run.wrpl`. Rejected commands are kept because they explain failed player actions such as lightning without a target.

Replay tests load this file format, apply accepted commands to a fresh `Simulation`, and assert the resulting board state. This makes old bugs easier to freeze into regression tests.

## Level Data

Runtime levels still use `TrainingLevel` for fast, type-safe access. A mirrored resource file, `training-levels.properties`, exists so target copy and level order can be audited as data. `TrainingLevelCatalog` loads it, and tests fail if the resource drifts from runtime levels.

This is the first step toward fully data-driven levels without taking a risky rewrite in one pass.

## Test Gates

Core checks:

- `SimulationSmokeCheck`
- `ModelRegressionCheck`
- `TrainingSessionSmokeCheck`
- `SettingsSmokeCheck`
- `PackagingSmokeCheck`

Visual checks:

- `VisualSmokeCheck` renders levels, warnings, deaths, popups, and victory overlays.
- `WindowVisualCheck` opens the real Swing frame, clicks Shop, buys an item, opens audio settings, and verifies completion video motion.

Release checks:

- GitHub Actions builds Windows and Ubuntu packages.
- The packaged jar must contain key dialogs, assets, and media before upload.
- CI fails if Shop is missing from the release jar.

## Bug Policy

Every repeated player-reported bug should become one of:

- model regression test,
- training/session regression test,
- visual screenshot check,
- packaged-app smoke check,
- replay fixture.

Current covered past bugs include Shop packaging, warning priority, pause countdown freeze, transparent death effects, completion video motion, climate combo math, and creatures walking over plants.

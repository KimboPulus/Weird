# Weird

A small Java terrarium simulation. Plants grow from soil conditions, rabbits graze on plants, humans plant new growth, and wolves and bears shape the balance. The player acts as a gardener by changing the environment or adding species to the board.

The current version includes illustrated terrain and wildlife, six balance-maintenance levels, changing weather events, a persistent score, a session-only upgrade shop, veteran animals, death animations, and explicit ecosystem stability goals.

## Current build

- Java 17
- Java Swing
- JavaFX 21.0.11 for embedded victory-video playback

The local JavaFX SDK and jmods are stored under `D:\java\dependencies`. The portable release includes the required runtime, so players do not install JavaFX separately.

## Run in IntelliJ

1. Open this repository folder in IntelliJ IDEA.
2. Use the `Weird` run configuration.
3. Run the project.

## Run from PowerShell

```powershell
.\scripts\run.ps1
```

The build output goes into `out/`, which is ignored by Git.

## Build a release

```powershell
.\scripts\release.ps1
```

This creates `release\Weird-portable.zip` with the full runnable Windows app bundle.
Inside that bundle, `Weird\Run Weird.bat` starts the packaged game.

## Check

```powershell
.\scripts\check.ps1
```

This runs a small simulation smoke check without installing a test framework.

For an offscreen render check:

```powershell
.\scripts\visual-check.ps1
```

It writes `out/visual-check.png`.

To briefly open and capture the real Swing window:

```powershell
.\scripts\window-check.ps1
```

It writes `out/window-check.png`.

## Screenshots

![Window capture](docs/screenshots/window-check.png)

![Animal pack capture](docs/screenshots/animal-pack-check.png)

![Level-up capture](docs/screenshots/level-up-check.png)

The same gallery is published in `docs/index.md` for the GitHub Pages view.

## Controls

- `Rain` cools a 4 x 4 patch hard while adding a smaller amount of moisture.
- `Drought` dries and warms a 4 x 4 patch centered on the click, and kills a creature if you click directly on it.
- `Compost` raises fertility on the clicked cell.
- `Rabbit` and `Wolf` place one organism on an empty clicked cell.
- `Human` plants nearby soil, can breed once when two humans meet, and walks over plants without destroying them.
- `Bear` hunts humans and also walks over plants without destroying them.
- `Lightning` spends 10 tokens and strikes one exact creature with blue lightning.
- `Sanctuary` is purchased in the shop and protects one 2 x 2 soil patch per run.
- `Pause` stops the timer.
- `Step` advances one tick.
- `Speed` changes the simulation pace.
- `Restart` starts a fresh terrarium and training session.
- `Audio` opens persistent music and effect-volume settings.
- `How to play` pauses the terrarium and reopens the short guide.

Keyboard controls:

- `1` to `0` select gardener tools.
- `Space` pauses or resumes.
- `N` advances one tick.
- `R` restarts the run.

Move the pointer over the board to inspect moisture, fertility, temperature, and the current occupant.

Wet soil shows cool highlights, dry soil develops small cracks, and sanctuary cells have a gold border. Rabbits, wolves, humans, plants, bears, and veteran animals each have a distinct board silhouette.

## Focus training

- Six levels give the run a clear objective. Each level asks you to keep plants, rabbits, wolves, humans, bears, moisture, and temperature inside an explicit target band, then press `Next Level`.
- Completing Level 6 ends the run, shows the victory screen, and plays the bundled video inside the game. `Restart Game` begins again at Level 1.
- The right panel shows the exact current values and target ranges for the active level under the `OBJECTIVE` heading.
- Level changes appear briefly over the board with a stronger celebration animation.
- Ecosystem crises add a bold labeled border and an always-visible warning strip.
- Older rabbits and wolves become veterans with a silver marker. Hovering reveals their age and energy.
- Deaths fade out over about 2.8 seconds, and lightning and human deaths use harsher sound cues.
- Run score resets with the terrarium. Total score and tokens persist in `data/progress.properties`.
- Shop purchases reset when a level is lost, when the full run restarts, and when the game is opened again.

## Risk and failure

Rain, Drought, and Compost have strong local effects. Repeated use can flood the terrarium, create lethal dry soil, or trigger uncontrolled plant growth.

A crisis warning appears when a tracked band leaves its safe range. Plant warnings allow 60 seconds to recover; other warnings allow 30 seconds. `Restart Level` rebuilds the terrarium on the same level and deducts 15 run-score points. Restarting the whole run shows a confirmation warning.

Ambient music and sound effects are generated through built-in Java Sound. Audio failure never prevents the simulation from running.

Design decisions and their supporting studies are recorded in `docs/research-backed-production.md`. These sources guide the interface but do not make the game a clinically validated treatment.

## Shop

Level rewards and strong balance streaks grant tokens without reducing total score. The shop contains session-only upgrades:

- `Sanctuary Permit` unlocks the protected soil tool.
- `Rain Barrel` increases Rain strength by 50%.
- `Rich Compost` increases Compost strength by 50%.

Shop ownership does not persist between launches.

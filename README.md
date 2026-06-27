# Weird

A small Java terrarium simulation. Plants grow from soil conditions, rabbits graze on plants, and wolves hunt rabbits. The player acts as a gardener by changing the environment or adding species to the board.

The current version includes illustrated terrain and wildlife, six objective-based levels, adaptive recall prompts, changing weather events, a persistent score and upgrade shop, veteran animals, and ecosystem stability goals.

## Current build

- Java 17
- Java Swing
- No external dependencies

## Run in IntelliJ

1. Open this repository folder in IntelliJ IDEA.
2. Use the `Weird` run configuration.
3. Run the project.

## Run from PowerShell

```powershell
.\scripts\run.ps1
```

The build output goes into `out/`, which is ignored by Git.

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

## Controls

- `Rain` adds moisture around the clicked cell.
- `Drought` dries the clicked area.
- `Compost` raises fertility in a small area.
- `Trim` clears nearby plants and opens movement paths.
- `Plant`, `Rabbit`, and `Wolf` place one organism on an empty clicked cell.
- `Sanctuary` is purchased in the shop and protects one 2 x 2 soil patch per run.
- `Pause` stops the timer.
- `Step` advances one tick.
- `Speed` changes the simulation pace.
- `Restart` starts a fresh terrarium and training session.

Keyboard controls:

- `1` to `8` select gardener tools.
- `Space` pauses or resumes.
- `N` advances one tick.
- `R` restarts the run.

Move the pointer over the board to inspect moisture, fertility, temperature, and the current occupant.

Wet soil shows cool highlights, dry soil develops small cracks, and sanctuary cells have a gold border. Rabbits, wolves, plants, and veteran animals each have a distinct board silhouette.

## Focus training

- Six levels give the run a clear objective. Fill the progress bar, collect the reward, then press `Next Level`.
- Recall questions ask whether a selected population was rising, stable, or falling.
- Recall lookback grows from 10 to 20 and then 32 ticks as the answer streak improves.
- Later levels can reverse the recall rule and ask for the opposite trend.
- Level changes appear briefly over the board, and ecosystem crises add a labeled border.
- Older rabbits and wolves become veterans with a silver marker. Hovering reveals their age and energy.
- Run score resets with the terrarium. Total score and tokens persist in `data/progress.properties`.

## Shop

Level rewards and correct recall answers grant tokens without reducing total score. The shop contains permanent upgrades:

- `Sanctuary Permit` unlocks the protected soil tool.
- `Rain Barrel` increases Rain strength by 50%.
- `Rich Compost` increases Compost strength by 50%.

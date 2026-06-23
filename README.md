# Weird

A small Java terrarium simulation. Plants grow from soil conditions, rabbits graze on plants, and wolves hunt rabbits. The player acts as a gardener by changing the environment or adding species to the board.

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

## Controls

- `Rain` adds moisture around the clicked cell.
- `Drought` dries the clicked area.
- `Plant`, `Rabbit`, and `Wolf` place one organism on an empty clicked cell.
- `Pause` stops the timer.
- `Step` advances one tick.

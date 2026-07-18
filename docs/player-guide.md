# Player guide

## Goal

Keep population and climate values inside active level's target bands, then use
`Next Level`. Completing level 6 ends run and plays bundled completion video.
Losing level returns current level with score penalty; restarting game returns
to level 1.

## Tools

- `Rain` cools and moistens a 4 x 4 area.
- `Drought` dries and warms a 4 x 4 area; direct hit kills one creature.
- `Compost` raises fertility across a 4 x 4 area.
- `Rabbit`, `Wolf`, `Human`, and `Bear` add one organism to empty cell.
- `Lightning` spends 10 tokens and strikes one exact creature.
- `Sanctuary` protects one 2 x 2 soil patch after shop purchase.
- `Pause`, `Step`, `Speed`, and `Restart` control simulation.
- `Audio` changes persistent music/effect levels.

## Keyboard

- `1` through `0`: select gardener tools;
- `Space`: pause/resume;
- `N`: advance one tick;
- `R`: restart run.

Hover board to inspect soil and occupant. Wet soil has cool highlights, dry soil
develops cracks, sanctuary cells use gold border, veteran animals use silver
marker.

## Warnings and failure

Plant-band warning allows 60 seconds to recover. Other tracked bands allow 30
seconds. `Restart Level` deducts 15 run-score points. Repeated climate tools can
flood soil, create lethal drought, or cause uncontrolled plant growth.

## Score and shop

Run score resets with terrarium. Total score and tokens persist in
`data/progress.properties`. Level rewards and balance streaks grant tokens.
Shop purchases last current session only:

- `Sanctuary Permit` unlocks protected soil;
- `Rain Barrel` increases Rain strength by 50%;
- `Rich Compost` increases Compost strength by 50%.

## Replay diagnostics

Every board click is recorded in `data/replays/latest-run.wrpl`. Accepted and
rejected actions remain visible, so bug report can include last replay without
guessing input sequence. Review replay before sharing because it records play
actions and local timing.

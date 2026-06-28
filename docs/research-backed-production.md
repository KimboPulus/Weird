# Research-Backed Production Pass

This document records the evidence used for the June 2026 production pass. Each implementation decision is tied to published research. The evidence informs design; it does not make Weird a clinically validated treatment.

| Decision | Implementation | Research basis | Limits |
| --- | --- | --- | --- |
| Match challenge to demonstrated skill | Recall span, cadence, and response window adapt to the answer streak. | Corcos (2018) experimentally links enjoyable challenge and positive game experience. The adaptive-scaffolding randomized trial by Dankbaar et al. (2024) reports effects on performance, self-regulation, cognitive load, and engagement. | Results come from different game genres and learning contexts. |
| Keep goals and feedback unambiguous | One level objective, progress bar, immediate score feedback, and explicit complete/failed states. | Harris et al. (2019) used clear goals and feedback while studying flow during skill acquisition. | A clear interface supports flow conditions but does not guarantee flow. |
| Teach through contextual practice | Early levels show one short hint that changes after tool use or recall practice. | Cao and Liu (2022) report that implicit guidance, practice, timely feedback, and just-in-time tutorial access can improve tutorial perception and enjoyment. | Their pilot was conducted in a different game. |
| Preserve meaningful failure and retry | Sustained crises lose the level; restart retains the objective and encourages a different strategy. | Lyons (2015) discusses failure and retrial as important to challenge and satisfaction when competence is not excessively undermined. | This is a design synthesis, not a direct test of Weird's thresholds. |
| Provide immediate action sounds | Tools, warnings, completion, failure, and restart use distinct synchronized cues. | Opitz et al. (2011) found superior learning with immediate rather than delayed feedback. Luciani et al. (2022) found auditory-motor feedback contributed to aspects of learning, especially for novices. | These studies are not direct comparisons of game sound effects. |
| Make music respond to game state | Ambient note duration, register, and intensity respond to calm, warning, and failure states. | Plut and Pasquier (2019) empirically found adaptive music could significantly affect reported player affect. Dorsey et al. (2023) demonstrate gameplay-state-driven emotionally directed soundtrack control. | The current procedural score is intentionally simple and has not had a listener study. |
| Give players audio control | Audio can be disabled; music and effects have separate persistent volume sliders. | Bavelier et al. (2020) report varied real-world listening environments and concurrent audio use among players, supporting user control rather than one fixed mix. | Listening-environment evidence does not specify ideal slider defaults. |
| Use both visual and auditory crisis feedback | Crises use a border, text label, side-panel warning, and sound cue. | Immediate-feedback findings from Opitz et al. (2011) support prompt informative feedback; van der Kuil et al. (2018) emphasize responsive, usable interaction in a cognitive rehabilitation game. | Redundant cues should still be user-tested for distraction. |
| Persist settings and expose failures | Progress and audio settings save locally; uncaught errors append to `data/error.log` and show a plain-language dialog. | Fluet et al. (2019) include device reliability, game reliability, adaptation, scoring, instructions, and graphics among evaluated serious-game quality criteria. Martins et al. (2021) show UX methods can reveal contrasting issues and stress that bugs harm experience. | Reliability features require continued testing on more machines. |
| Verify multiple UI states | Automated captures cover normal play, completion, failure, shop, and audio settings, alongside simulation checks. | Martins et al. (2021) argue that UX quality should not rely on a single evaluation method or moment. | Automated screenshots do not replace testing with target players. |

## Papers

- Corcos, A. (2018). [Being enjoyably challenged is the key to an enjoyable gaming experience](https://pmc.ncbi.nlm.nih.gov/articles/PMC5954478/).
- [Effects of adaptive scaffolding on performance, cognitive load and engagement in game-based learning](https://pmc.ncbi.nlm.nih.gov/articles/PMC11360721/) (2024).
- [Flow Experiences During Visuomotor Skill Acquisition](https://pmc.ncbi.nlm.nih.gov/articles/PMC6530424/) (2019).
- Cao, S., and Liu, F. (2022). [Learning to play: understanding in-game tutorials](https://pmc.ncbi.nlm.nih.gov/articles/PMC9676530/).
- Lyons, E. J. (2015). [Cultivating Engagement and Enjoyment in Exergames](https://pmc.ncbi.nlm.nih.gov/articles/PMC4580142/).
- Opitz et al. (2011). [Timing Matters: Immediate and Delayed Feedback](https://pmc.ncbi.nlm.nih.gov/articles/PMC3034228/).
- Luciani et al. (2022). [The role of auditory feedback in motor learning of music](https://pmc.ncbi.nlm.nih.gov/articles/PMC9671877/).
- Plut, C., and Pasquier, P. (2019). [Music Matters: adaptive music and player affect](https://doi.org/10.1109/CIG.2019.8847951).
- Dorsey et al. (2023). [Learning Adaptive Game Soundtrack Control](https://doi.org/10.1609/aaai.v37i13.26909).
- Bavelier et al. (2020). [Auditory cognition and perception of action video game players](https://pmc.ncbi.nlm.nih.gov/articles/PMC7462999/).
- van der Kuil et al. (2018). [Usability of a serious game in cognitive rehabilitation](https://pmc.ncbi.nlm.nih.gov/articles/PMC5996119/).
- [Usability evaluation of rehabilitation games](https://pmc.ncbi.nlm.nih.gov/articles/PMC6710625/) (2019).
- [Are UX Evaluation Methods Providing the Same Big Picture?](https://pmc.ncbi.nlm.nih.gov/articles/PMC8156257/) (2021).

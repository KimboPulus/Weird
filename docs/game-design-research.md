# Game Design Research

This implementation pass focused on changes that fit Weird's calm ecosystem format.

## Applied principles

- Keep painting work small and predictable. Swing painting stays on the event dispatch thread, timers coalesce updates, and hover changes repaint only affected cells.
- Keep the current objective and progress visible. Each level has one short goal and a progress bar.
- Give immediate, redundant feedback. Level changes use a board banner; ecosystem crises use text, color, and a labeled border.
- Support keyboard and mouse input. Number keys select tools, while pause, step, and restart have direct keys.
- Adapt challenge to demonstrated performance. Recall span and cadence increase with streak, while early prompts allow more response time.
- Train task switching and inhibition. Later levels can ask for the opposite population trend instead of the observed trend.

## Sources

- Oracle, [Painting in AWT and Swing](https://www.oracle.com/java/technologies/painting.html)
- Oracle, [Performing Custom Painting](https://docs.oracle.com/javase/tutorial/uiswing/painting/index.html)
- Oracle, [Troubleshooting Java 2D](https://docs.oracle.com/en/java/javase/24/troubleshoot/java-2d.html)
- Microsoft, [Xbox Accessibility Guideline 108: Input](https://learn.microsoft.com/en-us/gaming/accessibility/xbox-accessibility-guidelines/108)
- Microsoft, [Xbox Accessibility Guideline 118: Time Limits](https://learn.microsoft.com/en-us/gaming/accessibility/xbox-accessibility-guidelines/118)
- PMC, [Adaptive working memory training](https://pmc.ncbi.nlm.nih.gov/articles/PMC3059829/)
- PMC, [Working memory training and task switching](https://pmc.ncbi.nlm.nih.gov/articles/PMC7614331/)

These sources support implementation choices, but the game is not presented as medical treatment.

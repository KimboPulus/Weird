package com.kimbopulus.weird.training;

import java.util.List;

public record TrainingPrompt(
        String question,
        List<String> choices,
        int answerIndex,
        int createdAtTick,
        int lookback,
        FocusRule rule
) {
    public TrainingPrompt {
        choices = List.copyOf(choices);
        if (choices.size() != 3) {
            throw new IllegalArgumentException("A training prompt needs exactly three choices.");
        }
        if (answerIndex < 0 || answerIndex >= choices.size()) {
            throw new IllegalArgumentException("Answer index is outside the available choices.");
        }
    }

    public String answerLabel() {
        return choices.get(answerIndex);
    }
}

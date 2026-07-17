package com.kimbopulus.weird.training;

import java.util.List;

public record TrainingLevelSpec(
        int number,
        String title,
        String objective,
        String challenge,
        int target,
        List<BalanceBand> objectiveBands
) {
    public TrainingLevelSpec {
        if (number < 1) {
            throw new IllegalArgumentException("number must be positive");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        if (objective == null || objective.isBlank()) {
            throw new IllegalArgumentException("objective is required");
        }
        if (challenge == null || challenge.isBlank()) {
            throw new IllegalArgumentException("challenge is required");
        }
        if (target < 1) {
            throw new IllegalArgumentException("target must be positive");
        }
        objectiveBands = List.copyOf(objectiveBands);
    }
}

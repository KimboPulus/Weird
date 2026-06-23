package com.kimbopulus.weird.training;

import com.kimbopulus.weird.sim.OrganismKind;

public record TrainingPrompt(String question, OrganismKind answer, int createdAtTick) {
}


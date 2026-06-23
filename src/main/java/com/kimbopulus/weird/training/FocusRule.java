package com.kimbopulus.weird.training;

public enum FocusRule {
    NORMAL("Pick the real trend"),
    OPPOSITE("Pick the opposite trend");

    private final String instruction;

    FocusRule(String instruction) {
        this.instruction = instruction;
    }

    public String instruction() {
        return instruction;
    }
}

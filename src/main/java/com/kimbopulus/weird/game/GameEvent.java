package com.kimbopulus.weird.game;

public record GameEvent(long sequence, int tick, GameEventType type, String message) {
    public GameEvent {
        if (sequence < 1L) {
            throw new IllegalArgumentException("sequence must be positive");
        }
        if (tick < 0) {
            throw new IllegalArgumentException("tick must be non-negative");
        }
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message is required");
        }
    }
}

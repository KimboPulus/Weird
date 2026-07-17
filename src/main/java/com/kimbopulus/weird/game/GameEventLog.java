package com.kimbopulus.weird.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class GameEventLog {
    private static final int MAX_EVENTS = 80;

    private final ArrayDeque<GameEvent> events = new ArrayDeque<>();
    private long nextSequence = 1L;

    public GameEvent add(int tick, GameEventType type, String message) {
        GameEvent event = new GameEvent(nextSequence++, tick, type, message);
        events.addLast(event);
        while (events.size() > MAX_EVENTS) {
            events.removeFirst();
        }
        return event;
    }

    public GameEvent latest() {
        return events.peekLast();
    }

    public List<GameEvent> recent(int limit) {
        int safeLimit = Math.max(0, limit);
        List<GameEvent> copy = new ArrayList<>(events);
        int from = Math.max(0, copy.size() - safeLimit);
        return List.copyOf(copy.subList(from, copy.size()));
    }

    public void clear() {
        events.clear();
        nextSequence = 1L;
    }
}

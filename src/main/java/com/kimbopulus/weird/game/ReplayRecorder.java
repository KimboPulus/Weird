package com.kimbopulus.weird.game;

import com.kimbopulus.weird.sim.Position;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class ReplayRecorder {
    private final Path path;
    private final ReplayLog log = new ReplayLog();
    private long nextSequence = 1L;

    private ReplayRecorder(Path path) {
        this.path = path;
    }

    public static ReplayRecorder defaultRecorder() {
        return new ReplayRecorder(Path.of("data", "replays", "latest-run.wrpl"));
    }

    public static ReplayRecorder inMemory() {
        return new ReplayRecorder(null);
    }

    public void startNewRun() {
        log.clear();
        nextSequence = 1L;
        flush();
    }

    public GameCommand record(int tick, GameCommandType type, Position position, boolean accepted) {
        GameCommand command = new GameCommand(nextSequence++, tick, type, position, accepted);
        log.add(command);
        flush();
        return command;
    }

    public List<GameCommand> commands() {
        return log.commands();
    }

    private void flush() {
        if (path == null) {
            return;
        }
        try {
            log.save(path);
        } catch (IOException ignored) {
            // Replay logging is diagnostic. It must never interrupt play.
        }
    }
}

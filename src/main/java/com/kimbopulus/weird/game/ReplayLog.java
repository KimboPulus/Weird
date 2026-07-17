package com.kimbopulus.weird.game;

import com.kimbopulus.weird.sim.Simulation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ReplayLog {
    private static final String HEADER = "# Weird replay v1";

    private final List<GameCommand> commands = new ArrayList<>();

    public List<GameCommand> commands() {
        return List.copyOf(commands);
    }

    public void add(GameCommand command) {
        commands.add(command);
    }

    void clear() {
        commands.clear();
    }

    public int applyTo(Simulation simulation) {
        int applied = 0;
        for (GameCommand command : commands) {
            if (command.accepted() && command.type().applyTo(simulation, command.position())) {
                applied++;
            }
        }
        return applied;
    }

    public void save(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        List<String> lines = new ArrayList<>(commands.size() + 1);
        lines.add(HEADER);
        for (GameCommand command : commands) {
            lines.add(command.encode());
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    public static ReplayLog load(Path path) throws IOException {
        ReplayLog log = new ReplayLog();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            log.add(GameCommand.decode(line));
        }
        return log;
    }
}

package com.kimbopulus.weird.game;

import com.kimbopulus.weird.sim.Position;

public record GameCommand(
        long sequence,
        int tick,
        GameCommandType type,
        Position position,
        boolean accepted
) {
    public GameCommand {
        if (sequence < 1L) {
            throw new IllegalArgumentException("sequence must be positive");
        }
        if (tick < 0) {
            throw new IllegalArgumentException("tick must be non-negative");
        }
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        if (position == null) {
            throw new IllegalArgumentException("position is required");
        }
    }

    public String encode() {
        return sequence + "|" + tick + "|" + type.name() + "|"
                + position.x() + "|" + position.y() + "|" + accepted;
    }

    public static GameCommand decode(String line) {
        String[] parts = line.split("\\|");
        if (parts.length != 6) {
            throw new IllegalArgumentException("Invalid replay command: " + line);
        }
        return new GameCommand(
                Long.parseLong(parts[0]),
                Integer.parseInt(parts[1]),
                GameCommandType.valueOf(parts[2]),
                new Position(Integer.parseInt(parts[3]), Integer.parseInt(parts[4])),
                Boolean.parseBoolean(parts[5])
        );
    }
}

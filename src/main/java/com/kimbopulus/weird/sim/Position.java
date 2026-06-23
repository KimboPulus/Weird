package com.kimbopulus.weird.sim;

public record Position(int x, int y) {
    public Position move(Direction direction) {
        return new Position(x + direction.dx(), y + direction.dy());
    }
}


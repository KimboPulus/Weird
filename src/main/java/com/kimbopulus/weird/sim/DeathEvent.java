package com.kimbopulus.weird.sim;

public record DeathEvent(long id, OrganismKind kind, Position position, long createdAtMillis) {
}

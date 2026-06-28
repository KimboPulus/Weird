package com.kimbopulus.weird.sim;

public record DeathEvent(long id, OrganismKind kind, DeathCause cause, Position position, long createdAtMillis) {
}

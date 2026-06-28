package com.kimbopulus.weird.sim;

public record BirthEvent(long id, OrganismKind kind, Position position, long createdAtMillis) {
}

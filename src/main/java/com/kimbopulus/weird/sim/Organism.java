package com.kimbopulus.weird.sim;

public abstract class Organism {
    private int age;
    private int energy;
    private boolean alive = true;

    protected Organism(int energy) {
        this.energy = energy;
    }

    public final void tick(Simulation simulation, Position position) {
        age++;
        update(simulation, position);
        if (energy <= 0) {
            die();
        }
    }

    protected abstract void update(Simulation simulation, Position position);

    public abstract OrganismKind kind();

    public int age() {
        return age;
    }

    public int energy() {
        return energy;
    }

    public boolean alive() {
        return alive;
    }

    protected void gainEnergy(int amount) {
        energy += amount;
    }

    protected void spendEnergy(int amount) {
        energy -= amount;
    }

    protected void setEnergy(int energy) {
        this.energy = energy;
    }

    protected void die() {
        alive = false;
    }
}


package com.kimbopulus.weird.ui;

import com.kimbopulus.weird.sim.Position;
import com.kimbopulus.weird.sim.Simulation;

public enum ToolMode {
    RAIN("Rain") {
        @Override
        public void apply(Simulation simulation, Position position) {
            simulation.rain(position);
        }
    },
    DROUGHT("Drought") {
        @Override
        public void apply(Simulation simulation, Position position) {
            simulation.drought(position);
        }
    },
    PLANT("Plant") {
        @Override
        public void apply(Simulation simulation, Position position) {
            simulation.addPlant(position);
        }
    },
    RABBIT("Rabbit") {
        @Override
        public void apply(Simulation simulation, Position position) {
            simulation.addRabbit(position);
        }
    },
    WOLF("Wolf") {
        @Override
        public void apply(Simulation simulation, Position position) {
            simulation.addWolf(position);
        }
    };

    private final String label;

    ToolMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public abstract void apply(Simulation simulation, Position position);
}


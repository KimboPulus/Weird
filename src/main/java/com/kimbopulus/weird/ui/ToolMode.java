package com.kimbopulus.weird.ui;

import com.kimbopulus.weird.sim.Position;
import com.kimbopulus.weird.sim.Simulation;

public enum ToolMode {
    RAIN("Rain", "Add moisture in a small area. Useful when plants are fading.") {
        @Override
        public void apply(Simulation simulation, Position position) {
            simulation.rain(position);
        }
    },
    DROUGHT("Drought", "Dry a crowded area. Useful when plants are taking over.") {
        @Override
        public void apply(Simulation simulation, Position position) {
            simulation.drought(position);
        }
    },
    COMPOST("Compost", "Boost fertility around the clicked cell.") {
        @Override
        public void apply(Simulation simulation, Position position) {
            simulation.compost(position);
        }
    },
    CLEAR("Trim", "Clear nearby plants to open paths for animals.") {
        @Override
        public void apply(Simulation simulation, Position position) {
            simulation.clearPatch(position);
        }
    },
    PLANT("Plant", "Place one plant on an empty cell.") {
        @Override
        public void apply(Simulation simulation, Position position) {
            simulation.addPlant(position);
        }
    },
    RABBIT("Rabbit", "Place one grazer on an empty cell.") {
        @Override
        public void apply(Simulation simulation, Position position) {
            simulation.addRabbit(position);
        }
    },
    WOLF("Wolf", "Place one predator on an empty cell.") {
        @Override
        public void apply(Simulation simulation, Position position) {
            simulation.addWolf(position);
        }
    },
    SANCTUARY("Sanctuary", "Protect one 2 x 2 seed patch from seasons and world weather.") {
        @Override
        public void apply(Simulation simulation, Position position) {
            simulation.addSanctuary(position);
        }
    };

    private final String label;
    private final String description;

    ToolMode(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public abstract void apply(Simulation simulation, Position position);
}

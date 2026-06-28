package com.kimbopulus.weird.ui;

import com.kimbopulus.weird.sim.Position;
import com.kimbopulus.weird.sim.Simulation;

public enum ToolMode {
    RAIN("Rain", "Strongly water a 5 x 5 area. Repeated use can flood the world.") {
        @Override
        public void apply(Simulation simulation, Position position) {
            simulation.rain(position);
        }
    },
    DROUGHT("Drought", "Strongly dry a 5 x 5 area. Repeated use can kill the soil.") {
        @Override
        public void apply(Simulation simulation, Position position) {
            simulation.drought(position);
        }
    },
    COMPOST("Compost", "Strongly boost a 3 x 3 patch. Overuse can cause plant surges.") {
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

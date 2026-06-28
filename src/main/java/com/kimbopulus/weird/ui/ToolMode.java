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
    PLANT("Plant", "Place one plant on an empty cell.") {
        @Override
        public void apply(Simulation simulation, Position position) {
            simulation.addPlant(position);
        }
    },
    HUMAN("Human", "Place one human on an empty cell.") {
        @Override
        public void apply(Simulation simulation, Position position) {
            simulation.addHuman(position);
        }
    },
    BEAR("Bear", "Place one bear on an empty cell.") {
        @Override
        public void apply(Simulation simulation, Position position) {
            simulation.addBear(position);
        }
    },
    RABBIT_FEMALE("Rabbit F", "Place one female rabbit on an empty cell.") {
        @Override
        public void apply(Simulation simulation, Position position) {
            simulation.addRabbit(position, com.kimbopulus.weird.sim.RabbitSex.FEMALE);
        }
    },
    RABBIT_MALE("Rabbit M", "Place one male rabbit on an empty cell.") {
        @Override
        public void apply(Simulation simulation, Position position) {
            simulation.addRabbit(position, com.kimbopulus.weird.sim.RabbitSex.MALE);
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

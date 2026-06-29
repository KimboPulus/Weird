package com.kimbopulus.weird.ui;

import com.kimbopulus.weird.sim.Position;
import com.kimbopulus.weird.sim.Simulation;

public enum ToolMode {
    RAIN("Rain", "Water one square and cool it a little. Repeated use can flood the world.", 0) {
        @Override
        public boolean apply(Simulation simulation, Position position) {
            return simulation.rain(position);
        }
    },
    DROUGHT("Drought", "Dry one square and warm it a little. Repeated use can kill the soil.", 0) {
        @Override
        public boolean apply(Simulation simulation, Position position) {
            return simulation.drought(position);
        }
    },
    COMPOST("Compost", "Boost one square. Overuse can cause plant surges.", 0) {
        @Override
        public boolean apply(Simulation simulation, Position position) {
            return simulation.compost(position);
        }
    },
    PLANT("Plant", "Place one plant on an empty cell.", 0) {
        @Override
        public boolean apply(Simulation simulation, Position position) {
            return simulation.addPlant(position);
        }
    },
    HUMAN("Human", "Place one human on an empty cell.", 0) {
        @Override
        public boolean apply(Simulation simulation, Position position) {
            return simulation.addHuman(position);
        }
    },
    BEAR("Bear", "Place one bear on an empty cell.", 0) {
        @Override
        public boolean apply(Simulation simulation, Position position) {
            return simulation.addBear(position);
        }
    },
    RABBIT("Rabbit", "Place one male rabbit on an empty cell.", 0) {
        @Override
        public boolean apply(Simulation simulation, Position position) {
            return simulation.addRabbit(position, com.kimbopulus.weird.sim.RabbitSex.MALE);
        }
    },
    WOLF("Wolf", "Place one predator on an empty cell.", 0) {
        @Override
        public boolean apply(Simulation simulation, Position position) {
            return simulation.addWolf(position);
        }
    },
    LIGHTNING("Lightning", "Strike one exact creature for 50 tokens.", 50) {
        @Override
        public boolean apply(Simulation simulation, Position position) {
            return simulation.lightning(position);
        }
    },
    SANCTUARY("Sanctuary", "Protect one 2 x 2 seed patch from seasons and world weather.", 0) {
        @Override
        public boolean apply(Simulation simulation, Position position) {
            return simulation.addSanctuary(position);
        }
    };

    private final String label;
    private final String description;
    private final int tokenCost;

    ToolMode(String label, String description, int tokenCost) {
        this.label = label;
        this.description = description;
        this.tokenCost = tokenCost;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public int tokenCost() {
        return tokenCost;
    }

    public abstract boolean apply(Simulation simulation, Position position);
}

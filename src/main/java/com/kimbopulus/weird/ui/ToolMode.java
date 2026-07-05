package com.kimbopulus.weird.ui;

import com.kimbopulus.weird.sim.Position;
import com.kimbopulus.weird.sim.Simulation;

public enum ToolMode {
    RAIN("Rain", "Use when heat is high or land is too dry. Best on hot dry 4 x 4 patches.", 0) {
        @Override
        public boolean apply(Simulation simulation, Position position) {
            return simulation.rain(position);
        }
    },
    DROUGHT("Drought", "Use when moisture or plants are too high. Best on wet dense 4 x 4 patches.", 0) {
        @Override
        public boolean apply(Simulation simulation, Position position) {
            return simulation.drought(position);
        }
    },
    COMPOST("Compost", "Use when one weak square needs a fast fertility push.", 0) {
        @Override
        public boolean apply(Simulation simulation, Position position) {
            return simulation.compost(position);
        }
    },
    HUMAN("Human", "Use when humans are below target or you need more planting pressure.", 0) {
        @Override
        public boolean apply(Simulation simulation, Position position) {
            return simulation.addHuman(position);
        }
    },
    BEAR("Bear", "Use when humans are too high. Bears reduce human count.", 0) {
        @Override
        public boolean apply(Simulation simulation, Position position) {
            return simulation.addBear(position);
        }
    },
    RABBIT("Rabbit", "Use when rabbits are low or plants are taking over. This adds one male.", 0) {
        @Override
        public boolean apply(Simulation simulation, Position position) {
            return simulation.addRabbit(position, com.kimbopulus.weird.sim.RabbitSex.MALE);
        }
    },
    WOLF("Wolf", "Use when rabbits are too high or wolves are below target.", 0) {
        @Override
        public boolean apply(Simulation simulation, Position position) {
            return simulation.addWolf(position);
        }
    },
    LIGHTNING("Lightning", "Use when one exact creature must die now. Costs 10 tokens.", 10) {
        @Override
        public boolean apply(Simulation simulation, Position position) {
            return simulation.lightning(position);
        }
    },
    SANCTUARY("Sanctuary", "Use to lock one 2 x 2 patch against weather and season drift.", 0) {
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

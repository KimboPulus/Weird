package com.kimbopulus.weird;

public final class PackagingSmokeCheck {
    private static final String[] REQUIRED_CLASSES = {
            "com.kimbopulus.weird.Main",
            "com.kimbopulus.weird.ui.TerrariumFrame",
            "com.kimbopulus.weird.ui.TrainingPanel",
            "com.kimbopulus.weird.ui.ShopDialog",
            "com.kimbopulus.weird.ui.AudioSettingsDialog",
            "com.kimbopulus.weird.ui.IntroDialog",
            "com.kimbopulus.weird.ui.CompletionVideoDialog",
            "com.kimbopulus.weird.game.GameCommand",
            "com.kimbopulus.weird.game.ReplayLog",
            "com.kimbopulus.weird.game.GameEventLog",
            "com.kimbopulus.weird.audio.AudioEngine",
            "com.kimbopulus.weird.training.TrainingSession",
            "com.kimbopulus.weird.training.TrainingLevelCatalog",
            "com.kimbopulus.weird.sim.Simulation"
    };

    private static final String[] REQUIRED_RESOURCES = {
            "/com/kimbopulus/weird/sprites/rabbit.png",
            "/com/kimbopulus/weird/sprites/wolf.png",
            "/com/kimbopulus/weird/sprites/human.png",
            "/com/kimbopulus/weird/sprites/bear.png",
            "/com/kimbopulus/weird/effects/blood-splatter.png",
            "/com/kimbopulus/weird/audio/human-attack.wav",
            "/com/kimbopulus/weird/audio/bear-attack.wav",
            "/com/kimbopulus/weird/training/training-levels.properties",
            "/com/kimbopulus/weird/media/game-complete.mp4"
    };

    private PackagingSmokeCheck() {
    }

    public static void main(String[] args) throws Exception {
        for (String className : REQUIRED_CLASSES) {
            Class.forName(className);
        }
        for (String resource : REQUIRED_RESOURCES) {
            require(PackagingSmokeCheck.class.getResourceAsStream(resource) != null,
                    "Missing packaged resource: " + resource);
        }
        System.out.println("Packaging check passed.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}

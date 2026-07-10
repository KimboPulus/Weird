package com.kimbopulus.weird;

import com.kimbopulus.weird.settings.GameSettings;

public final class SettingsSmokeCheck {
    private SettingsSmokeCheck() {
    }

    public static void main(String[] args) {
        GameSettings settings = GameSettings.inMemory();
        require(settings.musicVolume() == 12, "Default music volume should start low.");
        settings.setAudioEnabled(false);
        settings.setMusicVolume(140);
        settings.setEffectsVolume(-20);
        settings.setIntroSeen(true);

        require(!settings.audioEnabled(), "Audio toggle should update.");
        require(settings.musicVolume() == 100, "Music volume should be clamped.");
        require(settings.effectsVolume() == 0, "Effect volume should be clamped.");
        require(settings.introSeen(), "Intro flag should update.");
        System.out.println("Settings check passed.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}

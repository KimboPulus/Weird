package com.kimbopulus.weird.audio;

import com.kimbopulus.weird.settings.GameSettings;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class AudioEngine implements AutoCloseable {
    private static final float SAMPLE_RATE = 22_050f;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    private static final double[] MUSIC_NOTES = {220.0, 277.18, 329.63, 415.30, 329.63, 277.18};
    private static final Path MUSIC_SOURCE = Paths.get("data", "music", "DOMÓWKA MIXTAPE CD.1.mp4");
    private static final Path MUSIC_WAV = Paths.get("data", "music", "domowka-theme.wav");

    private volatile boolean enabled = true;
    private volatile boolean running = true;
    private volatile SourceDataLine musicLine;
    private volatile double musicVolume = 0.35;
    private volatile double effectsVolume = 0.70;
    private volatile double tension;

    public AudioEngine() {
        Thread musicThread = new Thread(this::runMusic, "weird-ambient-music");
        musicThread.setDaemon(true);
        musicThread.start();
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled && musicLine != null) {
            musicLine.flush();
        }
    }

    public void applySettings(GameSettings settings) {
        musicVolume = settings.musicVolume() / 100.0;
        effectsVolume = settings.effectsVolume() / 100.0;
        setEnabled(settings.audioEnabled());
        applyMusicGain(musicLine, musicVolume);
    }

    public void setTension(double tension) {
        this.tension = Math.max(0.0, Math.min(1.0, tension));
    }

    public void play(SoundCue cue) {
        if (!enabled || !running) {
            return;
        }
        Thread soundThread = new Thread(() -> playClip(cue), "weird-sound-effect");
        soundThread.setDaemon(true);
        soundThread.start();
    }

    @Override
    public void close() {
        running = false;
        SourceDataLine line = musicLine;
        if (line != null) {
            line.stop();
            line.close();
        }
    }

    private void runMusic() {
        try {
            Path musicTrack = prepareMusicTrack();
            if (musicTrack != null) {
                try {
                    if (playMusicTrack(musicTrack)) {
                        return;
                    }
                } catch (Exception ignoredTrack) {
                    // Fall through to the generated loop.
                }
            }
            playGeneratedMusic();
        } catch (Exception ignored) {
            // Audio is optional; unavailable devices must not stop the game.
        }
    }

    private void playGeneratedMusic() throws Exception {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, FORMAT);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(FORMAT, 4096);
        line.start();
        musicLine = line;
        int noteIndex = 0;
        while (running) {
            if (!enabled) {
                Thread.sleep(80);
                continue;
            }
            double currentTension = tension;
            double frequency = MUSIC_NOTES[noteIndex] * (currentTension > 0.5 ? 0.75 : 1.0);
            double duration = 0.55 - currentTension * 0.20;
            double volume = (0.025 + currentTension * 0.018) * musicVolume;
            byte[] note = tone(frequency, duration, volume, true);
            line.write(note, 0, note.length);
            noteIndex = (noteIndex + 1) % MUSIC_NOTES.length;
        }
    }

    private boolean playMusicTrack(Path track) throws Exception {
        while (running) {
            if (!enabled) {
                Thread.sleep(80);
                continue;
            }

            SourceDataLine line = null;
            try (AudioInputStream stream = AudioSystem.getAudioInputStream(track.toFile())) {
                AudioFormat format = stream.getFormat();
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format, 4096);
                line.start();
                musicLine = line;
                applyMusicGain(line, musicVolume);

                byte[] buffer = new byte[4096];
                int read;
                while (running && enabled && (read = stream.read(buffer, 0, buffer.length)) >= 0) {
                    line.write(buffer, 0, read);
                }
            } finally {
                if (line != null) {
                    try {
                        line.drain();
                    } finally {
                        line.stop();
                        line.close();
                        if (musicLine == line) {
                            musicLine = null;
                        }
                    }
                }
            }
        }
        return true;
    }

    private Path prepareMusicTrack() throws IOException, InterruptedException {
        if (Files.notExists(MUSIC_SOURCE)) {
            return null;
        }

        Files.createDirectories(MUSIC_WAV.getParent());
        if (Files.exists(MUSIC_WAV)) {
            long sourceTime = Files.getLastModifiedTime(MUSIC_SOURCE).toMillis();
            long wavTime = Files.getLastModifiedTime(MUSIC_WAV).toMillis();
            if (wavTime >= sourceTime) {
                return MUSIC_WAV;
            }
        }

        ProcessBuilder builder = new ProcessBuilder(
                "ffmpeg",
                "-y",
                "-hide_banner",
                "-loglevel",
                "error",
                "-i",
                MUSIC_SOURCE.toString(),
                "-vn",
                "-ac",
                "1",
                "-ar",
                Integer.toString((int) SAMPLE_RATE),
                "-sample_fmt",
                "s16",
                MUSIC_WAV.toString()
        );
        builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        builder.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process process = builder.start();
        if (process.waitFor() != 0 || Files.notExists(MUSIC_WAV)) {
            return null;
        }
        return MUSIC_WAV;
    }

    private void playClip(SoundCue cue) {
        try {
            Clip clip = AudioSystem.getClip();
            byte[] data = cue == SoundCue.HUMAN_DEATH
                    ? harshDeathTone(cue.duration(), cue.volume() * effectsVolume)
                    : tone(cue.frequency(), cue.duration(), cue.volume() * effectsVolume, false);
            clip.open(FORMAT, data, 0, data.length);
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                }
            });
            clip.start();
        } catch (Exception ignored) {
            // Sound effects are optional for the same reason as music.
        }
    }

    private void applyMusicGain(SourceDataLine line, double volume) {
        if (line == null || !line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }
        FloatControl control = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
        double clamped = Math.max(0.0, Math.min(1.0, volume));
        float gain = clamped <= 0.0
                ? control.getMinimum()
                : (float) (20.0 * Math.log10(clamped));
        control.setValue(Math.max(control.getMinimum(), Math.min(control.getMaximum(), gain)));
    }

    private byte[] tone(double frequency, double seconds, double volume, boolean soft) {
        int samples = Math.max(1, (int) (SAMPLE_RATE * seconds));
        byte[] data = new byte[samples * 2];
        for (int i = 0; i < samples; i++) {
            double progress = i / (double) samples;
            double envelope = soft
                    ? Math.sin(Math.PI * progress) * 0.8
                    : Math.min(1.0, progress * 18.0) * Math.min(1.0, (1.0 - progress) * 12.0);
            double wave = Math.sin(2.0 * Math.PI * frequency * i / SAMPLE_RATE);
            if (soft) {
                wave += Math.sin(2.0 * Math.PI * frequency * 0.5 * i / SAMPLE_RATE) * 0.25;
            }
            short value = (short) (wave * envelope * volume * Short.MAX_VALUE);
            data[i * 2] = (byte) (value & 0xff);
            data[i * 2 + 1] = (byte) ((value >>> 8) & 0xff);
        }
        return data;
    }

    private byte[] harshDeathTone(double seconds, double volume) {
        int samples = Math.max(1, (int) (SAMPLE_RATE * seconds));
        byte[] data = new byte[samples * 2];
        for (int i = 0; i < samples; i++) {
            double progress = i / (double) samples;
            double envelope = Math.sin(Math.PI * progress) * (1.0 - progress * 0.18);
            double fall = 1.15 - progress * 0.70;
            double wave = Math.sin(2.0 * Math.PI * 220.0 * fall * i / SAMPLE_RATE)
                    + Math.sin(2.0 * Math.PI * 337.0 * fall * i / SAMPLE_RATE) * 0.85
                    + Math.sin(2.0 * Math.PI * 610.0 * i / SAMPLE_RATE) * 0.35;
            double noise = (((i * 1103515245L + 12345) >>> 16) & 0x7fff) / 16384.0 - 1.0;
            wave += noise * 0.6;
            short value = (short) (wave / 2.7 * envelope * volume * Short.MAX_VALUE);
            data[i * 2] = (byte) (value & 0xff);
            data[i * 2 + 1] = (byte) ((value >>> 8) & 0xff);
        }
        return data;
    }
}

package com.djmusic.vlc.service;

import com.djmusic.vlc.base.ChannelManager;
import com.social100.todero.processor.EventDefinition;
import uk.co.caprica.vlcj.media.Meta;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.State;
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent;

import java.io.File;

public class VlcService {
    public final static String MAIN_GROUP = "Main";
    public final static String CHANNELS_GROUP = "Channels";

    private final ChannelManager channelManager;

    private static final String[] mediaOptions = {
            ":audio-filter=normvol",
            ":norm-buff-size=20",  // Buffer size for normalization
            ":norm-max-level=1.0"  // Maximum level for normalized audio
    };

    public VlcService() {
        System.setProperty("jna.library.path", "C:\\Program Files\\VideoLAN\\VLC");
        channelManager = new ChannelManager();
    }

    public enum VlcPluginEvents implements EventDefinition {
        VOLUME_CHANGE("A change in the volume" ),
        CHANNEL_END("a channel stop playing" ),
        CHANNEL_START("a channel start playing" );

        private final String description;

        VlcPluginEvents(String description) {
            this.description = description;
        }

        @Override
        public String getDescription() {
            return description;
        }
    }

    public String moveCommand(String moveTo) {
        AudioPlayerComponent audioPlayer = channelManager.getCurrentChannel();
        try {
            long moveToTime = parseTime(moveTo);  // Parse the time string
            audioPlayer.mediaPlayer().controls().setTime(moveToTime);
            return "Playback moved to " + moveTo + ".";
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "Failed to move playback due to an unexpected error.";
        }
    }

    public String muteCommand() {
        AudioPlayerComponent audioPlayer = channelManager.getCurrentChannel();

        // Ensure media is present and valid
        if (!audioPlayer.mediaPlayer().media().isValid()) {
            return "No valid media loaded. Mute operation is not available.";
        }

        // Check the current mute state to predict the toggle outcome
        boolean wasMute = audioPlayer.mediaPlayer().audio().isMute();

        // Toggle the mute state
        audioPlayer.mediaPlayer().audio().mute();

        // Feedback based on the expected outcome, not the immediate check
        return wasMute ? "Playback has been unmuted." : "Playback has been muted.";
    }

    public String pauseCommand() {
        AudioPlayerComponent audioPlayer = channelManager.getCurrentChannel();
        State state = audioPlayer.mediaPlayer().status().state();

        // If the player is currently playing, it will be paused
        if (state == State.PLAYING) {
            audioPlayer.mediaPlayer().controls().pause();
            return "Playback paused.";
        }
        // If the player is already paused, it might be intended to resume playback
        else if (state == State.PAUSED) {
            audioPlayer.mediaPlayer().controls().play();
            return "Playback resumed.";
        } else {
            return "Playback is not active. Current state: " + state;
        }
    }

    public String playCommand(String mediaPathToPlay) {
        AudioPlayerComponent audioPlayer = channelManager.getCurrentChannel();
        MediaPlayer mediaPlayer = audioPlayer.mediaPlayer();

        String currentMediaPath = mediaPlayer.media().info() != null ? mediaPlayer.media().info().mrl() : null;

        if (!mediaPathToPlay.isEmpty()) {
            File file = new File(mediaPathToPlay);

            if (!file.exists()) {
                return "File not found: " + mediaPathToPlay;
            }

            if (!mediaPathToPlay.equals(currentMediaPath)) {
                mediaPlayer.media().play(mediaPathToPlay, mediaOptions);
                return "Playing new media: \"" + mediaPathToPlay + "\"";
            } else {
                mediaPlayer.controls().play();
                return "Resuming current media.";
            }
        } else if (mediaPlayer.media().isValid()) {
            mediaPlayer.controls().play();
            return "Resuming playback of current media.";
        } else {
            return "No media file specified and no current media to play.";
        }
    }

    public String skipCommand(long skipTime) { // Positive for forward, negative for backward
        AudioPlayerComponent audioPlayer = channelManager.getCurrentChannel();
        long mediaLength = audioPlayer.mediaPlayer().status().length();

        try {
            long currentTime = audioPlayer.mediaPlayer().status().time();
            long newTime = currentTime + skipTime * 1000;

            // Ensure new time is within media bounds
            newTime = Math.max(newTime, 0);  // Prevent going before the start
            newTime = Math.min(newTime, mediaLength);  // Prevent going beyond the end

            audioPlayer.mediaPlayer().controls().setTime(newTime);
            return String.format("Skipped to %d seconds (%s).", newTime / 1000, formatTime(newTime));
        } catch (NumberFormatException e) {
            return "Error: Invalid skip time format. Please specify the number of seconds as a numeric value.";
        }
    }

    public String statusCommand(Boolean all) {
        StringBuilder statusBuilder = new StringBuilder();
        AudioPlayerComponent audioPlayer = channelManager.getCurrentChannel();

        if (audioPlayer.mediaPlayer().media().isValid()) {
            if (all) {
                // Handle "status all" command
                for (Meta meta : Meta.values()) {
                    String value = audioPlayer.mediaPlayer().media().meta().get(meta);
                    if (value != null && !value.isEmpty()) {
                        statusBuilder.append(meta.name()).append(": ").append(value).append("\n");
                    }
                }
            } else {
                // Handle regular "status" command
                String title = audioPlayer.mediaPlayer().media().meta().get(Meta.TITLE);
                String mediaName = audioPlayer.mediaPlayer().media().info().mrl(); // Gets the MRL
                statusBuilder.append("Media Name: ").append(title != null && !title.isEmpty() ? title : mediaName).append("\n");

                long durationMs = audioPlayer.mediaPlayer().status().length();
                String duration = formatTime(durationMs);
                statusBuilder.append("Duration: ").append(duration).append("\n");
            }

            // Additional media status information
            String mediaPath = audioPlayer.mediaPlayer().media().info().mrl();
            String mediaState = audioPlayer.mediaPlayer().status().state().toString();
            String currentTime = formatTime(audioPlayer.mediaPlayer().status().time());
            int volume = audioPlayer.mediaPlayer().audio().volume();
            boolean isMute = audioPlayer.mediaPlayer().audio().isMute();

            statusBuilder.append("Media Path: ").append(mediaPath).append("\n")
                    .append("Media State: ").append(mediaState).append("\n")
                    .append("Current Time: ").append(currentTime).append("\n")
                    .append("Volume: ").append(volume).append("\n")
                    .append("Mute: ").append(isMute).append("\n");
        } else {
            statusBuilder.append("No valid media loaded.");
        }

        return statusBuilder.toString();
    }

    public String stopCommand() {
        AudioPlayerComponent audioPlayer = channelManager.getCurrentChannel();
        State currentState = audioPlayer.mediaPlayer().status().state();

        if (currentState != State.STOPPED) {
            audioPlayer.mediaPlayer().controls().stop();
            return "Playback stopped.";
        } else {
            return "Playback is already stopped.";
        }
    }

    public String volumeCommand(int volume) {
        AudioPlayerComponent audioPlayer = channelManager.getCurrentChannel();
        try {
            if (volume >= 0 && volume <= 150) {
                audioPlayer.mediaPlayer().audio().setVolume(volume);
                return "Volume set to " + volume + ".";
            } else {
                return "Invalid volume level. Volume must be between 0 and 150.";
            }
        } catch (NumberFormatException e) {
            return "Invalid volume level. Please provide a number between 0 and 150.";
        }
    }

    public String volumeDownCommand() {
        AudioPlayerComponent audioPlayer = channelManager.getCurrentChannel();
        int volume = audioPlayer.mediaPlayer().audio().volume();
        int newVolume = Math.max(0, volume - 5);  // Ensure volume does not go below 0
        audioPlayer.mediaPlayer().audio().setVolume(newVolume);

        if (newVolume == volume) {
            return "Volume is already at the minimum level.";
        } else {
            return "Volume decreased to " + newVolume + ".";
        }
    }

    public String volumeUpCommand() {
        AudioPlayerComponent audioPlayer = channelManager.getCurrentChannel();
        int volume = audioPlayer.mediaPlayer().audio().volume();
        int newVolume = Math.min(150, volume + 5);  // Ensure volume does not exceed the max limit of 150

        audioPlayer.mediaPlayer().audio().setVolume(newVolume);

        if (newVolume == volume) {
            return "Volume is already at the maximum level.";
        } else {
            return "Volume increased to " + newVolume + ".";
        }
    }

    public String addChannelCommand(String channelName) {
        return channelManager.addChannel(channelName);
    }

    public String listChannelCommand() {
        return channelManager.listChannels();
    }

    public String removeChannelCommand(String channelName) {
        // Use the removeChannel method and return its message
        return channelManager.removeChannel(channelName);
    }

    public String selectChannelCommand(String channelName) {
        return channelManager.selectChannel(channelName);
    }

    private long parseTime(String timeStr) {
        String[] parts = timeStr.split(":");
        long hours = 0;
        long minutes = 0;
        long seconds = 0;

        try {
            if (parts.length == 3) {
                // Format is HH:MM:SS
                hours = Long.parseLong(parts[0]);
                minutes = Long.parseLong(parts[1]);
                seconds = Long.parseLong(parts[2]);
            } else if (parts.length == 2) {
                // Format is MM:SS
                minutes = Long.parseLong(parts[0]);
                seconds = Long.parseLong(parts[1]);
            } else if (parts.length == 1) {
                // Format is SS
                seconds = Long.parseLong(parts[0]);
            } else {
                throw new IllegalArgumentException("Invalid time format. Use HH:MM:SS, MM:SS, or SS.");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid time format. Use HH:MM:SS, MM:SS, or SS.");
        }

        return ((hours * 60 + minutes) * 60 + seconds) * 1000;  // Convert to milliseconds
    }

    private String formatTime(long timeInMillis) {
        long totalSeconds = timeInMillis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
}

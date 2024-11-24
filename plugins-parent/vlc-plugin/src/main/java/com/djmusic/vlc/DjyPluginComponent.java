package com.djmusic.vlc;

import com.djmusic.vlc.base.ChannelManager;
import com.social100.processor.AIAController;
import com.social100.processor.Action;
import uk.co.caprica.vlcj.media.Meta;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.State;
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent;

import java.io.File;

@AIAController(name = "djy",
        type = "",
        description = "description")
public class DjyPluginComponent {
    final static String MAIN_GROUP = "Main";
    final static String CHANNELS_GROUP = "Channels";

    private final ChannelManager channelManager;

    private static final String[] mediaOptions = {
            ":audio-filter=normvol",
            ":norm-buff-size=20",  // Buffer size for normalization
            ":norm-max-level=1.0"  // Maximum level for normalized audio
    };

    public DjyPluginComponent() {
        System.setProperty("jna.library.path", "C:\\Program Files\\VideoLAN\\VLC");
        channelManager = new ChannelManager();
    }

    @Action(group = MAIN_GROUP, 
            command = "move",
            description = "Moves the playback to the specified time. Usage: move <HH:MM:SS|MM:SS|SS>")
    public String moveCommand(String[] args) {
        if (args.length == 0) {
            return "Error: Please specify the time to move to. Usage: move <time>";
        }

        AudioPlayerComponent audioPlayer = channelManager.getCurrentChannel();
        try {
            long moveToTime = parseTime(args[0]);  // Parse the time string
            audioPlayer.mediaPlayer().controls().setTime(moveToTime);
            return "Playback moved to " + args[0] + ".";
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "Failed to move playback due to an unexpected error.";
        }
    }

    @Action(group = MAIN_GROUP, 
            command = "mute",
            description = "Toggles the mute state of the playback if valid media is loaded.")
    public String muteCommand(String[] args) {
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

    @Action(group = MAIN_GROUP, 
            command = "pause",
            description = "Pauses the playback if it is currently playing.")
    public String pauseCommand(String[] args) {
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

    @Action(group = MAIN_GROUP, 
            command = "play",
            description = "Plays the specified media file. If no file is specified, resumes the current one.")
    public String playCommand(String[] args) {
        AudioPlayerComponent audioPlayer = channelManager.getCurrentChannel();
        MediaPlayer mediaPlayer = audioPlayer.mediaPlayer();

        String currentMediaPath = mediaPlayer.media().info() != null ? mediaPlayer.media().info().mrl() : null;

        if (args.length > 0) {
            String mediaPathToPlay = args[0];
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

    @Action(group = MAIN_GROUP, 
            command = "skip",
            description = "Skips the playback forward or backward by the specified number of seconds. Usage: skip <+/-seconds>")
    public String skipCommand(String[] args) {
        if (args.length == 0) {
            return "Error: Please specify the number of seconds to skip. Usage: skip <+/-seconds>";
        }

        AudioPlayerComponent audioPlayer = channelManager.getCurrentChannel();
        long mediaLength = audioPlayer.mediaPlayer().status().length();

        try {
            long skipTime = Long.parseLong(args[0]);  // Positive for forward, negative for backward
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

    @Action(group = MAIN_GROUP, 
            command = "status",
            description = "Displays the current status of VLC. Use 'status all' for all media info available.")
    public String statusCommand(String[] args) {
        StringBuilder statusBuilder = new StringBuilder();
        AudioPlayerComponent audioPlayer = channelManager.getCurrentChannel();

        if (audioPlayer.mediaPlayer().media().isValid()) {
            if (args.length > 0 && "all".equalsIgnoreCase(args[0])) {
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

    @Action(group = MAIN_GROUP, 
            command = "stop",
            description = "Stops the playback if it is currently active.")
    public String stopCommand(String[] args) {
        AudioPlayerComponent audioPlayer = channelManager.getCurrentChannel();
        State currentState = audioPlayer.mediaPlayer().status().state();

        if (currentState != State.STOPPED) {
            audioPlayer.mediaPlayer().controls().stop();
            return "Playback stopped.";
        } else {
            return "Playback is already stopped.";
        }
    }

    @Action(group = MAIN_GROUP, 
            command = "volume",
            description = "Sets the volume to a specified level between 0 and 150. Usage: volume <level>")
    public String volumeCommand(String[] args) {
        AudioPlayerComponent audioPlayer = channelManager.getCurrentChannel();

        if (args.length > 0) {
            try {
                int volume = Integer.parseInt(args[0]);
                if (volume >= 0 && volume <= 150) {
                    audioPlayer.mediaPlayer().audio().setVolume(volume);
                    return "Volume set to " + volume + ".";
                } else {
                    return "Invalid volume level. Volume must be between 0 and 150.";
                }
            } catch (NumberFormatException e) {
                return "Invalid volume level. Please provide a number between 0 and 150.";
            }
        } else {
            return "No volume level provided. Please specify a volume level between 0 and 150.";
        }
    }

    @Action(group = MAIN_GROUP, 
            command = "volume-down",
            description = "Decreases the volume by 5 units.")
    public String volumeDownCommand(String[] args) {
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

    @Action(group = MAIN_GROUP, 
            command = "volume-up",
            description = "Increases the volume by 5 units.")
    public String volumeUpCommand(String[] args) {
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

    @Action(group = CHANNELS_GROUP,
            command = "add-channel",
            description = "Adds a new channel. Usage: add-channel <channelName>")
    public String addChannelCommand(String[] args) {
        if (args.length > 0) {
            String channelName = args[0];
            return channelManager.addChannel(channelName);
        } else {
            return "Error: Please provide a channel name. Usage: add-channel <channelName>";
        }
    }

    @Action(group = CHANNELS_GROUP,
            command = "list-channels",
            description = "Lists all available channels.")
    public String listChannelCommand(String[] args) {
        return channelManager.listChannels();
    }

    @Action(group = CHANNELS_GROUP,
            command = "remove-channel",
            description = "Removes an existing channel. Usage: remove-channel <channelName>")
    public String removeChannelCommand(String[] args) {
        if (args.length > 0) {
            String channelName = args[0];
            // Use the removeChannel method and return its message
            return channelManager.removeChannel(channelName);
        } else {
            return "Error: Please provide a channel name. Usage: remove-channel <channelName>";
        }
    }

    @Action(group = CHANNELS_GROUP,
            command = "select-channel",
            description = "Selects an existing channel. Usage: select-channel <channelName>")
    public String selectChannelCommand(String[] args) {
        if (args.length > 0) {
            String channelName = args[0];
            return channelManager.selectChannel(channelName);
        } else {
            return "Error: Please provide a channel name. Usage: select-channel <channelName>";
        }
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

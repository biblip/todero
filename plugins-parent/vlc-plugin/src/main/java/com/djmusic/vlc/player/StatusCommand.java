package com.djmusic.vlc.player;

import com.djmusic.vlc.base.ChannelManager;
import com.social100.todero.common.Command;
import uk.co.caprica.vlcj.media.Meta;
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent;

import java.util.concurrent.TimeUnit;

public class StatusCommand implements Command {

    @Override
    public String execute(Object object, String[] args) {
        StringBuilder statusBuilder = new StringBuilder();
        AudioPlayerComponent audioPlayer = ((ChannelManager) object).getCurrentChannel();

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

    private String formatTime(long timeMs) {
        long hours = TimeUnit.MILLISECONDS.toHours(timeMs);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeMs) - TimeUnit.HOURS.toMinutes(hours);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeMs) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeMs));

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    @Override
    public String name() {
        return "status";
    }

    @Override
    public String description() {
        return "Displays the current status of VLC. Use 'status all' for all media info available.";
    }
}

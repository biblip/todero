package com.djmusic.vlc.player;

import com.djmusic.vlc.base.ChannelManager;
import com.social100.todero.common.Command;
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent;

public class MoveCommand implements Command {
    @Override
    public String execute(Object object, String[] args) {
        if (args.length == 0) {
            return "Error: Please specify the time to move to. Usage: move <time>";
        }

        AudioPlayerComponent audioPlayer = ((ChannelManager) object).getCurrentChannel();
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

    @Override
    public String name() {
        return "move";
    }

    @Override
    public String description() {
        return "Moves the playback to the specified time. Usage: move <HH:MM:SS|MM:SS|SS>";
    }
}

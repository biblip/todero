package com.djmusic.vlc.player;

import com.djmusic.vlc.base.ChannelManager;
import com.social100.todero.common.Command;
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent;

public class SkipCommand implements Command {
    @Override
    public String execute(Object object, String[] args) {
        if (args.length == 0) {
            return "Error: Please specify the number of seconds to skip. Usage: skip <+/-seconds>";
        }

        AudioPlayerComponent audioPlayer = ((ChannelManager) object).getCurrentChannel();
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

    @Override
    public String name() {
        return "skip";
    }

    @Override
    public String description() {
        return "Skips the playback forward or backward by the specified number of seconds. Usage: skip <+/-seconds>";
    }
}

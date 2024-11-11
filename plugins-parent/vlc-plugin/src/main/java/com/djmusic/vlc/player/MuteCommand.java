package com.djmusic.vlc.player;

import com.djmusic.vlc.base.ChannelManager;
import com.social100.todero.common.Command;
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent;

public class MuteCommand implements Command {
    @Override
    public String execute(Object object, String[] args) {
        AudioPlayerComponent audioPlayer = ((ChannelManager) object).getCurrentChannel();

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

    @Override
    public String name() {
        return "mute";
    }

    @Override
    public String description() {
        return "Toggles the mute state of the playback if valid media is loaded.";
    }
}

package com.djmusic.vlc.player;

import com.djmusic.vlc.base.ChannelManager;
import com.social100.todero.common.Command;
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent;

public class VolumeUpCommand implements Command {
    @Override
    public String execute(Object object, String[] args) {
        AudioPlayerComponent audioPlayer = ((ChannelManager) object).getCurrentChannel();
        int volume = audioPlayer.mediaPlayer().audio().volume();
        int newVolume = Math.min(150, volume + 5);  // Ensure volume does not exceed the max limit of 150

        audioPlayer.mediaPlayer().audio().setVolume(newVolume);

        if (newVolume == volume) {
            return "Volume is already at the maximum level.";
        } else {
            return "Volume increased to " + newVolume + ".";
        }
    }

    @Override
    public String name() {
        return "volume-up";
    }

    @Override
    public String description() {
        return "Increases the volume by 5 units.";
    }
}

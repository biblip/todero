package com.djmusic.vlc.player;

import com.djmusic.vlc.base.ChannelManager;
import com.social100.todero.common.Command;
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent;

public class VolumeCommand implements Command {
    @Override
    public String execute(Object object, String[] args) {
        AudioPlayerComponent audioPlayer = ((ChannelManager) object).getCurrentChannel();

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

    @Override
    public String name() {
        return "volume";  // Updated to reflect the command's purpose more accurately
    }

    @Override
    public String description() {
        return "Sets the volume to a specified level between 0 and 150. Usage: volume <level>";
    }
}

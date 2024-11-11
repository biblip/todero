package com.djmusic.vlc.channel;

import com.djmusic.vlc.base.ChannelManager;
import com.social100.todero.common.Command;

public class SelectChannelCommand implements Command {
    @Override
    public String execute(Object object, String[] args) {
        ChannelManager channelManager = (ChannelManager) object;
        if (args.length > 0) {
            String channelName = args[0];
            return channelManager.selectChannel(channelName);
        } else {
            return "Error: Please provide a channel name. Usage: select-channel <channelName>";
        }
    }

    @Override
    public String name() {
        return "select-channel";
    }

    @Override
    public String description() {
        return "Selects an existing channel. Usage: select-channel <channelName>";
    }
}

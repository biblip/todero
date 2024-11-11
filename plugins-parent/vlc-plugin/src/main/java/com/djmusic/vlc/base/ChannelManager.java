package com.djmusic.vlc.base;

import uk.co.caprica.vlcj.player.component.AudioPlayerComponent;

import java.util.LinkedHashMap;
import java.util.Map;

public class ChannelManager {
    private final Map<String, AudioPlayerComponent> channels = new LinkedHashMap<>();
    private String currentChannelName = "default";

    public ChannelManager() {
        channels.put(currentChannelName, new AudioPlayerComponent());  // Initialize the default channel
    }

    public String addChannel(String name) {
        if (channels.size() >= 5) {
            return "Maximum number of channels reached.";
        }
        if (channels.containsKey(name)) {
            return "Channel already exists.";
        }
        channels.put(name, new AudioPlayerComponent());
        return "Channel '" + name + "' successfully added.";
    }

    public String removeChannel(String name) {
        if (channels.size() <= 1) {
            return "Cannot remove the last channel.";
        }
        if (name.equals(currentChannelName)) {
            return "Cannot remove the current channel.";
        }
        AudioPlayerComponent component = channels.remove(name);
        if (component != null) {
            component.release();  // Release the AudioPlayerComponent resources
            return "Channel '" + name + "' removed.";
        } else {
            return "Channel not found.";
        }
    }

    public String selectChannel(String name) {
        if (channels.containsKey(name)) {
            currentChannelName = name;
            return "Channel '" + name + "' selected.";
        } else {
            return "Channel '" + name + "' not found.";
        }
    }

    public String getCurrentChannelName() {
        return currentChannelName;
    }

    public String listChannels() {
        if (channels.isEmpty()) {
            return "No channels available.";
        }
        StringBuilder channelList = new StringBuilder("Available channels:\n");
        channels.keySet().forEach(name -> channelList.append("- ").append(name).append("\n"));
        return channelList.toString();
    }

    public AudioPlayerComponent getCurrentChannel() {
        return channels.get(currentChannelName);
    }

    public void unload() {
        if (channels.isEmpty()) {
            return;
        }
        channels.keySet().forEach(channelName -> {
            AudioPlayerComponent channel = channels.get(channelName);
            channel.mediaPlayer().controls().stop();
            channel.release();
        });
    }
}

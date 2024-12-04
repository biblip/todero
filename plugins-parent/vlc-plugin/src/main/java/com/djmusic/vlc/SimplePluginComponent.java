package com.djmusic.vlc;

import com.social100.processor.AIAController;
import com.social100.processor.Action;

@AIAController(name = "fsimple",
        type = "",
        description = "description")
public class SimplePluginComponent {
    final static String MAIN_GROUP = "Main";
    final static String CHANNELS_GROUP = "Channels";

    public SimplePluginComponent() {
    }

    @Action(group = MAIN_GROUP, 
            command = "print",
            description = "Prints data to the output")
    public String printStatus(String[] args) {
        if (args.length == 0) {
            return "Error: Please specify the value to print. Usage: print <text>";
        }

        try {
            String value = args[0];
            return "echo (" + value + ")";
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "Failed to move playback due to an unexpected error.";
        }
    }
}

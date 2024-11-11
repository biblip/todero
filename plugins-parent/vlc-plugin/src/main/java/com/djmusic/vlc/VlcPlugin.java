package com.djmusic.vlc;

import com.djmusic.vlc.base.ChannelManager;
import com.social100.todero.common.Command;
import com.social100.todero.common.PluginInterface;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class VlcPlugin implements PluginInterface {
    private final ChannelManager channelManager;
    private final Map<String, Command> commandMap = new HashMap<>();

    public VlcPlugin() {
        System.setProperty("jna.library.path", "C:\\Program Files\\VideoLAN\\VLC");
        channelManager = new ChannelManager();
        discoverCommands();
    }

    @Override
    public Boolean hasCommand(String command) {
        return commandMap.containsKey(command);
    }

    public void discoverCommands() {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("com.djmusic.vlc", this.getClass().getClassLoader()))
                .setScanners(new SubTypesScanner()) // 'false' to exclude Object.class
                .addClassLoader(this.getClass().getClassLoader())
        );

        Set<Class<? extends Command>> commandClasses = reflections.getSubTypesOf(Command.class);

        for (Class<? extends Command> cls : commandClasses) {
            try {
                Command commandInstance = cls.getDeclaredConstructor().newInstance();
                String commandName = commandInstance.name();

                // Verify that the command name is a single word (contains no spaces)
                if (commandName.trim().contains(" ")) {
                    System.err.println("Invalid command name (should it be one word): " + commandName);
                    continue; // Skip this command
                }

                commandMap.put(commandName, commandInstance);

            } catch (Exception e) {
                System.err.println("Could not instantiate command: " + cls.getName());
                e.printStackTrace();
            }
        }
    }

    @Override
    public String execute(String command, String[] commandArgs) {
        Command commandScript = commandMap.get(command);
        if (commandScript != null) {
            return commandScript.execute(channelManager, commandArgs);
        }
        return "Command Not Found";
    }

    @Override
    public String name() {
        return "vlc";
    }

    @Override
    public String description() {
        return "description";
    }

    @Override
    public String[] getAllCommandNames() {
        return commandMap.keySet().toArray(new String[0]);
    }

    @Override
    public String getHelpMessage() {
        StringBuilder helpMessage = new StringBuilder();
        for (String commandName : commandMap.keySet()) {
            helpMessage.append(String.format("-  %-15s : %s\n", commandMap.get(commandName).name(), commandMap.get(commandName).description()));
        }
        return helpMessage.toString();
    }
}

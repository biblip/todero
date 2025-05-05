package com.social100.todero.console.workspace;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Workspace {
    private String username;
    private File workspaceDir;
    private File pluginsDir;
    private Map<String, PluginWorkspace> plugins = new ConcurrentHashMap<>();

    /**
     * Constructs a Workspace for the given user.
     * It creates the base workspace directory and a plugins directory.
     *
     * @param username the user identifier
     * @param workspaceDir the file system directory for this workspace
     */
    public Workspace(String username, File workspaceDir) {
        this.username = username;
        this.workspaceDir = workspaceDir;
        this.pluginsDir = new File(workspaceDir, "plugins");

        // Ensure the directories exist.
        if (!workspaceDir.exists()) {
            workspaceDir.mkdirs();
        }
        if (!pluginsDir.exists()) {
            pluginsDir.mkdirs();
        }
    }

    /**
     * Retrieves the Plugin instance for the given plugin name.
     * If it does not exist, it is created.
     *
     * @param pluginName the name of the plugin
     * @return the Plugin instance associated with this workspace
     */
    public PluginWorkspace getPlugin(String pluginName) {
        return plugins.computeIfAbsent(pluginName,
                p -> new PluginWorkspace(pluginName, this));
    }

    public File getWorkspaceDir() {
        return workspaceDir;
    }

    public File getPluginsDir() {
        return pluginsDir;
    }
}

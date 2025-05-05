package com.social100.todero.console.workspace;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WorkspaceManager {
    // Base directory under which all workspaces are stored.
    private static final String BASE_DIRECTORY = "workspaces";

    // Map from username to their Workspace
    private final Map<String, Workspace> workspaces = new ConcurrentHashMap<>();

    /**
     * Returns the Workspace for the given username.
     * If the workspace does not exist, it is created.
     *
     * @param username the user identifier
     * @return the user's Workspace instance
     */
    public Workspace getWorkspace(String username) {
        return workspaces.computeIfAbsent(username,
                u -> new Workspace(u, new File(BASE_DIRECTORY, u)));
    }
}

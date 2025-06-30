package com.social100.todero.common.config;

import com.social100.todero.common.config.plugin.PluginConfig;

import java.util.List;

public interface ApplicationINI {
    List<String> getPlugins();
    List<String> getWorkspaces();
    List<String> getUsers();
    List<String> getGroups();
    PluginConfig getPluginConfig(String name);
    WorkspaceConfig getWorkspaceConfig(String name);
    UserConfig getUserConfig(String name);
    GroupConfig getGroupConfig(String name);
}

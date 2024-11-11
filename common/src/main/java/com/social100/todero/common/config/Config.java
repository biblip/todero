package com.social100.todero.common.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Map;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Config extends ConfigParameters {
    private String version;
    private PluginsConfig plugins;
    private WorkspacesConfig workspaces;
    private Map<String, UserConfig> users;
    private Map<String, GroupConfig> groups;
}

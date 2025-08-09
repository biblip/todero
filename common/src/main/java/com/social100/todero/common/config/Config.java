package com.social100.todero.common.config;

import com.social100.todero.common.config.being.BeingsConfig;
import com.social100.todero.common.config.plugin.PluginsConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Config extends ConfigParameters {
    private String version;
    private ServerConfig ai_server;
    private ServerConfig aia_server;
    private PluginsConfig plugins;
    private BeingsConfig beings;
    private WorkspacesConfig workspaces;
    private Map<String, UserConfig> users;
    private Map<String, GroupConfig> groups;
}

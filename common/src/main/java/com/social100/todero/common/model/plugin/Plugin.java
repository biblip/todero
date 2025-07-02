package com.social100.todero.common.model.plugin;

import com.social100.todero.common.config.ServerType;
import lombok.Builder;
import lombok.Data;

import java.io.File;
import java.net.URLClassLoader;

@Data
@Builder
public class Plugin {
    String dir;
    File file;
    String name;
    ServerType type;
    String version;
    URLClassLoader classLoader;
    Class<? extends PluginInterface> pluginClass;
    PluginInterface pluginInstance;
    Component component;
}

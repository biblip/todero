package com.social100.todero.common.model.plugin;

import lombok.Builder;
import lombok.Data;

import java.io.File;
import java.net.URLClassLoader;
import java.util.Map;

@Data
@Builder
public class Plugin {
    String dir;
    File file;
    String name;
    String version;
    URLClassLoader classLoader;
    Class<? extends PluginInterface> pluginClass;
    PluginInterface pluginInstance;
    Map<String, PluginSection> sections;
}

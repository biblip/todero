package com.social100.todero.common.config.plugin;

import com.social100.todero.common.config.ConfigParameters;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PluginsConfig extends ConfigParameters {
    private String dir;
    private Map<String, PluginConfig> list;
}

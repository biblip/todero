package com.social100.todero.common.model.plugin;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class PluginSection {
    String name;
    Map<String, Command> commands;
}

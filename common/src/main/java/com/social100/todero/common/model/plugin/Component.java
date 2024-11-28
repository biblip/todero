package com.social100.todero.common.model.plugin;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class Component {
    String name;
    String description;
    Map<String, Command> commands;
}

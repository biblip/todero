package com.social100.todero.common.model.plugin;

import com.social100.todero.common.config.ServerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Component {
    String name;
    String description;
    ServerType type;
    Map<String, Map<String, Command>> commands;
}

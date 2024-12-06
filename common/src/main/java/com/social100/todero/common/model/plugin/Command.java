package com.social100.todero.common.model.plugin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Command {
    private String group;
    private String command;
    private String description;
    private String method;
    private String isStatic;
}
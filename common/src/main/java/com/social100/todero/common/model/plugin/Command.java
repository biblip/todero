package com.social100.todero.common.model.plugin;

import com.social100.todero.common.CommandMethod;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Command {
    String command;
    String description;
    CommandMethod commandMethod;
}

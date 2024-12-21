package com.social100.todero.common.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
public class CommandContext {
    private final String sourceId;
    @Getter
    private final String[] args;

}

package com.social100.todero.common.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WorkspaceConfig extends ConfigParameters {
    private String path;
    private String name;
    private OwnerConfig owner;
    private String desk;
    private List<String> plugins;
}

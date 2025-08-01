package com.social100.todero.common.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WorkspacesConfig extends ConfigParameters {
    private String dir;
    private Map<String, WorkspaceConfig> list;
}

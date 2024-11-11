package com.social100.todero.common.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PluginConfig extends ConfigParameters {
    private String name;
    private String libdir;
    private String licence;
    private Boolean cascade;
    private String certificate;
}

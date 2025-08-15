package com.social100.todero.common.config;

import com.social100.todero.common.config.being.BeingsConfig;
import com.social100.todero.common.config.plugin.PluginsConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Profile extends ConfigParameters {
    private String device_id;
    private String api_token;
}

package com.social100.todero.common.config.being;

import com.social100.todero.common.config.ConfigParameters;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BeingsConfig extends ConfigParameters {
    private String dir;
    private Map<String, BeingConfig> list;
}

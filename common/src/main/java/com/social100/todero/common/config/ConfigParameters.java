package com.social100.todero.common.config;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ConfigParameters {
    private Map<String, Object> parameters = new HashMap<>();

    @JsonAnySetter
    public void setParameters(String key, Object value) {
        this.parameters.put(key, value);
    }
}

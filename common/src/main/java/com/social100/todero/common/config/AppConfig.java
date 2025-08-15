package com.social100.todero.common.config;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AppConfig {
    private Profile profile;
    private Config app;
    private String version;
}

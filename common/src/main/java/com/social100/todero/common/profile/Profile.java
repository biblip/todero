package com.social100.todero.common.profile;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Profile {
    private final String profileId;
    private final String profileName;

}

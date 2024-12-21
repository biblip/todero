package com.social100.todero.common.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class Profile {
    private final String profileId;
    private final String profileName;

}

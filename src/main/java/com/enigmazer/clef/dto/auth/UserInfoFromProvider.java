package com.enigmazer.clef.dto.auth;

import com.enigmazer.clef.enums.SocialAccountProvider;
import lombok.Builder;

@Builder
public record UserInfoFromProvider(
        SocialAccountProvider providerEnum,
        String providerId,
        String email,
        String name,
        String avatarUrl
) {}

package com.enigmazer.clef.service.auth;

import com.enigmazer.clef.dto.auth.CustomOAuth2User;
import com.enigmazer.clef.dto.auth.UserInfoFromProvider;
import com.enigmazer.clef.entity.Role;
import com.enigmazer.clef.entity.SocialAccount;
import com.enigmazer.clef.entity.User;
import com.enigmazer.clef.enums.RoleType;
import com.enigmazer.clef.enums.SocialAccountProvider;
import com.enigmazer.clef.exception.SystemResourceNotFoundException;
import com.enigmazer.clef.repository.RoleRepository;
import com.enigmazer.clef.repository.SocialAccountRepository;
import com.enigmazer.clef.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SocialAccountRepository socialAccountRepository;

    private final RestTemplate restTemplate;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String providerString = userRequest.getClientRegistration().getRegistrationId();
        boolean hasPasswordResetIntent = providerString.endsWith("-reset");
        if(hasPasswordResetIntent) providerString = providerString.replace("-reset", "");

        UserInfoFromProvider userInfo = getVerifiedUserInfo(
                oAuth2User, providerString, attributes, userRequest);

        Optional<User> existingUser = userRepository.findByEmail(userInfo.email());
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            if(hasPasswordResetIntent){
                if(user.getPassword() == null)
                    throw new OAuth2AuthenticationException(
                            new OAuth2Error("password_reset_unavailable"),
                            "password_reset_unavailable"
                    );
            }
            checkActive(user, providerString);
            log.info("Existing user login [provider={}, userId={}]", providerString, user.getId());
        } else {
            if(hasPasswordResetIntent) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("account_unavailable"),
                        "account_unavailable"
                );
            }
            user = registerUser(userInfo);
            log.info("Registered new user [provider={}, userId={}", providerString, user.getId());
        }

        linkSocialAccount(user, userInfo, hasPasswordResetIntent);

        String authorityName = "ROLE_" + user.getRole().getName().name();

        log.info("Returned default oauth2 user [userId={}, role={}, " +
                "provider={}]", user.getId(), authorityName, providerString);
        return new CustomOAuth2User(
                new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority(authorityName)),
                Collections.singletonMap("email", user.getEmail()),
                "email"),
                hasPasswordResetIntent
        );
    }

    // --- Helper Methods ---
    private UserInfoFromProvider getVerifiedUserInfo(
            OAuth2User oAuth2User,
            String providerString,
            Map<String, Object> attributes,
            OAuth2UserRequest userRequest
    ){

        SocialAccountProvider providerEnum = getSocialAccountProvider(providerString);
        String providerId;
        String email;
        String name;

        if ("google".equals(providerString)) {
            providerId = getProviderId(oAuth2User.getAttribute("sub"), providerEnum.name());
            email = fetchVerifiedGoogleEmail(oAuth2User, attributes);
            name = oAuth2User.getAttribute("name");
            if (name == null || name.isBlank()) {
                name = email.substring(0, email.indexOf("@"));
            }

        } else if ("github".equals(providerString)) {
            providerId = getProviderId(String.valueOf(attributes.get("id")), providerEnum.name());
            email = fetchVerifiedGitHubEmail(userRequest.getAccessToken().getTokenValue());
            name = oAuth2User.getAttribute("name");
            if (name == null || name.isBlank()) {
                name = oAuth2User.getAttribute("login");
            }

        } else {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("unsupported_provider"),
                    "unsupported_provider"
            );
        }

        return UserInfoFromProvider.builder()
                .providerEnum(providerEnum)
                .providerId(providerId)
                .email(email)
                .name(name)
                .build();
    }

    private SocialAccountProvider getSocialAccountProvider(String providerString){
        try {
            return SocialAccountProvider.valueOf(providerString.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Login/registration attempt with Unsupported " +
                    "provider [provider={}]", providerString);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("unsupported_provider"),
                    "unsupported_provider"
            );
        }
    }

    private String getProviderId(String providerId, String provider) {
        if (providerId == null || providerId.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("missing_provider_id"),
                    "missing_provider_id"
            );
        }
        return providerId;
    }

    private String fetchVerifiedGoogleEmail(OAuth2User oAuth2User, Map<String, Object> attributes) {
        String email = oAuth2User.getAttribute("email");
        if (email == null || email.isBlank()) {
            log.warn("Email is missing from provider [provider=google, email={}]", email);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("missing_google_email"),
                    "missing_google_email");
        }
        if(!Boolean.TRUE.equals(attributes.get("email_verified"))){
            log.warn("Blocked unverified email registration/login " +
                    "attempt [provider=google, email={}]", email);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("unverified_email"),
                    "unverified_email");
        }
        return email;
    }

    private String fetchVerifiedGitHubEmail(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    "https://api.github.com/user/emails",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            List<Map<String, Object>> emails = response.getBody();
            if (emails != null) {
                for (Map<String, Object> emailData : emails) {
                    Boolean primary = (Boolean) emailData.get("primary");
                    Boolean verified = (Boolean) emailData.get("verified");
                    if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)) {
                        return (String) emailData.get("email");
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch email from GitHub API ",e);
        }
        throw new OAuth2AuthenticationException(
                new OAuth2Error("missing_github_email"),
                "missing_github_email"
        );
    }

    private User registerUser(UserInfoFromProvider userInfo){
        Role userRole = roleRepository.findByName(RoleType.USER)
                .orElseThrow(() -> new SystemResourceNotFoundException(
                        "Default USER role not found in database: ", RoleType.USER.name())
                );

        return userRepository.save(User.builder()
                .fullName(userInfo.name())
                .email(userInfo.email())
                .role(userRole)
                .build()
        );
    }

    private void linkSocialAccount(
            User user, UserInfoFromProvider userInfo, boolean hasPasswordResetIntent
    ){
        Optional<SocialAccount> socialAccount = socialAccountRepository
                .findByProviderAndProviderId(userInfo.providerEnum(), userInfo.providerId());
        if (socialAccount.isEmpty()) {
            if(hasPasswordResetIntent) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("provider_mismatch"),
                        "provider_mismatch"
                );
            }
            SocialAccount newSocialAccount = SocialAccount.builder()
                    .user(user)
                    .provider(userInfo.providerEnum())
                    .providerId(userInfo.providerId())
                    .build();
            socialAccountRepository.save(newSocialAccount);
            log.info("Social account linked with user account [provider={}, " +
                    "userId={}]", userInfo.providerEnum().name(), user.getId());
        }
    }

    private void checkActive(User user, String providerString){
        if (!user.isActive()) {
            log.warn("Disabled user account login attempt " +
                    "[provider={}, userId={}]", providerString, user.getId());
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("account_disabled"),
                    "account_disabled");
        }
    }
}
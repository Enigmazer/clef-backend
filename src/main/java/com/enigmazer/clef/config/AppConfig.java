package com.enigmazer.clef.config;

import com.enigmazer.clef.config.security.SecurityConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Separate configuration class to break the circular dependency between
 * {@link SecurityConfig} and {@link com.enigmazer.clef.service.auth.CustomOAuth2UserService}.
 *
 * SpringSecurity requires a CustomOAuth2UserService bean, and
 * CustomOAuth2UserService requires a RestTemplate bean. Defining
 * RestTemplate here breaks the cycle.
 */
@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }
}

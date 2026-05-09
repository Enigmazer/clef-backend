package com.enigmazer.clef.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(){
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        cacheManager.registerCustomCache("subjects",
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(12, TimeUnit.HOURS)
                        .recordStats()
                        .build()
        );

        cacheManager.registerCustomCache("urls",
                Caffeine.newBuilder()
                        .maximumSize(1000)
                        .expireAfterWrite(55, TimeUnit.MINUTES)
                        .recordStats()
                        .build()
        );
        return cacheManager;
    }
}

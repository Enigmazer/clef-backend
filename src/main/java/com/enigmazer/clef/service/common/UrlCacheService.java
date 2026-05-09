package com.enigmazer.clef.service.common;

import com.enigmazer.clef.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UrlCacheService {

    private final StorageService storageService;
    private final CacheManager cacheManager;

    @Cacheable(value = "urls", key = "'syllabus:' + #subjectId")
    public String getSyllabusUrl(String syllabusKey, Long subjectId){
        return storageService.generateSyllabusUrl(syllabusKey);
    }

    @Cacheable(value = "urls", key = "'material:' + #subjectId + ':' + #topicMaterialId")
    public String getTopicMaterialUrl(String topicMaterialKey, Long topicMaterialId){
        return storageService.generateTopicMaterialUrl(topicMaterialKey);
    }

    @CacheEvict(value = "urls", key = "'material:' + #subjectId + ':' + #topicMaterialId")
    public void evictTopicMaterialUrl(Long subjectId, Long topicMaterialId){}

    public void evictAllMaterialUrlsForSubject(Long subjectId) {
        Cache springCache = cacheManager.getCache("urls");

        if (springCache instanceof CaffeineCache caffeineCache) {
            String materialPrefix = "material:" + subjectId + ":";
            caffeineCache.getNativeCache().asMap().keySet()
                    .removeIf(key -> key.toString().startsWith(materialPrefix));
        }
    }
}

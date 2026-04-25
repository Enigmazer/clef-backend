package com.enigmazer.clef.service.topicMaterial;

import com.enigmazer.clef.dto.topicMaterial.TopicMaterialDeleteRequest;
import com.enigmazer.clef.dto.topicMaterial.TopicMaterialResponse;
import com.enigmazer.clef.dto.topicMaterial.TopicMaterialUrlResponse;
import com.enigmazer.clef.entity.Subject;
import com.enigmazer.clef.entity.Topic;
import com.enigmazer.clef.entity.TopicMaterial;
import com.enigmazer.clef.enums.TopicMaterialType;
import com.enigmazer.clef.exception.ResourceNotFoundException;
import com.enigmazer.clef.mapper.TopicMaterialMapper;
import com.enigmazer.clef.repository.TopicMaterialRepository;
import com.enigmazer.clef.repository.TopicRepository;
import com.enigmazer.clef.service.common.SubjectHelper;
import com.enigmazer.clef.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopicMaterialServiceImpl implements TopicMaterialService{

    private final TopicMaterialRepository topicMaterialRepository;
    private final TopicRepository topicRepository;

    private final StorageService storageService;

    private final TopicMaterialMapper topicMaterialMapper;

    private final SubjectHelper subjectHelper;

    @Override
    public TopicMaterialUrlResponse getTopicMaterialUrl(
            Long subjectId, Long unitId, Long topicId,
            Long topicMaterialId, Long userId
    ) {
        String topicMaterialKey = topicMaterialRepository
                .findByIdIfAccessible(
                        topicMaterialId, topicId, unitId, subjectId, userId
                ).orElseThrow(() -> new ResourceNotFoundException("Topic material not found"))
                .getTopicMaterialKey();

        String url = storageService.generateTopicMaterialUrl(topicMaterialKey);
        log.info("Returning url for topic material [topicMaterialId={}, " +
                "subjectId={}, userId={}]", topicId, subjectId, userId);
        return new TopicMaterialUrlResponse(url);
    }

    @Override
    @Transactional
    @CacheEvict(value = "subjects", key = "#subjectId")
    public TopicMaterialResponse uploadTopicMaterial(
            Long subjectId, Long unitId, Long topicId,
            MultipartFile file, Long teacherId
    ) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        Topic topic = topicRepository.findByIdAndParentValidation(topicId, unitId, subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic Not Found"));

        String topicMaterialKey = storageService.generateTopicMaterialKey(file);

        TopicMaterialType topicMaterialType = TopicMaterialType
                .fromMimeType(file.getContentType());

        TopicMaterial topicMaterial = topicMaterialRepository.save(
                TopicMaterial.builder()
                .type(topicMaterialType)
                .title(file.getOriginalFilename())
                .topic(topic)
                .topicMaterialKey(topicMaterialKey)
                .build()
        );

        storageService.uploadTopicMaterial(file, topicMaterialKey);

        subject.touch();
        log.info("Uploaded topic material successfully [type={}, subjectId={}, " +
                "teacherId={}]", topicMaterialType, subjectId, teacherId);
        return topicMaterialMapper.toResponse(topicMaterial);
    }

    @Override
    @Transactional
    @CacheEvict(value = "subjects", key = "#subjectId")
    public void deleteTopicMaterials(
            Long subjectId, Long unitId, Long topicId,
            TopicMaterialDeleteRequest request, Long teacherId
    ) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        List<TopicMaterial> topicMaterials = topicMaterialRepository
                .findByIdsWithParentValidation(request.topicMaterialIds(), topicId, unitId, subjectId);

        List<String> topicMaterialKeys = topicMaterials.stream()
                .map(TopicMaterial::getTopicMaterialKey)
                .toList();

        if(!topicMaterials.isEmpty()){
            storageService.deleteTopicMaterials(topicMaterialKeys);
            topicMaterialRepository.deleteAll(topicMaterials);
            subject.touch();
        }

        log.info("Deleted multiple topic materials [providedIdCount={}, deletedIdCount={}]",
                request.topicMaterialIds().size(), topicMaterialKeys.size());
    }
}

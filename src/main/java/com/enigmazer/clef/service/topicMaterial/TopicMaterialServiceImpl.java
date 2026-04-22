package com.enigmazer.clef.service.topicMaterial;

import com.enigmazer.clef.entity.Subject;
import com.enigmazer.clef.entity.Topic;
import com.enigmazer.clef.entity.TopicMaterial;
import com.enigmazer.clef.enums.TopicMaterialType;
import com.enigmazer.clef.exception.ResourceNotFoundException;
import com.enigmazer.clef.repository.TopicMaterialRepository;
import com.enigmazer.clef.repository.TopicRepository;
import com.enigmazer.clef.service.common.SubjectHelper;
import com.enigmazer.clef.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final SubjectHelper subjectHelper;

    @Override
    @Transactional
    public String getTopicMaterialUrl(
            Long subjectId, Long unitId, Long topicId,
            Long topicMaterialId, Long userId
    ) {
        String topicMaterialKey = topicMaterialRepository
                .findByIdIfAccessible(
                        topicMaterialId, topicId, unitId, subjectId, userId
                ).orElseThrow(() -> new ResourceNotFoundException("Topic material not found"))
                .getTopicMaterialKey();

        log.info("Returning url for topic material [topicMaterialId={}, " +
                "subjectId={}, userId={}]", topicId, subjectId, userId);
        return storageService.generateTopicMaterialUrl(topicMaterialKey);
    }

    @Override
    @Transactional
    public String uploadTopicMaterial(
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

        topicMaterialRepository.save(
                TopicMaterial.builder()
                .type(topicMaterialType)
                .title(file.getOriginalFilename())
                .topic(topic)
                .topicMaterialKey(topicMaterialKey)
                .build()
        );

        storageService.uploadTopicMaterial(file, topicMaterialKey);

        log.info("Uploaded topic material successfully [type={}, subjectId={}, " +
                "teacherId={}]", topicMaterialType, subjectId, teacherId);
        return storageService.generateTopicMaterialUrl(topicMaterialKey);
    }

    @Override
    @Transactional
    public void deleteTopicMaterials(
            Long subjectId, Long unitId, Long topicId,
            List<Long> topicMaterialIds, Long teacherId
    ) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        List<TopicMaterial> topicMaterials = topicMaterialRepository
                .findByIdsWithParentValidation(topicMaterialIds, topicId, unitId, subjectId);

        List<String> topicMaterialKeys = topicMaterials.stream()
                .map(TopicMaterial::getTopicMaterialKey)
                .toList();

        if(!topicMaterials.isEmpty()){
            storageService.deleteTopicMaterials(topicMaterialKeys);
            topicMaterialRepository.deleteAll(topicMaterials);
        }

        log.info("Deleted multiple topic materials [providedIdCount={}, deletedIdCount={}]",
                topicMaterialIds.size(), topicMaterialKeys.size());
    }
}

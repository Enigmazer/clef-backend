package com.enigmazer.clef.service.topicMaterial;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TopicMaterialService {

    String getTopicMaterialUrl(Long subjectId, Long unitId, Long topicId, Long topicMaterialId, Long teacherId);

    String uploadTopicMaterial(Long subjectId, Long unitId, Long topicId, MultipartFile file, Long teacherId);

    void deleteTopicMaterials(Long subjectId, Long unitId, Long topicId, List<Long> topicMaterialIds, Long teacherId);
}

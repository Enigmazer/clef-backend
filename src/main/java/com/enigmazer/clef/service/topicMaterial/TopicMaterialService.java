package com.enigmazer.clef.service.topicMaterial;

import com.enigmazer.clef.dto.topicMaterial.TopicMaterialDeleteRequest;
import com.enigmazer.clef.dto.topicMaterial.TopicMaterialResponse;
import com.enigmazer.clef.dto.topicMaterial.TopicMaterialUrlResponse;
import org.springframework.web.multipart.MultipartFile;

public interface TopicMaterialService {
    TopicMaterialUrlResponse getTopicMaterialUrl(Long subjectId, Long unitId, Long topicId, Long topicMaterialId, Long teacherId);

    TopicMaterialResponse uploadTopicMaterial(Long subjectId, Long unitId, Long topicId, MultipartFile file, Long teacherId);

    void deleteTopicMaterials(Long subjectId, Long unitId, Long topicId, TopicMaterialDeleteRequest request, Long teacherId);
}

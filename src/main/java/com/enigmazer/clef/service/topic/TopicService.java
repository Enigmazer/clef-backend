package com.enigmazer.clef.service.topic;

import com.enigmazer.clef.dto.topic.TopicCompleteResponse;
import com.enigmazer.clef.dto.topic.TopicDeleteRequest;
import com.enigmazer.clef.dto.topic.TopicUpdateRequest;
import com.enigmazer.clef.dto.topic.TopicUpdateResponse;

import java.util.List;
import java.util.Set;

public interface TopicService {
    List<TopicUpdateResponse> updateTopics(
            Long subjectId, Long unitId,
            List<TopicUpdateRequest> request, Long teacherId
    );

    TopicCompleteResponse toggleTopicComplete(Long subjectId, Long unitId, Long topicId, Long teacherId);

    void deleteTopics(Long subjectId, Long unitId, TopicDeleteRequest request, Long teacherId);
}

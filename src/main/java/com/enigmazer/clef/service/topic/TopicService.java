package com.enigmazer.clef.service.topic;

import com.enigmazer.clef.dto.topic.TopicCompleteResponse;
import com.enigmazer.clef.dto.topic.TopicUpdateRequest;
import com.enigmazer.clef.dto.topic.TopicUpdateResponse;

import java.util.List;

public interface TopicService {
    List<TopicUpdateResponse> updateTopic(
            Long subjectId, Long unitId,
            List<TopicUpdateRequest> request, Long teacherId
    );

    void deleteTopic(Long subjectId, Long unitId, List<Long> topicIds, Long teacherId);
}

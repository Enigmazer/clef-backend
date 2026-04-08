package com.enigmazer.clef.service.topic;

import com.enigmazer.clef.dto.topic.TopicUpdateRequest;
import com.enigmazer.clef.dto.topic.TopicUpdateResponse;
import com.enigmazer.clef.entity.Subject;
import com.enigmazer.clef.entity.Topic;
import com.enigmazer.clef.exception.ResourceAlreadyExistsException;
import com.enigmazer.clef.exception.ResourceNotFoundException;
import com.enigmazer.clef.mapper.SimpleTopicMapper;
import com.enigmazer.clef.repository.TopicRepository;
import com.enigmazer.clef.service.common.SubjectHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService{

    private final TopicRepository topicRepository;

    private final SimpleTopicMapper simpleTopicMapper;

    private final SubjectHelper subjectHelper;

    @Override
    @Transactional
    public List<TopicUpdateResponse> updateTopic(
            Long subjectId, Long unitId,
            List<TopicUpdateRequest> request, Long teacherId
    ) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        List<TopicUpdateResponse> updatedTopics = new ArrayList<>();

        for(TopicUpdateRequest updatedTopic : request) {
            Topic topic = topicRepository
                    .findByIdAndUnitIdAndSubjectId(updatedTopic.topicId(), unitId, subjectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));

            try {
                if (updatedTopic.title() != null && !updatedTopic.title().isBlank()) {
                    topic.setTitle(updatedTopic.title());
                }
                updatedTopics.add(simpleTopicMapper.toUpdateResponse(topicRepository.save(topic)));
            } catch (DataIntegrityViolationException ex) {
                throw new ResourceAlreadyExistsException(
                        "Topic " + updatedTopic.title() + " already exists in this unit"
                );
            }
        }

        log.info("Successfully updated topics [subjectId={}, unitId={}, " +
                "topicsCount={}, teacherId={}]", subjectId, unitId, request.size(), teacherId);
        return updatedTopics;
    }

    @Override
    @Transactional
    public void deleteTopic(Long subjectId, Long unitId, List<Long> topicIds, Long teacherId) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        for (Long topicId : topicIds) {
            Topic topic = topicRepository.findByIdAndUnitIdAndSubjectId(topicId, unitId, subjectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));

            if ((subject.getCurrentTopic() != null && subject.getCurrentTopic().getId().equals(topicId)) ||
                    (subject.getNextTopic() != null && subject.getNextTopic().getId().equals(topicId))) {
                log.warn("Deleted topic was set as current or next topic " +
                        "[subjectId={}, topicId={}]", subjectId, topicId);
            }

            topicRepository.delete(topic);
        }
        log.info("Successfully deleted topics [subjectId={}, unitId={}, " +
                "topicCount={}, teacherId={}]", subjectId, unitId, topicIds.size(), teacherId);
    }


}

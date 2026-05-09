package com.enigmazer.clef.service.topic;

import com.enigmazer.clef.dto.topic.TopicCompleteResponse;
import com.enigmazer.clef.dto.topic.TopicDeleteRequest;
import com.enigmazer.clef.dto.topic.TopicUpdateRequest;
import com.enigmazer.clef.dto.topic.TopicUpdateResponse;
import com.enigmazer.clef.entity.Subject;
import com.enigmazer.clef.entity.Topic;
import com.enigmazer.clef.entity.TopicMaterial;
import com.enigmazer.clef.exception.InvalidRequestException;
import com.enigmazer.clef.exception.ResourceAlreadyExistsException;
import com.enigmazer.clef.exception.ResourceNotFoundException;
import com.enigmazer.clef.mapper.SimpleTopicMapper;
import com.enigmazer.clef.mapper.TopicMapper;
import com.enigmazer.clef.repository.SubjectRepository;
import com.enigmazer.clef.repository.TopicRepository;
import com.enigmazer.clef.service.common.SubjectHelper;
import com.enigmazer.clef.service.common.UrlCacheService;
import com.enigmazer.clef.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopicServiceImpl implements TopicService{

    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;

    private final UrlCacheService urlCacheService;
    private final StorageService storageService;

    private final TopicMapper topicMapper;
    private final SimpleTopicMapper simpleTopicMapper;

    private final SubjectHelper subjectHelper;

    @Override
    @Transactional
    @CacheEvict(value = "subjects", key = "#subjectId")
    public List<TopicUpdateResponse> updateTopics(
            Long subjectId, Long unitId,
            List<TopicUpdateRequest> request, Long teacherId
    ) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        List<TopicUpdateResponse> updatedTopics = new ArrayList<>();

        for(TopicUpdateRequest topicUpdateRequest : request) {
            Topic topic = topicRepository
                    .findByIdAndParentValidation(topicUpdateRequest.topicId(), unitId, subjectId)
                    .orElseThrow(() -> new InvalidRequestException("Topic not found: " + topicUpdateRequest.topicId()));

            if (topicUpdateRequest.title() != null && !topicUpdateRequest.title().isBlank()) {
                String trimmed = topicUpdateRequest.title().trim();
                if (topicRepository.existsByTitleAndUnitId(trimmed, unitId)) {
                    throw new ResourceAlreadyExistsException(
                            "Topic " + trimmed + " already exists in this unit"
                    );
                }
                topic.setTitle(trimmed);
            }
            updatedTopics.add(simpleTopicMapper.toUpdateResponse(topic));
        }

        subject.touch();
        log.info("Successfully updated topics [subjectId={}, unitId={}, " +
                "topicsCount={}, teacherId={}]", subjectId, unitId, request.size(), teacherId);
        return updatedTopics;
    }

    @Override
    @Transactional
    @CacheEvict(value = "subjects", key = "#subjectId")
    public TopicCompleteResponse toggleTopicComplete(
            Long subjectId, Long unitId,
            Long topicId, Long teacherId
    ) {
        Subject subject = subjectRepository.findWithCurrentNextTopicsByIdAndTeacherId(subjectId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        subjectHelper.checkArchived(subject);

        Topic topic = topicRepository.findByIdAndParentValidation(topicId, unitId, subjectId)
                .orElseThrow(() -> new InvalidRequestException("Topic not found"));

        if(topic.getCompletedAt() == null) {
            topic.setCompletedAt(Instant.now());

            if (subject.getCurrentTopic() != null && subject.getCurrentTopic().equals(topic)) {
                setNewCurrentAndNextTopics(subject);
            }else if (subject.getNextTopic() != null && subject.getNextTopic().equals(topic)) {
                setNewNextTopic(subject);
            }
        }else {
            topic.setCompletedAt(null);
        }

        subject.touch();
        log.info("Toggled topic complete state [subjectId={}, unitId={}, " +
                "id={}, teacherId={}]", subjectId, unitId, topicId, teacherId);
        return topicMapper.toTopicCompleteResponse(topic);
    }

    @Override
    @Transactional
    @CacheEvict(value = "subjects", key = "#subjectId")
    public void deleteTopics(Long subjectId, Long unitId, TopicDeleteRequest request, Long teacherId) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        List<Topic> topics = topicRepository
                .findWithTopicMaterialsByIdsAndParentValidation(request.topicIds(), unitId, subjectId);

        if(topics.isEmpty() || topics.size() != request.topicIds().size()){
            throw new InvalidRequestException("Topic(s) not found");
        }

        boolean currentAffected = false;
        boolean nextAffected = false;
        Long currentTopicId = null;
        Long nextTopicId = null;

        for (Topic topic : topics){
            if ((subject.getCurrentTopic() != null && subject.getCurrentTopic().equals(topic))){
                currentAffected = true;
                currentTopicId = topic.getId();
            }
            if ((subject.getNextTopic() != null && subject.getNextTopic().equals(topic))) {
                nextAffected = true;
                nextTopicId = topic.getId();
            }
        }
        List<TopicMaterial> topicMaterials = topics.stream()
                .flatMap(topic -> topic.getTopicMaterials().stream())
                .toList();

        List<String> topicMaterialKeys = topicMaterials.stream()
                .map(TopicMaterial::getTopicMaterialKey)
                .toList();

        if (currentAffected || nextAffected) {
            if (currentAffected) subject.setCurrentTopic(null);
            if (nextAffected) subject.setNextTopic(null);
            log.warn("Deleted topic was set as current or next topic, pointers cleared [subjectId={}, " +
                    "currentTopicId={}, nextTopicId={}]", subjectId, currentTopicId, nextTopicId);
        }

        if(!topicMaterialKeys.isEmpty()){
            topicMaterials.forEach(
                    tm -> urlCacheService.evictTopicMaterialUrl(subjectId, tm.getId()));
            storageService.deleteTopicMaterials(topicMaterialKeys);
        }

        topicRepository.deleteAll(topics);

        subject.touch();
        log.info("Successfully deleted topics [subjectId={}, unitId={}, " +
                "topicCount={}, teacherId={}]", subjectId, unitId, request.topicIds().size(), teacherId);
    }

    // --- Helper Methods ---
    private void setNewCurrentAndNextTopics(Subject subject) {
        Long oldCurrentId = subject.getCurrentTopic().getId();

        Topic newCurrentTopic = subject.getNextTopic();

        if(newCurrentTopic == null){
            newCurrentTopic = getNewNextTopicAfter(subject.getCurrentTopic());
        }
        Long newCurrentId = newCurrentTopic != null ? newCurrentTopic.getId() : null;
        Topic newNextTopic = newCurrentTopic != null ?
                getNewNextTopicAfter(newCurrentTopic) : null;

        subject.setCurrentTopic(newCurrentTopic);
        subject.setNextTopic(newNextTopic);
    }

    private void setNewNextTopic(Subject subject) {
        Long currentId = subject.getCurrentTopic() != null ?
                subject.getCurrentTopic().getId() : null;
        Topic newNextTopic = getNewNextTopicAfter(subject.getNextTopic());

        subject.setNextTopic(newNextTopic);
    }

    private Topic getNewNextTopicAfter(Topic topic){
        Topic newNextTopic = topicRepository
                .findFirstTopicInSequence(
                        topic.getId(),
                        topic.getOrderIndex(),
                        topic.getUnit().getOrderIndex(),
                        topic.getUnit().getSubject().getId()
                ).orElse(null);

        if (newNextTopic == null) {
            newNextTopic = topicRepository
                    .findRemainingFirstTopicBySubjectId(
                            topic.getId(),
                            topic.getUnit().getSubject().getId()
                    ).orElse(null);
        }
        return newNextTopic;
    }
}

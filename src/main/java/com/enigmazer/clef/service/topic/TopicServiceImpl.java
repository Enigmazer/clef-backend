package com.enigmazer.clef.service.topic;

import com.enigmazer.clef.dto.topic.TopicCompleteResponse;
import com.enigmazer.clef.dto.topic.TopicUpdateRequest;
import com.enigmazer.clef.dto.topic.TopicUpdateResponse;
import com.enigmazer.clef.entity.Subject;
import com.enigmazer.clef.entity.Topic;
import com.enigmazer.clef.entity.TopicMaterial;
import com.enigmazer.clef.exception.ResourceAlreadyExistsException;
import com.enigmazer.clef.exception.ResourceNotFoundException;
import com.enigmazer.clef.mapper.SimpleTopicMapper;
import com.enigmazer.clef.mapper.TopicMapper;
import com.enigmazer.clef.repository.SubjectRepository;
import com.enigmazer.clef.repository.TopicRepository;
import com.enigmazer.clef.service.common.SubjectHelper;
import com.enigmazer.clef.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService{

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;

    private final StorageService storageService;

    private final TopicMapper topicMapper;
    private final SimpleTopicMapper simpleTopicMapper;

    private final SubjectHelper subjectHelper;

    @Override
    @Transactional
    public List<TopicUpdateResponse> updateTopics(
            Long subjectId, Long unitId,
            List<TopicUpdateRequest> request, Long teacherId
    ) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        List<TopicUpdateResponse> updatedTopics = new ArrayList<>();

        for(TopicUpdateRequest updatedTopic : request) {
            Topic topic = topicRepository
                    .findByIdAndParentValidation(updatedTopic.topicId(), unitId, subjectId)
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
    public TopicCompleteResponse toggleTopicComplete(
            Long subjectId, Long unitId,
            Long topicId, Long teacherId
    ) {
        Subject subject = subjectRepository.findWithCurrentAndNextTopicByIdAndTeacherId(subjectId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        subjectHelper.checkArchived(subject);

        Topic topic = topicRepository.findByIdAndParentValidation(topicId, unitId, subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));

        if(topic.getCompletedAt() == null) {
            topic.setCompletedAt(Instant.now());

            if (subject.getCurrentTopic() != null && subject.getCurrentTopic().equals(topic)) {
                setNewCurrentAndNextTopics(subject);
                subjectRepository.save(subject);
            }else if (subject.getNextTopic() != null && subject.getNextTopic().equals(topic)) {
                setNewNextTopic(subject);
                subjectRepository.save(subject);
            }
        }else {
            topic.setCompletedAt(null);
        }
        Topic saved = topicRepository.save(topic);

        log.info("Toggled topic complete state [subjectId={}, unitId={}, " +
                "topicId={}, teacherId={}]", subjectId, unitId, topicId, teacherId);
        return topicMapper.toTopicCompleteResponse(saved);
    }

    @Override
    @Transactional
    public void deleteTopics(Long subjectId, Long unitId, List<Long> topicIds, Long teacherId) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        List<String> topicMaterialKeys = new ArrayList<>();

        List<Topic> topics = topicRepository
                .findWithTopicMaterialsByIdsAndParentValidation(topicIds, unitId, subjectId);

        if(topics.isEmpty() || topics.size() != topicIds.size()){
            throw new ResourceNotFoundException("Topic(s) not found");
        }

        boolean currentAffected = false;
        boolean nextAffected = false;
        Long topicId = null;

        for (Topic topic : topics){

            if ((subject.getCurrentTopic() != null && subject.getCurrentTopic().equals(topic))){
                currentAffected = true;
                topicId = topic.getId();
            }
            if ((subject.getNextTopic() != null && subject.getNextTopic().equals(topic))) {
                nextAffected = true;
                topicId = topic.getId();
            }

            Set<TopicMaterial> topicMaterials = topic.getTopicMaterials();
            topicMaterialKeys.addAll(
                    topicMaterials.stream()
                            .map(TopicMaterial::getTopicMaterialKey)
                            .filter(Objects::nonNull).toList()
            );
        }

        if (currentAffected || nextAffected) {
            if (currentAffected) subject.setCurrentTopic(null);
            if (nextAffected) subject.setNextTopic(null);
            subjectRepository.save(subject);
            log.warn("Deleted topic was set as current or next topic, pointers cleared " +
                    "[subjectId={}, topicId={}]", subjectId, topicId);
        }

        if(!topicMaterialKeys.isEmpty()){
            storageService.deleteTopicMaterials(topicMaterialKeys);
        }

        topicRepository.deleteAll(topics);

        log.info("Successfully deleted topics [subjectId={}, unitId={}, " +
                "topicCount={}, teacherId={}]", subjectId, unitId, topicIds.size(), teacherId);
    }

    // --- Helper Methods ---
    private void setNewCurrentAndNextTopics(Subject subject) {
        Long oldCurrentId = subject.getCurrentTopic().getId();

        Topic newCurrentTopic = subject.getNextTopic();

        if(newCurrentTopic == null){
            newCurrentTopic = getNewNextTopicAfter(subject.getCurrentTopic(), oldCurrentId);
        }
        Long newCurrentId = newCurrentTopic != null ? newCurrentTopic.getId() : null;
        Topic newNextTopic = newCurrentTopic != null ?
                getNewNextTopicAfter(newCurrentTopic, newCurrentId) : null;

        subject.setCurrentTopic(newCurrentTopic);
        subject.setNextTopic(newNextTopic);
    }

    private void setNewNextTopic(Subject subject) {
        Long currentId = subject.getCurrentTopic() != null ?
                subject.getCurrentTopic().getId() : null;
        Topic newNextTopic = getNewNextTopicAfter(subject.getNextTopic(), currentId);

        subject.setNextTopic(newNextTopic);
    }

    private Topic getNewNextTopicAfter(Topic topic, Long currentId){
        Topic newNextTopic = topicRepository
                .findByOrderIndexAndParentValidation(
                        topic.getOrderIndex() + 1,
                        topic.getUnit().getId(),
                        topic.getUnit().getSubject().getId(),
                        currentId
                ).orElse(null);

        if (newNextTopic == null) {
            newNextTopic = topicRepository
                    .findFirstTopicByUnitOrderIndexAndSubjectId(
                            topic.getUnit().getOrderIndex() + 1,
                            topic.getUnit().getSubject().getId(),
                            currentId
                    ).orElse(null);
        }

        if (newNextTopic == null) {
            newNextTopic = topicRepository
                    .findRemainingFirstTopicBySubjectId(
                            topic.getUnit().getSubject().getId(),
                            currentId
                    ).orElse(null);
        }
        return newNextTopic;
    }
}

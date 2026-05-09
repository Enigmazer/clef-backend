package com.enigmazer.clef.service.unit;

import com.enigmazer.clef.dto.topic.TopicCreationUpdateRequest;
import com.enigmazer.clef.dto.topic.TopicReorderRequest;
import com.enigmazer.clef.dto.unit.UnitCreationRequest;
import com.enigmazer.clef.dto.unit.UnitReorderRequest;
import com.enigmazer.clef.dto.unit.UnitUpdateRequest;
import com.enigmazer.clef.dto.unit.UnitUpdateResponse;
import com.enigmazer.clef.entity.Subject;
import com.enigmazer.clef.entity.Topic;
import com.enigmazer.clef.entity.TopicMaterial;
import com.enigmazer.clef.entity.Unit;
import com.enigmazer.clef.exception.InvalidRequestException;
import com.enigmazer.clef.exception.ResourceAlreadyExistsException;
import com.enigmazer.clef.exception.ResourceNotFoundException;
import com.enigmazer.clef.exception.SystemResourceNotFoundException;
import com.enigmazer.clef.mapper.UnitMapper;
import com.enigmazer.clef.repository.SubjectRepository;
import com.enigmazer.clef.repository.TopicRepository;
import com.enigmazer.clef.repository.UnitRepository;
import com.enigmazer.clef.service.common.SubjectHelper;
import com.enigmazer.clef.service.common.UrlCacheService;
import com.enigmazer.clef.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnitServiceImpl implements UnitService{

    private final UnitRepository unitRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;

    private final UrlCacheService urlCacheService;
    private final StorageService storageService;

    private final UnitMapper unitMapper;

    private final SubjectHelper subjectHelper;

    @Override
    @Transactional
    @CacheEvict(value = "subjects", key = "#subjectId")
    public void bulkAddUnits(Long subjectId, List<UnitCreationRequest> request, Long teacherId) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        int unitOrder = unitRepository.findMaxOrderIndex(subjectId)+1;

        for (UnitCreationRequest unitCreationRequest: request){
            Unit unit;
            String trimmed = unitCreationRequest.title().trim();
            if (unitRepository.existsByTitleAndSubjectId(trimmed, subjectId)) {
                throw new ResourceAlreadyExistsException("Unit " + trimmed + " already exists in this subject");
            }

            unit = unitRepository.save(
                    Unit.builder()
                            .title(trimmed)
                            .subject(subject)
                            .orderIndex(unitOrder++)
                            .build()
            );

            int topicOrder = 1;
            for (TopicCreationUpdateRequest topicCreationUpdateRequest : unitCreationRequest.topics()){
                String topicTrimmed = topicCreationUpdateRequest.title().trim();
                if (topicRepository.existsByTitleAndUnitId(trimmed, unit.getId())) {
                    throw new ResourceAlreadyExistsException(
                            "Topic " + trimmed + " already exists in this unit"
                    );
                }

                topicRepository.save(
                        Topic.builder()
                                .title(topicTrimmed)
                                .unit(unit)
                                .orderIndex(topicOrder++)
                                .build()
                );
            }
        }

        subject.touch();
        log.info("Bulk units created [subjectId={}, teacherId={}]", subjectId, teacherId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "subjects", key = "#subjectId")
    public UnitUpdateResponse updateUnit(
            Long subjectId, Long unitId,
            UnitUpdateRequest request, Long teacherId
    ) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        Unit unit = unitRepository.findByIdAndSubjectId(unitId, subjectId).orElseThrow(
                () -> new InvalidRequestException("Unit not found")
        );

        if (request.title() != null && !request.title().isBlank()) {
            String trimmed = request.title().trim();
            if (unitRepository.existsByTitleAndSubjectId(trimmed, subjectId)) {
                throw new ResourceAlreadyExistsException("Unit " + trimmed + " already exists in this subject");
            }
            unit.setTitle(trimmed);
        }

        int topicOrder = topicRepository.findMaxOrderIndex(unitId) + 1;
        for (TopicCreationUpdateRequest topicCreationUpdateRequest : request.topics()){
            try {
                topicRepository.save(
                        Topic.builder()
                                .title(topicCreationUpdateRequest.title())
                                .unit(unit)
                                .orderIndex(topicOrder++)
                                .build()
                );
            }catch (DataIntegrityViolationException ex){
                throw new ResourceAlreadyExistsException(
                        "Topic " + topicCreationUpdateRequest.title() + " already exists in this unit"
                );
            }
        }
        Unit savedUnit = unitRepository.findWithTopicsByIdAndSubjectId(unitId, subjectId).orElseThrow(
                () -> new SystemResourceNotFoundException("Unit not found", unitId)
        );

        subject.touch();
        log.info("Successfully updated unit [subjectId={}, " +
                "unitId={}, teacherId={}]", subjectId, unitId, teacherId);
        return unitMapper.toUpdateResponse(savedUnit);
    }

    @Override
    @Transactional
    @CacheEvict(value = "subjects", key = "#subjectId")
    public void reorder(Long subjectId, List<UnitReorderRequest> request, Long teacherId) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        List<Long> unitIds = request.stream().map(UnitReorderRequest::id).toList();
        List<Unit> units = unitRepository.findWithTopicsByIdsAndSubjectId(unitIds, subjectId);
        if (units.size() != request.size()) throw new InvalidRequestException("Unit(s) not found");
        Map<Long, Unit> unitMap = units.stream().collect(Collectors.toMap(Unit::getId, unit -> unit));

        Unit unit;
        for (UnitReorderRequest unitReorderRequest: request){
            unit = unitMap.get(unitReorderRequest.id());
            if (unit == null)
                throw new InvalidRequestException("Unit not found: " + unitReorderRequest.id());
            unit.setOrderIndex(unitReorderRequest.orderIndex());

            Map<Long, Topic> topicMap = unit.getTopics().stream()
                    .collect(Collectors.toMap(Topic::getId, topic -> topic));
            Topic topic;
            for (TopicReorderRequest topicReorderRequest: unitReorderRequest.topics()){
                topic = topicMap.get(topicReorderRequest.id());
                if (topic == null)
                    throw new InvalidRequestException("Topic not found: " + topicReorderRequest.id());
                topic.setOrderIndex(topicReorderRequest.orderIndex());
            }
        }
        subject.touch();
    }

    @Override
    @Transactional
    @CacheEvict(value = "subjects", key = "#subjectId")
    public void deleteUnit(Long subjectId, Long unitId,Long teacherId) {
        Subject subject = subjectRepository.findWithCurrentNextTopicsByIdAndTeacherId(subjectId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        subjectHelper.checkArchived(subject);

        Unit unit = unitRepository.findWithTopicsByIdAndSubjectId(unitId, subjectId)
                .orElseThrow(() -> new InvalidRequestException("Unit not found"));

        boolean currentAffected = subject.getCurrentTopic() != null &&
                subject.getCurrentTopic().getUnit().getId().equals(unitId);
        boolean nextAffected = subject.getNextTopic() != null &&
                subject.getNextTopic().getUnit().getId().equals(unitId);

        if (currentAffected || nextAffected) {
            if (currentAffected) subject.setCurrentTopic(null);
            if (nextAffected) subject.setNextTopic(null);
            log.warn("Deleted unit contained current or next topic, pointers cleared " +
                    "[subjectId={}, unitId={}]", subjectId, unitId);
        }

        List<TopicMaterial> topicMaterials = unit.getTopics().stream()
                .flatMap(topic -> topic.getTopicMaterials().stream())
                .toList();
        List<String> topicMaterialKeys = topicMaterials.stream()
                .map(TopicMaterial::getTopicMaterialKey)
                .toList();

        if(!topicMaterials.isEmpty()){
            topicMaterials.forEach(
                    tm -> urlCacheService.evictTopicMaterialUrl(subjectId, tm.getId()));
            storageService.deleteTopicMaterials(topicMaterialKeys);
        }

        unitRepository.delete(unit);

        subject.touch();
        log.info("Successfully deleted the unit [subjectId={}, " +
                "unitId={}, teacherId={}]", subjectId, unitId, teacherId);
    }
}

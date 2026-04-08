package com.enigmazer.clef.service.unit;

import com.enigmazer.clef.dto.topic.TopicCreationUpdateRequest;
import com.enigmazer.clef.dto.unit.UnitUpdateRequest;
import com.enigmazer.clef.dto.unit.UnitUpdateResponse;
import com.enigmazer.clef.entity.Subject;
import com.enigmazer.clef.entity.Topic;
import com.enigmazer.clef.entity.Unit;
import com.enigmazer.clef.exception.ResourceAlreadyExistsException;
import com.enigmazer.clef.exception.ResourceNotFoundException;
import com.enigmazer.clef.exception.SystemResourceNotFoundException;
import com.enigmazer.clef.mapper.UnitMapper;
import com.enigmazer.clef.repository.SubjectRepository;
import com.enigmazer.clef.repository.TopicRepository;
import com.enigmazer.clef.repository.UnitRepository;
import com.enigmazer.clef.service.common.SubjectHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnitServiceImpl implements UnitService{

    private final UnitRepository unitRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;

    private final UnitMapper unitMapper;

    private final SubjectHelper subjectHelper;

    @Override
    @Transactional
    public UnitUpdateResponse updateUnit(
            Long subjectId, Long unitId,
            UnitUpdateRequest request, Long teacherId
    ) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        Unit unit = unitRepository.findByIdAndSubjectId(unitId, subjectId).orElseThrow(
                () -> new ResourceNotFoundException("Unit not found")
        );

        try {
            if (request.title() != null && !request.title().isBlank()) {
                unit.setTitle(request.title().trim());
            }
            unitRepository.save(unit);
        }catch (DataIntegrityViolationException ex){
            throw new ResourceAlreadyExistsException(
                    "Unit " + request.title() + " already exists in this subject");
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
        Unit savedUnit = unitRepository.findByIdAndSubjectId(unitId, subjectId).orElseThrow(
                () -> new SystemResourceNotFoundException("Unit not found", unitId)
        );

        log.info("Successfully updated unit [subjectId={}, " +
                "unitId={}, teacherId={}]", subjectId, unitId, teacherId);
        return unitMapper.toUpdateResponse(savedUnit);
    }

    @Override
    @Transactional
    public void deleteUnit(Long subjectId, Long unitId,Long teacherId) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        Unit unit = unitRepository.findByIdAndSubjectId(unitId, subjectId).orElseThrow(
                () -> new ResourceNotFoundException("Unit not found")
        );

        boolean currentAffected = subject.getCurrentTopic() != null &&
                subject.getCurrentTopic().getUnit().getId().equals(unitId);
        boolean nextAffected = subject.getNextTopic() != null &&
                subject.getNextTopic().getUnit().getId().equals(unitId);

        if (currentAffected || nextAffected) {
            if (currentAffected) subject.setCurrentTopic(null);
            if (nextAffected) subject.setNextTopic(null);
            subjectRepository.save(subject);
            log.warn("Deleted unit contained current or next topic, pointers cleared " +
                    "[subjectId={}, unitId={}]", subjectId, unitId);
        }

        unitRepository.delete(unit);
        log.info("Successfully deleted the unit [subjectId={}, " +
                "unitId={}, teacherId={}]", subjectId, unitId, teacherId);
    }
}

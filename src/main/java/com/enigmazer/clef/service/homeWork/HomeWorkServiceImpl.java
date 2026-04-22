package com.enigmazer.clef.service.homeWork;

import com.enigmazer.clef.dto.homework.HomeWorkUpdateRequest;
import com.enigmazer.clef.dto.homework.HomeworkResponse;
import com.enigmazer.clef.entity.Homework;
import com.enigmazer.clef.entity.Subject;
import com.enigmazer.clef.entity.Topic;
import com.enigmazer.clef.exception.BusinessException;
import com.enigmazer.clef.exception.ResourceNotFoundException;
import com.enigmazer.clef.mapper.HomeworkMapper;
import com.enigmazer.clef.repository.HomeWorkRepository;
import com.enigmazer.clef.repository.TopicRepository;
import com.enigmazer.clef.service.common.SubjectHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeWorkServiceImpl implements HomeWorkService{

    private final HomeWorkRepository homeWorkRepository;
    private final TopicRepository topicRepository;

    private final HomeworkMapper homeworkMapper;

    private final SubjectHelper subjectHelper;

    @Override
    @Transactional
    public HomeworkResponse updateHomeWork(
            Long subjectId, Long homeWorkId,
            HomeWorkUpdateRequest request, Long teacherId
    ) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);
        subjectHelper.checkArchived(subject);

        Homework homework = homeWorkRepository.findByIdAndSubjectId(homeWorkId, subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Homework not found"));

        if (homework.getDueDate().isBefore(Instant.now())) {
            throw new BusinessException("Cannot update homework that is already due");
        }

        if (request.title() != null) homework.setTitle(request.title());
        if (request.description() != null) homework.setDescription(request.description());
        if (request.dueDate() != null) homework.setDueDate(request.dueDate());

        if (request.topicIds() != null) {
            Set<Topic> topics = topicRepository.findByIdsAndSubjectId(request.topicIds(), subjectId);
            if (topics.size() != request.topicIds().size()) {
                throw new ResourceNotFoundException("Topic(s) not found");
            }
            homework.setTopics(topics);
        }

        homeWorkRepository.save(homework);
        log.info("Homework updated [homeworkId={}, subjectId={}, teacherId={}]",
                homeWorkId, subjectId, teacherId);
        return homeworkMapper.toResponse(homework);
    }

    @Override
    @Transactional
    public void deleteHomeWork(Long subjectId, Long homeWorkId, Long teacherId) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        Homework homework = homeWorkRepository.findByIdAndSubjectId(homeWorkId, subjectId).orElseThrow(
                () -> new ResourceNotFoundException("Homework not found")
        );

        homeWorkRepository.delete(homework);
        log.info("Homework successfully deleted [homeWorkId={}, " +
                "subjectId={}, teacherId={}]", homeWorkId, subjectId, teacherId);
    }
}

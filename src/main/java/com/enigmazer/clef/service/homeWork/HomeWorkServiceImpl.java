package com.enigmazer.clef.service.homeWork;

import com.enigmazer.clef.dto.homework.HomeWorkUpdateRequest;
import com.enigmazer.clef.dto.homework.HomeworkCreationRequest;
import com.enigmazer.clef.dto.homework.HomeworkResponse;
import com.enigmazer.clef.dto.page.PageResponse;
import com.enigmazer.clef.entity.Homework;
import com.enigmazer.clef.entity.Subject;
import com.enigmazer.clef.entity.Topic;
import com.enigmazer.clef.exception.BusinessException;
import com.enigmazer.clef.exception.ResourceNotFoundException;
import com.enigmazer.clef.mapper.HomeworkMapper;
import com.enigmazer.clef.repository.HomeWorkRepository;
import com.enigmazer.clef.repository.SubjectRepository;
import com.enigmazer.clef.repository.TopicRepository;
import com.enigmazer.clef.service.common.SubjectHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeWorkServiceImpl implements HomeWorkService{

    private final HomeWorkRepository homeWorkRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;

    private final HomeworkMapper homeworkMapper;

    private final SubjectHelper subjectHelper;

    @Override
    @Transactional
    public HomeworkResponse createHomework(Long subjectId, HomeworkCreationRequest request, Long teacherId) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        Set<Topic> topics = new LinkedHashSet<>();
        if(request.topicIds() != null){
            topics = topicRepository.findByIdsAndSubjectId(request.topicIds(), subjectId);
            if(topics.size() != request.topicIds().size()){
                throw new ResourceNotFoundException("Topic(s) not found");
            }
        }

        Homework homework = homeWorkRepository.save(
                Homework.builder()
                        .title(request.title())
                        .description(request.description())
                        .subject(subject)
                        .topics(topics)
                        .dueDate(request.dueDate())
                        .build()
        );

        subject.touch();
        log.info("Homework created [homeworkId={}, teacherId={}]",
                homework.getId(), teacherId);
        return homeworkMapper.toResponse(homework);
    }

    @Override
    public PageResponse<HomeworkResponse> getHomeWorkPage(
            Long subjectId, String filter, int page, Long userId
    ) {
        Subject subject = subjectRepository.findSubjectByIdAndUserId(subjectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        boolean isUpcoming = filter.equals("upcoming");

        Sort sort = isUpcoming ?
                Sort.by(Sort.Direction.ASC, "dueDate") :
                Sort.by(Sort.Direction.DESC, "dueDate");
        Pageable pageable = PageRequest.of(page, 10, sort);

        Instant now = Instant.now();

        Page<Long> idsPage = isUpcoming ?
                homeWorkRepository.findUpcomingHomeworkIdsBySubject(subject, now, pageable)
                : homeWorkRepository.findPastHomeworkIdsBySubject(subject, now, pageable);


        if (idsPage.isEmpty()) {
            return PageResponse.from(Page.empty(pageable));
        }

        List<Homework> homeworks = isUpcoming ?
                homeWorkRepository.findFutureHomeworksWithTopicsByIds(idsPage.getContent())
                : homeWorkRepository.findPastHomeworksWithTopicsByIds(idsPage.getContent());

        Page<Homework> homework = new PageImpl<>(homeworks, pageable, idsPage.getTotalElements());

        log.info("Returned homework page [subjectId={}, userId={}, " +
                "pageNo={}, filter={}]", subjectId, userId, page, filter);
        return PageResponse.from(homework.map(homeworkMapper::toResponse));
    }

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

        subject.touch();
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
        subject.touch();
        log.info("Homework successfully deleted [homeWorkId={}, " +
                "subjectId={}, teacherId={}]", homeWorkId, subjectId, teacherId);
    }
}

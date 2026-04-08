package com.enigmazer.clef.service.subject;

import com.enigmazer.clef.dto.enrollment.EnrolledStudentResponse;
import com.enigmazer.clef.dto.subject.*;
import com.enigmazer.clef.dto.subject.SubjectCurrentNextTopicRequest;
import com.enigmazer.clef.dto.topic.TopicCreationUpdateRequest;
import com.enigmazer.clef.dto.subject.SubjectCurrentNextTopicResponse;
import com.enigmazer.clef.dto.unit.UnitCreationRequest;
import com.enigmazer.clef.entity.*;
import com.enigmazer.clef.exception.*;
import com.enigmazer.clef.mapper.*;
import com.enigmazer.clef.repository.*;
import com.enigmazer.clef.service.common.SubjectHelper;
import com.enigmazer.clef.util.JoinCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final UnitRepository unitRepository;
    private final TopicRepository topicRepository;
    private final EnrollmentRepository enrollmentRepository;

    private final SubjectMapper subjectMapper;
    private final TopicMapper topicMapper;
    private final EnrollmentMapper enrollmentMapper;

    private final SubjectHelper subjectHelper;

    @Override
    @Transactional
    public SubjectDetailsTeacherResponse createSubject(SubjectCreationRequest request, Long teacherId) {
        User teacher = userRepository.getReferenceById(teacherId);

        Subject newSubject = subjectMapper.toEntity(request);
        newSubject.setTeacher(teacher);
        newSubject.setJoinCode(getJoinCode());

        try {
            Subject savedSubject = subjectRepository.save(newSubject);
            log.info("Subject created [name={}, id={}, teacherId={}]",
                    savedSubject.getName(), savedSubject.getId(), teacherId);
            return subjectMapper.toTeacherResponse(savedSubject);
        }catch (DataIntegrityViolationException ex) {
            throw new ResourceAlreadyExistsException("Subject " + request.name() + " already exists");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListTeacherSubjectsResponse> listTeacherSubjects(Long teacherId) {
        log.debug("Returned all subjects [teacherId={}]", teacherId);
        return subjectRepository.findAllByTeacherIdAndIsArchivedFalse(teacherId)
                .stream()
                .map(subjectMapper::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<ListTeacherSubjectsResponse> listArchivedTeacherSubjects(Long teacherId) {
        log.debug("Returned all archived subjects [teacherId={}]", teacherId);
        return subjectRepository.findAllByTeacherIdAndIsArchivedTrue(teacherId)
                .stream()
                .map(subjectMapper::toSummaryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListStudentSubjectsResponse> listStudentSubjects(Long studentId) {
        log.debug("Returned all subjects [studentId={}]", studentId);
        return enrollmentRepository.findAllByStudentIdAndIsArchivedFalse(studentId)
                .stream()
                .map(enrollmentMapper::toStudentSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrolledStudentResponse> listEnrolledStudents(Long subjectId, Long teacherId) {
        if (!subjectRepository.existsByIdAndTeacherId(subjectId, teacherId)) {
            throw new ResourceNotFoundException("Subject not found");
        }

        log.debug("Returned all students enrolled in the subject " +
                "[subjectId={}, teacherId={}]", subjectId, teacherId);
        return enrollmentRepository.findAllBySubjectIdAndTeacherId(subjectId, teacherId)
                .stream()
                .map(enrollmentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectDetailsTeacherResponse getTeacherSubjectDetails(Long subjectId, Long teacherId) {
        Subject subject = subjectRepository.findByIdForTeacher(subjectId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        log.debug("Returned subject details [subjectId={}, teacherId={}]", subjectId, teacherId);
        return subjectMapper.toTeacherResponse(subject);
    }

    @Override
    @Transactional(readOnly = true)
    public SubjectDetailsStudentResponse getStudentSubjectDetails(Long subjectId, Long studentId) {
        // check first because if enrollment doesn't exist, we save a heavy db call
        if (!enrollmentRepository.existsByStudentIdAndSubjectId(studentId, subjectId)) {
            throw new ResourceNotFoundException("Subject not found");
        }

        Subject subject = subjectRepository.findByIdForStudent(subjectId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        if(!subject.getTeacher().isShowPhoneToStudents()){
            subject.getTeacher().setPhoneNumbers(null);
        }

        log.debug("Returned subject details [subjectId={}, studentId={}]", subjectId, studentId);
        return subjectMapper.toStudentResponse(subject);
    }

    @Override
    @Transactional
    public SubjectUpdateReqRes updateSubject(Long subjectId, SubjectUpdateReqRes request, Long teacherId){
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        try {
            if (request.name() != null && !request.name().isBlank()) {
                subject.setName(request.name().trim());
            }
            if (request.description() != null && !request.description().isBlank()) {
                subject.setDescription(request.description().trim());
            }
            subject = subjectRepository.save(subject);
        } catch (DataIntegrityViolationException ex) {
            throw new ResourceAlreadyExistsException("Subject " + request.name() + " already exists.");
        }

        log.info("Successfully updated subject [subjectId={}, teacherId={}]", subjectId, teacherId);
        return subjectMapper.toUpdateResponse(subject);
    }

    @Override
    @Transactional
    public boolean toggleSubjectLock(Long subjectId, Long teacherId) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        boolean lock = !subject.isLocked();
        subject.setLocked(lock);

        subjectRepository.save(subject);
        log.info("Toggled lock state [oldLock={}, newLock={}, " +
                "subjectId={}, teacherId={}]", !lock, lock, subject.getId(), teacherId);
        return lock;
    }

    @Override
    @Transactional
    public boolean toggleSubjectArchive(Long subjectId, Long teacherId) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        boolean archive = !subject.isArchived();
        subject.setArchived(archive);

        subjectRepository.save(subject);
        log.info("Toggled archive state [oldArchive={}, newArchive={}, " +
                "subjectId={}, teacherId={}]", !archive, archive, subject.getId(), teacherId);
        return archive;
    }

    @Override
    @Transactional
    public String setSyllabusUrl(Long subjectId, String syllabusUrl, Long teacherId) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        subject.setSyllabusFileUrl(syllabusUrl);

        subjectRepository.save(subject);
        log.info("Successfully added syllabus url for subject " +
                "[subjectId={}, teacherId={}]", subjectId, teacherId);
        return syllabusUrl;
    }

    @Override
    @Transactional
    public SubjectCurrentNextTopicResponse setCurrentTopic(
            Long subjectId,
            SubjectCurrentNextTopicRequest request,
            Long teacherId
    ){
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        Topic topic = topicRepository.findByIdAndUnitIdAndSubjectId(
                        request.id(), request.unitId(), subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));

        if (topic.getCompletedAt() != null) {
            throw new InvalidRequestException("A completed topic cannot be set as current topic");
        }

        if (subject.getNextTopic() != null && subject.getNextTopic().getId().equals(request.id())) {
            throw new InvalidRequestException("Current and next topic cannot be the same");
        }

        subject.setCurrentTopic(topic);

        subjectRepository.save(subject);
        log.info("Changed current topic for subject [newTopicId={} " +
                "subjectId={}, teacherId={}]", topic.getId(), subjectId, teacherId);
        return topicMapper.toResponse(topic);
    }

    @Override
    @Transactional
    public SubjectCurrentNextTopicResponse setNextTopic(
            Long subjectId,
            SubjectCurrentNextTopicRequest request,
            Long teacherId
    ){
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        Topic topic = topicRepository.findByIdAndUnitIdAndSubjectId(
                        request.id(), request.unitId(), subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));

        if (topic.getCompletedAt() != null) {
            throw new InvalidRequestException("A completed topic cannot be set as current topic");
        }

        if (subject.getCurrentTopic() != null && subject.getCurrentTopic().getId().equals(request.id())) {
            throw new InvalidRequestException("Next and current topic cannot be the same");
        }

        subject.setNextTopic(topic);

        subjectRepository.save(subject);
        log.info("Changed next topic for subject [newTopicId={} " +
                "subjectId={}, teacherId={}]", topic.getId(), subjectId, teacherId);
        return topicMapper.toResponse(topic);
    }

    @Override
    @Transactional
    public void deleteSubject(Long subjectId, Long teacherId) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        // Clearing Hibernate session references before delete
        subject.setCurrentTopic(null);
        subject.setNextTopic(null);
        subjectRepository.save(subject);

        subjectRepository.delete(subject);
        log.info("Successfully deleted the subject [subjectId={}, teacherId={}]", subjectId, teacherId);
    }

    @Override
    @Transactional
    public SubjectDetailsStudentResponse joinSubject(String joinCode, Long studentId) {
        Subject subject = subjectRepository.findByJoinCode(joinCode).orElseThrow(
                () -> new ResourceNotFoundException("Subject not found with Join Code: " + joinCode)
        );

        if (subject.isLocked() || subject.isArchived()) {
            throw new InvalidRequestException("This subject is not accepting new enrollments");
        }

        if (subject.getTeacher().getId().equals(studentId)){
            throw new InvalidRequestException("You cannot join a subject you own");
        }

        User student = userRepository.getReferenceById(studentId);

        try {
            enrollmentRepository.save(Enrollment.builder()
                    .student(student)
                    .subject(subject)
                    .build()
            );
        }catch (DataIntegrityViolationException ex) {
            throw new ResourceAlreadyExistsException("You are already enrolled in this subject");
        }

        Subject fullSubject = subjectRepository.findByIdForStudent(subject.getId(), studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        if(!subject.getTeacher().isShowPhoneToStudents()){
            subject.getTeacher().setPhoneNumbers(null);
        }

        log.info("Student successfully joined subject [studentId={}, " +
                "subjectId={}, teacherId={}]", studentId, subject.getId(), subject.getTeacher().getId());
        return subjectMapper.toStudentResponse(fullSubject);
    }


    @Override
    @Transactional
    public SubjectDetailsTeacherResponse bulkAddUnits(Long subjectId, List<UnitCreationRequest> request, Long teacherId) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        int unitOrder = unitRepository.findMaxOrderIndex(subjectId)+1;

        for (UnitCreationRequest unitCreationRequest: request){
            Unit unit;
            try {
                unit = unitRepository.save(
                        Unit.builder()
                                .title(unitCreationRequest.title())
                                .subject(subject)
                                .orderIndex(unitOrder++)
                                .build()
                );
            }catch (DataIntegrityViolationException ex){
                throw new ResourceAlreadyExistsException(
                        "Unit " + unitCreationRequest.title() + " already exists in this subject");
            }

            int topicOrder = 1;
            for (TopicCreationUpdateRequest topicCreationUpdateRequest : unitCreationRequest.topics()){
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
        }
        log.info("Bulk units created [subjectId={}, teacherId={}]", subjectId, teacherId);

        return getTeacherSubjectDetails(subjectId, teacherId);
    }

    // generates a unique 6-character join code — throws BusinessException after 10 failed attempts
    private String getJoinCode() {
        int attempts = 0;
        String code;
        do {
            if (attempts++ > 10) {
                throw new BusinessException("Failed to generate unique join code");
            }
            code = JoinCodeGenerator.generate();
        } while (subjectRepository.existsByJoinCode(code));
        return code;
    }
}